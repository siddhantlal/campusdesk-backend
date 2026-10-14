$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/local-env.ps1"
Set-Location -LiteralPath $ProjectRoot
Import-LocalEnvironment
python scripts/benchmark.py
Assert-NativeSuccess 'Benchmark'
