$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/local-env.ps1"
$bin = Join-Path $ProjectRoot '.tools\postgres\bin\pg_ctl.exe'
& $bin -D (Join-Path $ProjectRoot '.local\pgdata') -w stop
Assert-NativeSuccess 'PostgreSQL shutdown'
