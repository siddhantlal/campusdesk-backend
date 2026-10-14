param([switch]$SkipBuild, [switch]$Background)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/local-env.ps1"
Set-Location -LiteralPath $ProjectRoot
Import-LocalEnvironment
$env:MAVEN_USER_HOME = Join-Path $ProjectRoot '.tools\maven-home'
if (-not $SkipBuild) {
    & .\mvnw.cmd -B -ntp '-Dmaven.repo.local=.tools/m2' -DskipTests package
    Assert-NativeSuccess 'Application build'
}
Write-Host 'CampusDesk: http://localhost:8080/ (Swagger: /swagger-ui.html; Ctrl+C stops the application)'
# Run a copy so Windows can rebuild target/ while the application is running.
$runtime = Join-Path $ProjectRoot '.local\runtime'
New-Item -ItemType Directory -Force $runtime | Out-Null
$runtimeJar = Join-Path $runtime 'campusdesk.jar'
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'target\campusdesk-1.0.0.jar') -Destination $runtimeJar -Force
if ($Background) {
    $process = Start-Process -FilePath (Get-Command java.exe).Source -ArgumentList '-jar', ('"' + $runtimeJar + '"') -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $ProjectRoot '.local\app.log') -RedirectStandardError (Join-Path $ProjectRoot '.local\app-error.log') -PassThru
    @{ id = $process.Id; startTicks = $process.StartTime.Ticks } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $ProjectRoot '.local\app-process.json')
    Write-Host "Started in background, PID $($process.Id). Stop with scripts/stop-app.ps1."
} else {
    java -jar $runtimeJar
    Assert-NativeSuccess 'Application'
}
