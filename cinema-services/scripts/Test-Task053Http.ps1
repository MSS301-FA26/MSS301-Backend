# Real Java-to-Java HTTP checks. Only run against Start-Task053Runtime.ps1 fixtures.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$state=Import-Clixml (Join-Path $root 'target/task053-state.clixml')
Add-Type -AssemblyName System.Net.Http
$client=New-Object System.Net.Http.HttpClient
$client.Timeout=[TimeSpan]::FromSeconds(20)
$results=[Collections.Generic.List[object]]::new()
function Request($name,$path,$expected=200,$token=$null,$method='GET',$body=$null,$port=15800,$headers=@{}) {
    $request=New-Object System.Net.Http.HttpRequestMessage ([System.Net.Http.HttpMethod]::new($method)),"http://127.0.0.1:$port$path"
    if ($token) { $request.Headers.TryAddWithoutValidation('Authorization',"Bearer $token") | Out-Null }
    foreach ($key in $headers.Keys) { $request.Headers.TryAddWithoutValidation($key,[string]$headers[$key]) | Out-Null }
    if ($null -ne $body) { $request.Content=[System.Net.Http.StringContent]::new(($body | ConvertTo-Json -Depth 10 -Compress),[Text.Encoding]::UTF8,'application/json') }
    $timer=[Diagnostics.Stopwatch]::StartNew()
    try {
        $response=$client.SendAsync($request).GetAwaiter().GetResult()
        $status=[int]$response.StatusCode
        $text=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $json=try { $text | ConvertFrom-Json } catch { $null }
        $passed=$status -in @($expected)
        $results.Add([pscustomobject]@{Name=$name;Status=$status;Expected=(@($expected)-join ',');Passed=$passed;Milliseconds=$timer.ElapsedMilliseconds})
        Write-Host "$name HTTP=$status PASS=$passed"
        if (!$passed) { Write-Host ($text.Substring(0,[Math]::Min(300,$text.Length))) }
        return $json
    } finally { $request.Dispose() }
}
function Sql($service,$sql) {
    $labels=docker inspect "task053-$service-db" --format '{{json .Config.Labels}}' | ConvertFrom-Json
    if ($labels.'task.owner' -ne 'task053') { throw 'Database ownership check failed' }
    $value="SET client_min_messages=warning; $sql" | docker exec -i "task053-$service-db" psql -q -v ON_ERROR_STOP=1 -U "${service}_verify" -d "${service}_db" -At
    if ($LASTEXITCODE) { throw "Fixture SQL failed: $service" }
    return $value
}
function Check($name,$passed) {
    $results.Add([pscustomobject]@{Name=$name;Status='ASSERT';Expected='true';Passed=[bool]$passed;Milliseconds=0})
    Write-Host "$name PASS=$passed"
}
function Stop-App($name) {
    $app=$state.Apps | Where-Object Name -eq $name
    $process=Get-CimInstance Win32_Process -Filter "ProcessId=$($app.Pid)"
    if (!$process -or !$process.CommandLine.Contains($app.Jar)) { throw 'PID ownership check failed' }
    Stop-Process -Id $app.Pid
    Start-Sleep -Seconds 1
}
function Restart-App($name) {
    $app=$state.Apps | Where-Object Name -eq $name
    $process=Start-Process java -WindowStyle Hidden -ArgumentList ($app.Args + '--logging.level.root=INFO') -WorkingDirectory $app.WorkingDirectory -RedirectStandardOutput (Join-Path $root "target/task053-$name-restart.log") -RedirectStandardError (Join-Path $root "target/task053-$name-restart.err.log") -PassThru
    $app.Pid=$process.Id
    $state | Export-Clixml (Join-Path $root 'target/task053-state.clixml')
    for ($attempt=0;$attempt -lt 90;$attempt++) {
        try { if ((Invoke-WebRequest "http://127.0.0.1:$($app.Port)/actuator/health" -UseBasicParsing -TimeoutSec 1).StatusCode -eq 200) {
            if ($name -eq 'catalog-service') {
                Request catalog-restart-warmup '/internal/v1/catalog/checkout-quote' 200 $null POST @{showtimeId=1;seatIds=@(2);tickets=@(@{seatId=2;ticketType='ADULT';viewerAge=20;quantity=1});foods=@()} 15802 @{'X-Internal-Service-Secret'=$state.Internal} | Out-Null
            }
            return
        } } catch { }
        Start-Sleep -Seconds 1
    }
    throw "Restart timeout: $name"
}
try {
    foreach ($port in 15800..15804) { Request "concurrent-health-$port" '/actuator/health' 200 $null GET $null $port | Out-Null }
    # Reset only disposable fixture bookings; no shared database is reachable here.
    Sql booking 'TRUNCATE bookings CASCADE' | Out-Null
    Sql identity @'
INSERT INTO users(id,email,password_hash,status,email_verified)
SELECT v.id,v.email,u.password_hash,'ACTIVE',true FROM users u CROSS JOIN
(VALUES(1001,'manager-a@task053.test'),(1002,'staff-b@task053.test'),(1003,'customer@task053.test'),(1004,'wallet-race@task053.test')) v(id,email)
WHERE u.email='admin@cinemaai.com' ON CONFLICT(id) DO NOTHING;
INSERT INTO user_profiles(user_id,full_name) VALUES(1001,'Manager A'),(1002,'Staff B'),(1003,'Customer'),(1004,'Concurrent Wallet') ON CONFLICT(user_id) DO NOTHING;
INSERT INTO user_roles(user_id,role_id) SELECT v.id,r.id FROM (VALUES(1001,'MANAGER'),(1002,'STAFF'),(1003,'CUSTOMER'),(1004,'CUSTOMER')) v(id,role) JOIN roles r ON r.name=v.role ON CONFLICT DO NOTHING;
INSERT INTO user_cinema_assignments(user_id,cinema_id) VALUES(1001,1),(1002,999) ON CONFLICT(user_id) DO NOTHING;
'@ | Out-Null
    $admin=(Request admin-login '/api/v1/auth/login' 200 $null POST @{email='admin@cinemaai.com';password=$state.Password}).data.accessToken
    $manager=(Request manager-login '/api/v1/auth/login' 200 $null POST @{email='manager-a@task053.test';password=$state.Password}).data.accessToken
    $customer=(Request customer-login '/api/v1/auth/login' 200 $null POST @{email='customer@task053.test';password=$state.Password}).data.accessToken
    if (!$admin -or !$manager -or !$customer) { throw 'Login token missing' }
    foreach ($path in @('/api/v1/movies','/api/v1/cinemas','/api/v1/showtimes','/api/v1/foods/items')) { Request "public-$path" $path | Out-Null }
    foreach ($path in @('/api/v1/users/me','/api/v1/bookings','/api/v1/wallet','/api/v1/admin/movies')) {
        Request "missing-jwt-$path" $path 401 | Out-Null
        Request "invalid-jwt-$path" $path 401 invalid | Out-Null
    }
    Request valid-user '/api/v1/users/me' 200 $customer | Out-Null
    Request valid-bookings '/api/v1/bookings' 200 $customer | Out-Null
    $wallet=Request valid-wallet '/api/v1/wallet' 200 $customer
    $repeat=Request wallet-repeated-get '/api/v1/wallet' 200 $customer
    Check wallet-repeat-preserves-id-and-zero ($wallet.data.id -eq $repeat.data.id -and $wallet.data.balance -eq 0 -and $repeat.data.balance -eq 0)
    Check wallet-single-row ((Sql payment 'select count(*) from cine_wallets where user_id=1003') -eq 1)
    Check wallet-no-transaction ((Sql payment 'select count(*) from wallet_transactions where user_id=1003') -eq 0)
    Check wallet-no-withdrawal ((Sql payment 'select count(*) from withdrawal_requests where user_id=1003') -eq 0)
    $raceToken=(Request wallet-race-login '/api/v1/auth/login' 200 $null POST @{email='wallet-race@task053.test';password=$state.Password}).data.accessToken
    if (!$raceToken) { throw 'Concurrent wallet fixture login failed' }
    $tasks=@();$requests=@()
    for($i=0;$i -lt 8;$i++) {
        $req=[System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get,'http://127.0.0.1:15800/api/v1/wallet')
        $req.Headers.TryAddWithoutValidation('Authorization',"Bearer $raceToken") | Out-Null
        $requests+=$req;$tasks+=$client.SendAsync($req)
    }
    $walletIds=@()
    try {
        for($i=0;$i -lt 8;$i++) {
            $response=$tasks[$i].GetAwaiter().GetResult()
            $result=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            Check "wallet-concurrent-$i-http-and-balance" ([int]$response.StatusCode -eq 200 -and $null -ne $result.data.id -and $result.data.balance -eq 0)
            $walletIds+=$result.data.id
            $response.Dispose()
        }
    } finally { foreach($req in $requests){$req.Dispose()} }
    Check wallet-concurrent-one-id (@($walletIds | Select-Object -Unique).Count -eq 1)
    Check wallet-concurrent-one-row ((Sql payment 'select count(*) from cine_wallets where user_id=1004') -eq 1)
    Check wallet-concurrent-no-financial-side-effects ((Sql payment 'select (select count(*) from wallet_transactions where user_id=1004)+(select count(*) from withdrawal_requests where user_id=1004)') -eq 0)
    $own=Request wallet-spoof-cannot-read-other '/api/v1/wallet?userId=1004' 200 $customer GET $null 15800 @{'X-User-Id'='1004';'X-User-Roles'='ADMIN'}
    Check wallet-owner-is-principal ($own.data.id -eq $wallet.data.id -and $own.data.id -ne $walletIds[0])
    Request valid-loyalty '/api/v1/loyalty/config' 200 $customer | Out-Null
    Request valid-admin-users '/api/v1/admin/users' 200 $admin | Out-Null
    Request valid-admin-bookings '/api/v1/admin/bookings' 200 $admin | Out-Null
    Request manager-staff-alias '/api/v1/staff/profiles/2' 200 $manager | Out-Null
    Request manager-staff-canonical '/api/v1/admin/staff-profiles/2' 200 $manager | Out-Null
    Request manager-cross-staff '/api/v1/staff/profiles/1002' 403 $manager | Out-Null
    $spoof=@{'X-User-Id'='1';'X-User-Roles'='ADMIN';'X-User-Email'='admin@cinemaai.com';'X-User-Cinema-Id'='999';'X-Internal-Service-Secret'=$state.Internal}
    Request spoof-without-jwt '/api/v1/admin/users' 401 $null GET $null 15800 $spoof | Out-Null
    Request spoof-with-customer '/api/v1/admin/users' 403 $customer GET $null 15800 $spoof | Out-Null
    Request spoof-manager-cross-staff '/api/v1/staff/profiles/1002' 403 $manager GET $null 15800 $spoof | Out-Null
    Request gateway-internal-block '/internal/v1/users/1/scope' 404 $admin GET $null 15800 $spoof | Out-Null
    $internal=@{'X-Internal-Service-Secret'=$state.Internal}
    foreach ($target in @(@(15801,'/internal/v1/users/1/scope'),@(15802,'/internal/v1/cinemas/1'),@(15803,'/internal/v1/bookings/showtimes/1/occupied-seats'),@(15804,'/internal/v1/payments/bookings/999999/status'))) {
        Request "internal-missing-$($target[0])" $target[1] 403 $null GET $null $target[0] | Out-Null
        Request "internal-invalid-$($target[0])" $target[1] 403 $null GET $null $target[0] @{'X-Internal-Service-Secret'='invalid'} | Out-Null
        Request "internal-valid-$($target[0])" $target[1] 200 $null GET $null $target[0] $internal | Out-Null
    }
    Request identity-catalog-valid '/api/v1/admin/users/2/cinema' 200 $admin PUT @{cinemaId=1} | Out-Null
    Request identity-catalog-missing '/api/v1/admin/users/2/cinema' 404 $admin PUT @{cinemaId=999999} | Out-Null
    $genre=Request catalog-audit-create '/api/v1/admin/genres' 201 $admin POST @{name="Task053-$([guid]::NewGuid().ToString('N'))";description='Disposable integration fixture verifying cross-service audit HTTP authentication.'}
    $audit=Request catalog-identity-audit '/api/v1/admin/audit-logs?targetType=GENRE' 200 $admin
    Sql catalog "INSERT INTO reviews(id,movie_id,user_id,rating,content,status) VALUES(2000,1,1003,4,'Task053 disposable review','PUBLISHED') ON CONFLICT(id) DO UPDATE SET status='PUBLISHED'" | Out-Null
    Request catalog-identity-review-audit '/api/v1/admin/reviews/2000/hide' 200 $admin POST @{reason='Task053 moderation fixture'} | Out-Null
    Check catalog-audit-persisted ((Sql identity "select count(*) from audit_logs where target_type='REVIEW' and target_id=2000") -gt 0)
    $emptyMap=Request catalog-booking-seat-map '/api/v1/showtimes/1/seat-map' 200
    Check catalog-valid-empty-occupancy-allows-free-seat (@($emptyMap.data.seats | Where-Object { $_.seatId -eq 1 -and $_.runtimeStatus -eq 'AVAILABLE' }).Count -eq 1)
    Request catalog-missing-showtime '/api/v1/showtimes/999999/seat-map' 404 | Out-Null
    Request booking-catalog-invalid '/api/v1/bookings/hold' 400 $customer POST @{showtimeId=999999;seatIds=@(1)} | Out-Null
    $hold=Request booking-catalog-quote '/api/v1/bookings/hold' 200 $customer POST @{showtimeId=1;seatIds=@(1)}
    $bookingId=$hold.data.id
    if (!$bookingId) { throw 'Real quote/hold failed' }
    Request booking-identity-scope '/api/v1/admin/bookings?cinemaId=1' 200 $manager | Out-Null
    $scoped=Request booking-identity-cross '/api/v1/admin/bookings?cinemaId=999' 200 $manager
    Check booking-list-enforced-cinema (@($scoped.data.content | Where-Object cinemaId -ne 1).Count -eq 0)
    Request booking-checkout "/api/v1/bookings/$bookingId/checkout" 200 $customer POST @{} | Out-Null
    Sql booking "update bookings set loyalty_points_redeemed=1 where id=$bookingId" | Out-Null
    Request booking-payment-status-guard "/api/v1/bookings/$bookingId/items" 200 $customer PUT @{loyaltyPointsToRedeem=0} | Out-Null
    Check booking-payment-guard-reset ((Sql booking "select loyalty_points_redeemed from bookings where id=$bookingId") -eq 0)
    Request payment-booking-missing '/api/v1/payments/mock?bookingId=999999' 404 $customer POST | Out-Null
    Request payment-booking-cross-owner "/api/v1/payments/mock?bookingId=$bookingId" 404 $manager POST | Out-Null
    # Existing MOCK endpoint, disposable fixtures only: never sends a VNPay request.
    Request payment-booking-fixture-mock "/api/v1/payments/mock?bookingId=$bookingId" 200 $customer POST | Out-Null
    Start-Sleep -Seconds 1
    Check payment-booking-mark-paid ((Sql booking "select status from bookings where id=$bookingId") -eq 'PAID')
    $paidMap=Request catalog-booked-seat-map '/api/v1/showtimes/1/seat-map' 200
    Check catalog-booked-seat-protected (@($paidMap.data.seats | Where-Object { $_.seatId -eq 1 -and $_.runtimeStatus -eq 'BOOKED' }).Count -eq 1)
    $paymentStatus=Request payment-status-mapping "/internal/v1/payments/bookings/$bookingId/status" 200 $null GET $null 15804 $internal
    Check payment-status-active ($paymentStatus.data.activePayment -and $paymentStatus.data.status -eq 'SUCCESS')
    $config=@{cinemaId=1;cinemaName='Task053 A';earningRatePercent=5;redemptionRatePercent=100;redemptionPoints=100;redemptionValueVnd=1000;maxRedemptionPercent=50;expiryMonth=12;expiryDay=31;expiryTime='23:59'}
    Request payment-identity-manager-read '/api/v1/admin/loyalty/config' 200 $manager | Out-Null
    Request payment-identity-manager-write '/api/v1/admin/loyalty/config' 200 $manager PUT $config | Out-Null
    $config.cinemaId=999
    Request payment-identity-cross-write '/api/v1/admin/loyalty/config' 403 $manager PUT $config | Out-Null
    Request payment-identity-missing-profile '/api/v1/admin/loyalty/add' 404 $admin POST @{userId=999999;points=1;reason='Task053'} | Out-Null
    Request payment-identity-profile '/api/v1/admin/loyalty/add' 200 $admin POST @{userId=1003;points=1;reason='Task053'} | Out-Null
    $before=Sql payment 'select count(*) from loyalty_configurations'
    Stop-App identity-service
    try {
        $config.cinemaId=1
        Request identity-offline-loyalty-write '/api/v1/admin/loyalty/config' 403 $manager PUT $config | Out-Null
        Request identity-offline-booking-scope '/api/v1/admin/bookings' 401 $manager | Out-Null
        Request identity-offline-catalog-isolated '/api/v1/movies' 200 | Out-Null
        Check identity-offline-no-config-insert ((Sql payment 'select count(*) from loyalty_configurations') -eq $before)
    } finally { Restart-App identity-service }
    Stop-App catalog-service
    try {
        Request catalog-offline-identity '/api/v1/admin/users/2/cinema' 400 $admin PUT @{cinemaId=1} | Out-Null
        Request catalog-offline-quote '/api/v1/bookings/hold' 400 $customer POST @{showtimeId=1;seatIds=@(2)} | Out-Null
        Request catalog-offline-wallet-service-isolated '/api/v1/loyalty/config' 200 $customer | Out-Null
    } finally { Restart-App catalog-service }
    $hold=Request failure-fixture-hold '/api/v1/bookings/hold' 200 $customer POST @{showtimeId=1;seatIds=@(2)}
    $failureBooking=$hold.data.id
    if (!$failureBooking) { throw 'Failure fixture missing; no SQL mutation attempted' }
    Sql booking "update bookings set loyalty_points_redeemed=1 where id=$failureBooking" | Out-Null
    Stop-App payment-service
    try {
        Request payment-offline-price-fail-closed "/api/v1/bookings/$failureBooking/items" 400 $customer PUT @{loyaltyPointsToRedeem=0} | Out-Null
        Check payment-offline-no-reprice ((Sql booking "select loyalty_points_redeemed from bookings where id=$failureBooking") -eq 1)
        Request payment-offline-identity-isolated '/api/v1/users/me' 200 $customer | Out-Null
    } finally { Restart-App payment-service }
    Stop-App booking-service
    try {
        Request booking-offline-payment '/api/v1/payments/mock?bookingId=999998' 500 $customer POST | Out-Null
        Check booking-offline-no-payment ((Sql payment 'select count(*) from payments where booking_id=999998') -eq 0)
        $seatMap=Request booking-offline-catalog-seat-map '/api/v1/showtimes/1/seat-map' 503
        Check booking-offline-seat-availability-fails-closed ($seatMap.success -eq $false -and $null -eq $seatMap.data)
        Request booking-offline-catalog-schedule '/api/v1/showtimes/customer-schedule' 503 | Out-Null
        Request booking-offline-catalog-resolve '/api/v1/showtimes/1/resolve' 503 | Out-Null
    } finally { Restart-App booking-service }
} finally {
    $results | Export-Csv (Join-Path $root 'target/task053-http-results.csv') -NoTypeInformation
    $client.Dispose()
    Write-Host "TOTAL=$($results.Count) PASSED=$(@($results | Where-Object Passed).Count) FAILED=$(@($results | Where-Object { !$_.Passed }).Count)"
}
if (@($results | Where-Object { !$_.Passed }).Count -gt 0) { throw 'HTTP regression failures recorded; inspect target/task053-http-results.csv' }
