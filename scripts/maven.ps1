param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArgs)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$jdkDirectory = Join-Path $env:USERPROFILE '.jdks'
if (Test-Path -LiteralPath $jdkDirectory) {
    $candidates += @(Get-ChildItem -LiteralPath $jdkDirectory -Directory | Sort-Object Name -Descending | Select-Object -ExpandProperty FullName)
}
$jdkHome = $null
foreach ($candidate in $candidates) {
    $release = Join-Path $candidate 'release'
    if ((Test-Path -LiteralPath $release) -and (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe'))) {
        if ((Get-Content -LiteralPath $release) -match '^JAVA_VERSION="25(?:[.\-+\"]|$)') {
            $jdkHome = $candidate
            break
        }
    }
}
if (!$jdkHome) { throw 'JDK 25 was not found. Set JAVA_HOME to an installed JDK 25 and reopen the terminal.' }

$oldJavaHome = $env:JAVA_HOME
$oldPath = $env:PATH
$oldMavenHome = $env:MAVEN_USER_HOME
$oldTemp = $env:TEMP
$oldTmp = $env:TMP
$oldTestPassword = $env:CP02_TEST_ADMIN_PASSWORD
$result = 1
try {
    if ($MavenArgs -contains 'verify' -or $MavenArgs -contains 'integration-test') {
        & (Join-Path $PSScriptRoot 'test-postgres.ps1')
        $privateFixture = Get-Content (Join-Path $projectRoot '.cache/cp02-test-admin.json') -Raw | ConvertFrom-Json
        $env:CP02_TEST_ADMIN_PASSWORD = $privateFixture.password
    }
    $env:JAVA_HOME = $jdkHome
    $env:PATH = (Join-Path $jdkHome 'bin') + ';' + $env:PATH
    $env:MAVEN_USER_HOME = Join-Path $projectRoot '.cache\maven-home'
    $env:TEMP = Join-Path $projectRoot '.cache\temp'
    $env:TMP = $env:TEMP
    New-Item -ItemType Directory -Force -Path $env:MAVEN_USER_HOME,$env:TEMP | Out-Null
    Push-Location (Join-Path $projectRoot 'backend')
    try {
        # Windows PowerShell maps native stderr warnings to ErrorRecords.
        # Trust Maven's process exit code instead of aborting on a warning.
        $ErrorActionPreference = 'Continue'
        & .\mvnw.cmd "-Dmaven.repo.local=$projectRoot/.cache/maven-repository" @MavenArgs
        $result = $LASTEXITCODE
    } finally { $ErrorActionPreference = 'Stop'; Pop-Location }
} finally {
    $env:JAVA_HOME = $oldJavaHome
    $env:PATH = $oldPath
    $env:MAVEN_USER_HOME = $oldMavenHome
    $env:TEMP = $oldTemp
    $env:TMP = $oldTmp
    $env:CP02_TEST_ADMIN_PASSWORD = $oldTestPassword
}
exit $result
