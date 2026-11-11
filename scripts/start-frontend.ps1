param([switch]$Production)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$frontend=Join-Path $root 'frontend'
if(!(Test-Path (Join-Path $frontend 'node_modules/next/package.json'))) {
    throw 'Install frontend dependencies first: cd frontend; npm ci'
}
$previousTemp=$env:TEMP
$previousTmp=$env:TMP
$previousCache=$env:npm_config_cache
$previousTelemetry=$env:NEXT_TELEMETRY_DISABLED
try {
    $env:TEMP=Join-Path $root '.cache/temp'
    $env:TMP=$env:TEMP
    $env:npm_config_cache=Join-Path $root '.cache/npm'
    $env:NEXT_TELEMETRY_DISABLED='1'
    New-Item -ItemType Directory -Force $env:TEMP | Out-Null
    Push-Location $frontend
    try {
        $ErrorActionPreference='Continue'
        if($Production) { npm run start } else { npm run dev }
        $result=$LASTEXITCODE
    } finally { $ErrorActionPreference='Stop';Pop-Location }
} finally {
    $env:TEMP=$previousTemp;$env:TMP=$previousTmp
    $env:npm_config_cache=$previousCache;$env:NEXT_TELEMETRY_DISABLED=$previousTelemetry
}
exit $result
