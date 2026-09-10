[CmdletBinding()]
param(
    [switch]$SkipTests,
    [switch]$Migrate,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -LiteralPath $repoRoot

if (-not (Test-Path -LiteralPath ".\pom.xml")) {
    throw "pom.xml not found. Run this script from the CinemaAI repository."
}

$maven = ".\mvnw.cmd"
if (-not (Test-Path -LiteralPath $maven)) {
    throw "Maven Wrapper not found: $maven"
}

Write-Host "== CinemaAI bootstrap =="
Write-Host "Repository: $repoRoot"

& $maven --version
if ($LASTEXITCODE -ne 0) {
    throw "Maven/Java environment check failed."
}

Write-Host "`n== Restore dependencies =="
& $maven -q dependency:go-offline
if ($LASTEXITCODE -ne 0) {
    throw "Dependency restore failed."
}

if ($Migrate) {
    $pom = Get-Content -Raw -LiteralPath ".\pom.xml"
    $properties = Get-Content -Raw -LiteralPath ".\src\main\resources\application.properties"
    $hasFlywayRuntime = $pom -match "flyway-core"
    $flywayEnabled = $properties -match "(?m)^\s*spring\.flyway\.enabled\s*=\s*true\s*$"

    if (-not $hasFlywayRuntime -or -not $flywayEnabled) {
        throw @"
Migration requested but the repository is not migration-ready:
- flyway-core dependency present: $hasFlywayRuntime
- spring.flyway.enabled=true: $flywayEnabled

Migration SQL exists under src/main/resources/db/migration, but runtime currently uses
Hibernate ddl-auto=update with Flyway disabled. Add/enable Flyway in a dedicated task,
then rerun .\init.ps1 -Migrate against a disposable database first.
"@
    }

    Write-Host "`n== Validate/start Flyway migration =="
    & $maven -DskipTests spring-boot:run `
        "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --spring.jpa.hibernate.ddl-auto=validate"
    if ($LASTEXITCODE -ne 0) {
        throw "Database migration/startup validation failed."
    }
}

Write-Host "`n== Build =="
$buildGoal = if ($Clean) { "clean" } else { "package" }
if ($Clean) {
    & $maven clean package -DskipTests
} else {
    & $maven package -DskipTests
}
if ($LASTEXITCODE -ne 0) {
    throw "Build failed."
}

if ($SkipTests) {
    Write-Host "`nTests skipped by -SkipTests."
} else {
    Write-Host "`n== Test =="
    & $maven test
    if ($LASTEXITCODE -ne 0) {
        throw "Tests failed. See target/surefire-reports and progress.md for known baseline issues."
    }
}

Write-Host "`nCinemaAI initialization completed successfully."

