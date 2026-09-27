param(
    [Parameter(Mandatory = $true)]
    [string]$Service
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'MSS301-Backend-dev'

try {
    if ($Service -eq 'frontend') {
        Set-Location (Join-Path $projectRoot 'MSS301-Frontend-main')
        & npm run dev
        exit $LASTEXITCODE
    }

    if ($Service -eq 'recommendation-service') {
        $servicePath = Join-Path $backendRoot 'cinema-services\recommendation-service'
        Set-Location $servicePath
        $venvPython = Join-Path $servicePath '.venv\Scripts\python.exe'

        if (-not (Test-Path $venvPython)) {
            Write-Host 'Tao moi truong Python va cai thu vien can thiet...' -ForegroundColor Cyan
            & python -m venv .venv
            if ($LASTEXITCODE -ne 0) { throw 'Khong tao duoc Python virtual environment.' }
            & $venvPython -m pip install fastapi==0.115.0 uvicorn==0.30.6 pydantic==2.8.2 python-dotenv==1.0.1
            if ($LASTEXITCODE -ne 0) { throw 'Khong cai duoc thu vien cho recommendation-service.' }
        }

        & $venvPython -m uvicorn main:app --host 127.0.0.1 --port 8000
        exit $LASTEXITCODE
    }

    if ($Service -eq 'cinemaAI') {
        $javaHome = 'C:\Program Files\Java\jdk-21.0.10'
        if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
            throw "Khong tim thay JDK 21 tai $javaHome"
        }

        $env:JAVA_HOME = $javaHome
        $env:Path = "$javaHome\bin;$env:Path"
        # cinemaAI uses the local PostgreSQL container. These values override
        # the legacy parent .env only for this process.
        $env:SERVER_PORT = '8080'
        $env:DB_URL = 'jdbc:postgresql://localhost:5432/cinema_ai'
        $env:DB_USERNAME = 'catalog_user'
        $env:DB_PASSWORD = 'catalog_pass_123'
        $env:HIKARI_MAX_POOL_SIZE = '10'
        $env:HIKARI_MIN_IDLE = '2'
        $env:HIKARI_MAX_LIFETIME = '1800000'
        $env:HIKARI_KEEPALIVE_TIME = '300000'
        $env:HIKARI_CONNECTION_TIMEOUT = '30000'
        $env:HIKARI_IDLE_TIMEOUT = '600000'
        $env:HIKARI_TEST_QUERY = 'SELECT 1'
        $env:JPA_OPEN_IN_VIEW = 'false'
        $env:CACHE_TYPE = 'simple'
        $env:CACHE_NAMES = 'publicMovies,publicMovieDetails'
        $env:FOOD_ORDER_PAYMENT_CLEANUP_ENABLED = 'true'
        $env:FOOD_ORDER_PAYMENT_CLEANUP_FIXED_DELAY_MS = '60000'
        $env:BULK_REFUND_ENABLE = 'true'
        $env:BULK_REFUND_RETRY_MAX_ATTEMPTS = '3'
        $env:BULK_REFUND_RETRY_DELAY_SECONDS = '60'
        $env:BULK_REFUND_NOTIFICATION_ENABLED = 'true'
        $env:VNPAY_BULK_REFUND_TIMEOUT_SECONDS = '30'
        $env:VNPAY_BULK_REFUND_BATCH_SIZE = '10'
        Set-Location (Join-Path $backendRoot 'cinemaAI')
        # The current integration tests still use the previous
        # TicketPricingRuleRequest constructor. Skip test compilation for the
        # local runtime so an unrelated test-source error cannot stop the app.
        & .\mvnw.cmd '-Dmaven.test.skip=true' spring-boot:run
        exit $LASTEXITCODE
    }

    $javaHome = 'C:\Program Files\Java\jdk-21.0.10'
    if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
        throw "Khong tim thay JDK 21 tai $javaHome"
    }

    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;$env:Path"
    Set-Location (Join-Path $backendRoot ("cinema-services\" + $Service))
    & mvn spring-boot:run
    exit $LASTEXITCODE
} catch {
    Write-Host "LOI: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
