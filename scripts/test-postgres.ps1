$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$pgBin = $env:WC_PG_BIN
if (!$pgBin) {
    $installation = Get-ChildItem 'HKLM:\SOFTWARE\PostgreSQL\Installations' -ErrorAction SilentlyContinue |
        ForEach-Object { Get-ItemProperty $_.PSPath } | Select-Object -First 1
    if ($installation) { $pgBin = Join-Path $installation.'Base Directory' 'bin' }
}
if (!$pgBin -or !(Test-Path (Join-Path $pgBin 'pg_ctl.exe'))) {
    throw 'Set WC_PG_BIN to your PostgreSQL bin directory before running integration tests.'
}
$cluster = Join-Path $root '.cache\cp02-postgres-test'
$privateFile = Join-Path $root '.cache\cp02-test-admin.json'
New-Item -ItemType Directory -Force -Path (Join-Path $root '.cache') | Out-Null
if (!(Test-Path -LiteralPath $privateFile)) {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $password = [Convert]::ToBase64String($bytes)
    @{ password = $password; port = 55432; username = 'wc_test_admin' } | ConvertTo-Json | Set-Content -LiteralPath $privateFile -Encoding UTF8
}
$config = Get-Content -LiteralPath $privateFile -Raw | ConvertFrom-Json
if (!(Test-Path -LiteralPath (Join-Path $cluster 'PG_VERSION'))) {
    $pwFile = Join-Path $root '.cache\cp02-test-init-password'
    [System.IO.File]::WriteAllText($pwFile, $config.password)
    try {
        & "$pgBin\initdb.exe" -D $cluster -U $config.username --pwfile=$pwFile --auth=scram-sha-256 --encoding=UTF8 --locale=C *> (Join-Path $root '.cache\cp02-initdb.log')
        if ($LASTEXITCODE) { throw 'initdb failed; see private tooling log.' }
    } finally { Remove-Item -LiteralPath $pwFile -ErrorAction SilentlyContinue }
}
& "$pgBin\pg_ctl.exe" -D $cluster status *> $null
if ($LASTEXITCODE -ne 0) {
    & "$pgBin\pg_ctl.exe" -D $cluster -l (Join-Path $root '.cache\cp02-postgres.log') -o '-h 127.0.0.1 -p 55432' -w start
    if ($LASTEXITCODE) { throw 'Temporary PostgreSQL startup failed.' }
}
Write-Output 'Isolated PostgreSQL test cluster is ready at 127.0.0.1:55432; no existing database was accessed.'
