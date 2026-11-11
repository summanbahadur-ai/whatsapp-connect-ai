param([switch]$LiveJava)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $root 'frontend')
try {
    foreach($task in @('test','lint','typecheck','build','test:browser')) {
        $ErrorActionPreference='Continue'
        npm run $task
        $code=$LASTEXITCODE
        $ErrorActionPreference='Stop'
        if($code) { throw "Frontend verification failed at $task (exit $code)." }
    }
    if($LiveJava) {
        $ErrorActionPreference='Continue'
        npm run test:java
        $code=$LASTEXITCODE
        $ErrorActionPreference='Stop'
        if($code) { throw 'Real Java browser smoke failed. Start Java8081 and frontend3001 first.' }
    }
} finally { $ErrorActionPreference='Stop';Pop-Location }
