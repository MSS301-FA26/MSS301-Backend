$ErrorActionPreference = 'Continue'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeDirectory = Join-Path $projectRoot '.local-runtime'

if (-not (Test-Path $runtimeDirectory)) {
    Write-Host 'Khong co service chay nen do Start-Local.ps1 quan ly.' -ForegroundColor Yellow
    exit 0
}

Get-ChildItem -Path $runtimeDirectory -Filter '*.pid' | ForEach-Object {
    $service = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
    $serviceProcessId = Get-Content -Path $_.FullName -Raw
    $process = Get-Process -Id $serviceProcessId -ErrorAction SilentlyContinue
    if ($null -ne $process) {
        Write-Host "[$service] Dang dung process $serviceProcessId..." -ForegroundColor Cyan
        & taskkill.exe /PID $serviceProcessId /T /F *> $null
    }
    Remove-Item -Path $_.FullName -Force
}

# spring-boot:run can start a child Java process that outlives its Maven
# launcher. Stop only listeners that clearly belong to this workspace.
$servicePorts = 3000, 8000, 8080, 8081, 8082, 8083, 8084
foreach ($port in $servicePorts) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            $listenerProcessId = $_
            $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$listenerProcessId" -ErrorAction SilentlyContinue
            $commandLine = [string]$processInfo.CommandLine
            $isWorkspaceService = $commandLine -match 'MSS301-Backend-dev|MSS301-Frontend-main|com\.cinemaai\.|com\.sba301\.cinemaai\.'
            if ($isWorkspaceService) {
                Write-Host "[port $port] Dang dung process $listenerProcessId..." -ForegroundColor Cyan
                & taskkill.exe /PID $listenerProcessId /T /F *> $null
            }
        }
}

Write-Host 'Da gui lenh dung cac service chay nen.' -ForegroundColor Green
