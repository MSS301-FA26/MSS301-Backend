param([switch]$Stop)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$stateFile = Join-Path $root 'target/task053-state.clixml'
if ($Stop) {
    $state = Import-Clixml $stateFile
    foreach ($app in $state.Apps) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($app.Pid)"
        if ($process -and $process.CommandLine -and $process.CommandLine.Contains($app.Jar)) {
            Stop-Process -Id $app.Pid
        }
    }
    foreach ($name in $state.Containers) {
        $existing = docker ps -aq --filter "name=^/$name`$"
        if (!$existing) { continue } # --rm containers can disappear after a host restart.
        $labels = docker inspect $name --format '{{json .Config.Labels}}' 2>$null | ConvertFrom-Json
        if ($labels.'task.owner' -eq 'task053') { docker stop $name | Out-Null }
    }
    $network = docker network ls -q --filter name=^task053-local$
    if ($network) {
        $labels = docker network inspect task053-local --format '{{json .Labels}}' | ConvertFrom-Json
        if ($labels.'task.owner' -ne 'task053') { throw 'Network ownership check failed' }
        docker network rm task053-local | Out-Null
        if ($LASTEXITCODE) { throw 'Owned network cleanup failed' }
    }
    Remove-Item -LiteralPath $stateFile
    return
}
if (Test-Path $stateFile) { throw 'Existing state: inspect/stop the previous owned runtime first.' }
$ports = @(15800,15801,15802,15803,15804,57101,57102,57103,57104,57172)
$busy = Get-NetTCPConnection -State Listen | Where-Object LocalPort -In $ports
if ($busy) { throw 'A reserved task053 port is already in use.' }
New-Item -ItemType Directory -Force (Join-Path $root 'target') | Out-Null
$password = [guid]::NewGuid().ToString('N')
$jwt = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$internal = [guid]::NewGuid().ToString('N')
$gateway = [guid]::NewGuid().ToString('N')
$state = @{ Apps=@(); Containers=@(); Password=$password; Internal=$internal; Gateway=$gateway }
docker network create --label task.owner=task053 task053-local | Out-Null
if ($LASTEXITCODE) { throw 'Network creation failed' }
$services = @('identity','catalog','booking','payment')
for ($i=0; $i -lt 4; $i++) {
    $service = $services[$i]
    $name = "task053-$service-db"
    docker run -d --rm --name $name --label task.owner=task053 --network task053-local --publish "127.0.0.1:$($ports[5+$i]):5432" --tmpfs /var/lib/postgresql/data -e "POSTGRES_DB=${service}_db" -e "POSTGRES_USER=${service}_verify" -e "POSTGRES_PASSWORD=$password" postgres:16-alpine | Out-Null
    if ($LASTEXITCODE) { throw "Cannot create $name" }
    $state.Containers += $name
    $state | Export-Clixml $stateFile
}
docker run -d --rm --name task053-rabbit --label task.owner=task053 --network task053-local --publish 127.0.0.1:57172:5672 --tmpfs /var/lib/rabbitmq -e RABBITMQ_DEFAULT_USER=local_verify -e "RABBITMQ_DEFAULT_PASS=$password" rabbitmq:3.13-alpine | Out-Null
if ($LASTEXITCODE) { throw 'Cannot create broker' }
$state.Containers += 'task053-rabbit'
$state | Export-Clixml $stateFile
foreach ($service in $services) {
    $ready = $false
    for ($retry=0; $retry -lt 60; $retry++) {
        docker exec "task053-$service-db" pg_isready -h 127.0.0.1 -U "${service}_verify" -d "${service}_db" 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready=$true; break }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw "Database not ready: $service" }
}
$modules = @('identity-service','catalog-service','booking-service','payment-service','api-gateway')
for ($i=0; $i -lt 5; $i++) {
    $module=$modules[$i]
    $moduleRoot=Join-Path $root $module
    $jar=Join-Path $moduleRoot "target/$module-1.0.0-SNAPSHOT.jar"
    if (!(Test-Path $jar)) { throw "Build missing: $jar" }
    $appPort=if ($i -eq 4) {15800} else {15801+$i}
    $appArgs=@('-Xmx320m', '-jar', $jar, '--spring.config.import=file:.env.example[.properties]', '--logging.pattern.level=%5p', "--APPLICATION_NAME=$module", "--server.port=$appPort", '--server.address=127.0.0.1', "--JWT_SECRET=$jwt", "--INTERNAL_GATEWAY_SECRET=$gateway", "--INTERNAL_SERVICE_SECRET=$internal", "--AI_GATEWAY_SECRET=$gateway", '--MAIL_ENABLED=false', '--management.health.mail.enabled=false', '--GATEWAY_URL=http://127.0.0.1:15800', '--IDENTITY_SERVICE_URL=http://127.0.0.1:15801', '--CATALOG_SERVICE_URL=http://127.0.0.1:15802', '--BOOKING_SERVICE_URL=http://127.0.0.1:15803', '--PAYMENT_SERVICE_URL=http://127.0.0.1:15804', '--RECOMMENDATION_SERVICE_URL=http://127.0.0.1:15805', '--IDENTITY_SERVICE_URI=http://127.0.0.1:15801', '--CATALOG_SERVICE_URI=http://127.0.0.1:15802', '--BOOKING_SERVICE_URI=http://127.0.0.1:15803', '--PAYMENT_SERVICE_URI=http://127.0.0.1:15804', '--RECOMMENDATION_SERVICE_URI=http://127.0.0.1:15805', '--RABBITMQ_HOST=127.0.0.1', '--RABBITMQ_PORT=57172', '--RABBITMQ_USERNAME=local_verify', "--RABBITMQ_PASSWORD=$password", '--BOOKING_HOLD_CLEANUP_ENABLED=false', '--FOOD_ORDER_PAYMENT_CLEANUP_ENABLED=false', '--BULK_REFUND_ENABLE=false', '--BULK_REFUND_NOTIFICATION_ENABLED=false', '--VNPAY_PAY_URL=http://127.0.0.1:15806/disabled', '--VNPAY_API_QUERY_URL=http://127.0.0.1:15806/disabled', "--SEED_ADMIN_PASSWORD=$password", "--SEED_MANAGER_PASSWORD=$password", "--SEED_STAFF_PASSWORD=$password")
    if ($i -lt 4) {
        $service=$services[$i]
        $appArgs += @("--spring.datasource.url=jdbc:postgresql://127.0.0.1:$($ports[5+$i])/${service}_db?ApplicationName=task053-$service", "--spring.datasource.username=${service}_verify", "--spring.datasource.password=$password", '--spring.jpa.hibernate.ddl-auto=validate')
    }
    $process=Start-Process java -WindowStyle Hidden -ArgumentList $appArgs -WorkingDirectory $moduleRoot -RedirectStandardOutput (Join-Path $root "target/task053-$module.log") -RedirectStandardError (Join-Path $root "target/task053-$module.err.log") -PassThru
    $state.Apps += @{Name=$module;Pid=$process.Id;Jar=$jar;Port=$appPort;Args=$appArgs;WorkingDirectory=$moduleRoot}
    $state | Export-Clixml $stateFile
    Write-Output "$module PID=$($process.Id) port=$appPort"
}
Write-Output 'Owned runtime launched. Inspect all five health endpoints before testing. State contains disposable secrets; do not publish it.'
