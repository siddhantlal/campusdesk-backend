$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/local-env.ps1"
$recordPath = Join-Path $ProjectRoot '.local\app-process.json'
if (-not (Test-Path -LiteralPath $recordPath)) { Write-Host 'No background application record.'; exit 0 }
$record = Get-Content -LiteralPath $recordPath | ConvertFrom-Json
$process = Get-Process -Id $record.id -ErrorAction SilentlyContinue
if ($process -and $process.ProcessName -eq 'java' -and $process.StartTime.Ticks -eq $record.startTicks) {
    Stop-Process -Id $process.Id
    Write-Host 'Stopped CampusDesk.'
} else { Write-Host 'The recorded CampusDesk process is no longer running.' }
Remove-Item -LiteralPath $recordPath
