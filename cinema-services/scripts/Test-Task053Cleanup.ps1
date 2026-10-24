# Dependency-free regression test. All process, Docker and deletion operations are fakes.
$ErrorActionPreference = 'Stop'
$launcher = Join-Path $PSScriptRoot 'Start-Task053Runtime.ps1'
& {
    $stopped = [Collections.Generic.List[int]]::new()
    $removed = [Collections.Generic.List[string]]::new()
    function Import-Clixml {
        @{ Apps = @(
            @{ Pid=91001; Jar='owned.jar' }, # no longer exists
            @{ Pid=91002; Jar='owned.jar' }, # PID reused; command line inaccessible
            @{ Pid=91003; Jar='owned.jar' }, # unrelated readable process
            @{ Pid=91004; Jar='owned.jar' }  # owned JVM
        ); Containers=@() }
    }
    function Get-CimInstance {
        param($ClassName, $Filter)
        switch ($Filter) {
            'ProcessId=91001' { return $null }
            'ProcessId=91002' { return [pscustomobject]@{CommandLine=$null} }
            'ProcessId=91003' { return [pscustomobject]@{CommandLine='java -jar unrelated.jar'} }
            'ProcessId=91004' { return [pscustomobject]@{CommandLine='java -jar owned.jar'} }
            default { throw "Unexpected process lookup: $Filter" }
        }
    }
    function Stop-Process { param([int]$Id) $stopped.Add($Id) }
    function docker {
        if (($args -join ' ') -ne 'network ls -q --filter name=^task053-local$') {
            throw 'Unexpected Docker operation in cleanup test'
        }
        return $null # no task network remains
    }
    function Remove-Item { param($LiteralPath) $removed.Add($LiteralPath) }
    & $launcher -Stop
    if ($stopped.Count -ne 1 -or $stopped[0] -ne 91004) {
        throw 'Cleanup must stop only the matching owned JVM'
    }
    $expectedState = Join-Path (Split-Path $PSScriptRoot -Parent) 'target/task053-state.clixml'
    if ($removed.Count -ne 1 -or $removed[0] -ne $expectedState) {
        throw 'Cleanup must remove only its credential state'
    }
    Write-Output 'TASK053_CLEANUP_REGRESSION=PASS (missing/null/reused/owned PID cases)'
}
