param([switch]$Database)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$previous=@{}
try {
    $profiles='local'
    if($Database) {
        $file=Join-Path $root 'local-data/config/database.json'
        if(!(Test-Path $file)) { throw 'Run scripts/configure-local-database.ps1 and follow database/README.md first.' }
        $config=Get-Content $file -Raw | ConvertFrom-Json
        foreach($key in @('WC_DB_URL','WC_OWNER_PASSWORD','WC_AUTH_PASSWORD','WC_APP_PASSWORD','WC_WORKER_PASSWORD')) {
            $previous[$key]=[Environment]::GetEnvironmentVariable($key,'Process')
            if(!$config.$key) { throw "Missing private setting $key" }
            [Environment]::SetEnvironmentVariable($key,$config.$key,'Process')
        }
        $profiles='local,database'
    }
    & (Join-Path $PSScriptRoot 'maven.ps1') spring-boot:run "-Dspring-boot.run.profiles=$profiles"
} finally {
    foreach($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key,$previous[$key],'Process') }
}
