# Runs the essential suite on an ISOLATED local PostgreSQL database; never uses the application database.
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/local-env.ps1"
Set-Location -LiteralPath $ProjectRoot
Import-LocalEnvironment
$testUrl = $env:DB_URL -replace '/campusdesk$', '/campusdesk_test'
if ($testUrl -eq $env:DB_URL) { throw 'Expected a local campusdesk database URL; refusing to run destructive test fixtures.' }
$env:MAVEN_USER_HOME = Join-Path $ProjectRoot '.tools\maven-home'
& .\mvnw.cmd -B -ntp '-Dmaven.repo.local=.tools/m2' "-Dit.db.url=$testUrl" "-Dit.db.user=$env:DB_USER" "-Dit.db.password=$env:DB_PASSWORD" verify
Assert-NativeSuccess 'Essential unit and PostgreSQL integration tests'
