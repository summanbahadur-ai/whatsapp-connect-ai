# Generates private local configuration only; never connects to PostgreSQL.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$directory=Join-Path $root 'local-data/config'
New-Item -ItemType Directory -Force $directory | Out-Null
$file=Join-Path $directory 'database.json'
if(Test-Path $file) { throw 'Private configuration already exists. Keep it; do not rotate passwords accidentally.' }
$config=[ordered]@{WC_DB_URL='jdbc:postgresql://localhost:5432/whatsapp_connect_ai'}
$sql=@('-- PRIVATE: run in pgAdmin as administrator after database/bootstrap/00_create_roles.sql.')
foreach($role in @('owner','auth','app','worker')) {
    $bytes=New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $password=[Convert]::ToBase64String($bytes)
    $config["WC_$($role.ToUpper())_PASSWORD"]=$password
    $sql+="ALTER ROLE wc_$role LOGIN PASSWORD '$password';"
}
$config | ConvertTo-Json | Set-Content $file -Encoding UTF8
$sql | Set-Content (Join-Path $directory 'set-role-passwords.sql') -Encoding UTF8
Write-Output 'Private configuration generated under ignored local-data/config. Do not commit or share those files.'
