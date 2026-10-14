# Local development only: portable PostgreSQL, no Docker, services, or system installation.
param([int]$DatabasePort = 5432)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/local-env.ps1"
Set-Location -LiteralPath $ProjectRoot
if ($DatabasePort -lt 1024 -or $DatabasePort -gt 65535) { throw 'Choose a database port between 1024 and 65535.' }
New-Item -ItemType Directory -Force '.tools', '.local' | Out-Null
function New-RandomSecret {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}
if (-not (Test-Path -LiteralPath $ConfigPath)) {
    $dbPassword = New-RandomSecret
    $jwt = New-RandomSecret
    $adminPassword = New-RandomSecret
    @("DB_URL=jdbc:postgresql://localhost:$DatabasePort/campusdesk", 'DB_USER=campusdesk', "DB_PASSWORD=$dbPassword",
      "JWT_SECRET=$jwt", 'BOOTSTRAP_ADMIN_ENABLED=true', 'ADMIN_NAME=Local Campus Administrator',
      'ADMIN_EMAIL=admin@campusdesk.test', "ADMIN_PASSWORD=$adminPassword", 'CORS_ORIGINS=', 'DEMO_ENABLED=false') |
        Set-Content -LiteralPath $ConfigPath -Encoding ascii
}
Import-LocalEnvironment
$pgRoot = Join-Path $ProjectRoot '.tools\postgres'
$pgBin = Join-Path $pgRoot 'bin'
if (-not (Test-Path -LiteralPath (Join-Path $pgBin 'postgres.exe'))) {
    $url = 'https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/17.6.0/embedded-postgres-binaries-windows-amd64-17.6.0.jar'
    $archive = Join-Path $ProjectRoot '.tools\postgres.zip'
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $archive
    $expected = 'c91d39b6db91cf8811e3b20d941e74a0396e277360873821daab47cdb4c1282c'
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath $archive).Hash.ToLowerInvariant() -ne $expected) { throw 'PostgreSQL archive checksum mismatch' }
    $unpack = Join-Path $ProjectRoot '.tools\pg-archive'
    Expand-Archive -LiteralPath $archive -DestinationPath $unpack -Force
    New-Item -ItemType Directory -Force $pgRoot | Out-Null
    tar -xJf (Join-Path $unpack 'postgres-windows-x86_64.txz') -C $pgRoot
    Assert-NativeSuccess 'PostgreSQL extraction'
}
$data = Join-Path $ProjectRoot '.local\pgdata'
if (-not (Test-Path -LiteralPath (Join-Path $data 'PG_VERSION'))) {
    $passwordFile = Join-Path $ProjectRoot '.local\pg-password'
    $env:DB_PASSWORD | Set-Content -LiteralPath $passwordFile -Encoding ascii
    & (Join-Path $pgBin 'initdb.exe') -D $data -U $env:DB_USER --pwfile=$passwordFile --auth-host=scram-sha-256 --auth-local=scram-sha-256 --encoding=UTF8 --locale=C
    Assert-NativeSuccess 'Database initialization'
    # Credentials stay in the ignored config; initdb's temporary password file is no longer needed.
    Remove-Item -LiteralPath $passwordFile
}
& (Join-Path $pgBin 'pg_ctl.exe') -D $data status *> $null
if ($LASTEXITCODE -ne 0) {
    if ($env:DB_URL -notmatch 'localhost:(\d+)/') { throw 'Local config must use localhost.' }
    $configuredPort = [int]$Matches[1]
    & (Join-Path $pgBin 'pg_ctl.exe') -D $data -l (Join-Path $ProjectRoot '.local\postgres.log') -o "-h 127.0.0.1 -p $configuredPort -c timezone=UTC" -w start
    Assert-NativeSuccess 'Database startup'
}
# The portable runtime excludes client utilities; a tiny JDBC helper creates the two databases.
$env:MAVEN_USER_HOME = Join-Path $ProjectRoot '.tools\maven-home'
& .\mvnw.cmd -B -ntp '-Dmaven.repo.local=.tools/m2' dependency:copy '-Dartifact=org.postgresql:postgresql:42.7.7' '-DoutputDirectory=.tools/jdbc'
Assert-NativeSuccess 'JDBC client download'
java --class-path '.tools/jdbc/postgresql-42.7.7.jar' scripts/LocalDatabase.java
Assert-NativeSuccess 'Database creation'
Write-Host "Local PostgreSQL is ready. Credentials are in $ConfigPath (ignored by Git)."
Write-Host 'Next: powershell -ExecutionPolicy Bypass -File scripts/run-local.ps1'
