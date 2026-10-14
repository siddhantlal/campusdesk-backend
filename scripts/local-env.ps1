$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ConfigPath = Join-Path $ProjectRoot '.local\local.env'
function Import-LocalEnvironment {
    if (-not (Test-Path -LiteralPath $ConfigPath)) { throw 'Run scripts/setup-local.ps1 first.' }
    foreach ($line in Get-Content -LiteralPath $ConfigPath) {
        if ($line -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process') }
    }
}
function Assert-NativeSuccess([string]$Action) {
    if ($LASTEXITCODE -ne 0) { throw "$Action failed (exit $LASTEXITCODE)" }
}
