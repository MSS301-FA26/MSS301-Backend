param(
    [switch]$SkipRecommendation,
    [switch]$Background,
    [switch]$Monolith
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'MSS301-Backend-dev'
$runner = Join-Path $PSScriptRoot 'Run-LocalService.ps1'
$runtimeDirectory = Join-Path $projectRoot '.local-runtime'

if ($Background -and -not (Test-Path $runtimeDirectory)) {
    New-Item -ItemType Directory -Path $runtimeDirectory | Out-Null
}

function Test-PortInUse {
    param([int]$Port)
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-ServiceTerminal {
    param(
        [string]$Name,
        [int]$Port
    )

    if (Test-PortInUse $Port) {
        Write-Host "[$Name] Bo qua: cong $Port dang duoc su dung." -ForegroundColor Yellow
        return
    }

    if ($Background) {
        $outputLog = Join-Path $runtimeDirectory "$Name.out.log"
        $errorLog = Join-Path $runtimeDirectory "$Name.err.log"
        $pidFile = Join-Path $runtimeDirectory "$Name.pid"
        Write-Host "[$Name] Dang chay nen. Log: .local-runtime\\$Name.out.log" -ForegroundColor Cyan
        $process = Start-Process powershell.exe -WindowStyle Hidden -PassThru -ArgumentList @(
            '-ExecutionPolicy', 'Bypass', '-File', $runner, '-Service', $Name
        ) -RedirectStandardOutput $outputLog -RedirectStandardError $errorLog
        Set-Content -Path $pidFile -Value $process.Id -NoNewline
    } else {
        Write-Host "[$Name] Dang mo terminal..." -ForegroundColor Cyan
        Start-Process powershell.exe -ArgumentList @(
            '-NoExit', '-ExecutionPolicy', 'Bypass', '-File', $runner, '-Service', $Name
        )
    }
}

function Ensure-MonolithDatabase {
    Write-Host 'Kiem tra database cinema_ai...' -ForegroundColor Cyan
    & docker start cinema-catalog-db *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Khong khoi dong duoc PostgreSQL container cinema-catalog-db.'
    }

    & docker exec cinema-catalog-db psql -U catalog_user -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'cinema_ai'" | Out-Null
    $databaseQueryResult = & docker exec cinema-catalog-db psql -U catalog_user -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'cinema_ai'"
    $databaseExists = $null -ne $databaseQueryResult -and (($databaseQueryResult -join '').Trim() -eq '1')
    if (-not $databaseExists) {
        Write-Host 'Tao database local cinema_ai...' -ForegroundColor Cyan
        & docker exec cinema-catalog-db psql -U catalog_user -d postgres -c 'CREATE DATABASE cinema_ai' | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Khong tao duoc database cinema_ai.' }
    }
}

if ($Monolith) {
    Write-Host 'Che do cinemaAI day du: dung cac service nen cu de giai phong cong 8080...' -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot 'Stop-Local.ps1')
    Start-Sleep -Seconds 2
    if (Test-PortInUse 8080) {
        throw 'Cong 8080 van dang duoc su dung. Hay dung process dang chay tren cong nay truoc khi chay -Monolith.'
    }

    Ensure-MonolithDatabase
    Start-ServiceTerminal -Name 'cinemaAI' -Port 8080
    Start-ServiceTerminal -Name 'frontend' -Port 3000
    Write-Host 'Da bat cinemaAI day du va frontend. Doi backend khoi dong, sau do vao http://localhost:3000.' -ForegroundColor Green
    exit 0
}

Write-Host 'Kiem tra RabbitMQ...' -ForegroundColor Cyan
$rabbitExists = $false
try {
    & docker container inspect cinema-rabbitmq *> $null
    $rabbitExists = $LASTEXITCODE -eq 0
} catch {
    $rabbitExists = $false
}

if ($rabbitExists) {
    & docker start cinema-rabbitmq *> $null
    Write-Host 'RabbitMQ da san sang hoac dang duoc khoi dong.' -ForegroundColor Green
} else {
    Push-Location $backendRoot
    try {
        & docker compose up -d rabbitmq
    } finally {
        Pop-Location
    }
}

Start-ServiceTerminal -Name 'identity-service' -Port 8081
Start-ServiceTerminal -Name 'catalog-service' -Port 8082
Start-ServiceTerminal -Name 'booking-service' -Port 8083
Start-ServiceTerminal -Name 'payment-service' -Port 8084
Start-ServiceTerminal -Name 'api-gateway' -Port 8080

if (-not $SkipRecommendation) {
    Start-ServiceTerminal -Name 'recommendation-service' -Port 8000
}

Start-ServiceTerminal -Name 'frontend' -Port 3000

Write-Host ''
if ($Background) {
    Write-Host 'Cac service dang chay nen. Xem log trong .local-runtime; dung bang .\scripts\Stop-Local.ps1.' -ForegroundColor Green
} else {
    Write-Host 'Da mo cac terminal can thiet. Doi cac service hien "Started" truoc khi vao http://localhost:3000.' -ForegroundColor Green
}
Write-Host 'Service nao da chiem cong se duoc bo qua de tranh loi trung cong.' -ForegroundColor Yellow
