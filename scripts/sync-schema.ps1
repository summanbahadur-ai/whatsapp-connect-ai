param([switch]$Check)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$migrationDirectory = Join-Path $root 'backend\src\main\resources\db\migration'
$files = @(Get-ChildItem -LiteralPath $migrationDirectory -File -Filter 'V*__*.sql' | Sort-Object { [int]($_.Name.Split('__')[0].Substring(1)) })
if ($files.Count -eq 0) { throw 'No versioned migrations found.' }
$text = "-- GENERATED MASTER SCHEMA: run scripts/sync-schema.ps1 after EVERY SQL structure change.`n-- Fresh-install/reference only; NEVER replay against an existing database.`n-- Recommended setup: run bootstrap files, then let Java/Flyway apply migrations.`n-- Manual installation needs explicit Flyway baseline adoption before Java migration.`nBEGIN;`nDO `$`$ BEGIN`n  IF current_database() <> 'whatsapp_connect_ai' AND current_database() !~ '^whatsapp_connect_ai_test_[a-f0-9]{32}$' THEN`n    RAISE EXCEPTION 'Wrong database: expected whatsapp_connect_ai or an isolated test database';`n  END IF;`n  IF to_regnamespace('identity') IS NOT NULL OR to_regclass('public.accounts') IS NOT NULL THEN`n    RAISE EXCEPTION 'Master schema requires a fresh database; use incremental Flyway migrations instead';`n  END IF;`nEND `$`$;`n"
foreach ($file in $files) {
    $body = [System.IO.File]::ReadAllText($file.FullName).Replace("`r`n", "`n").TrimEnd()
    $text += "`n-- BEGIN MIGRATION: $($file.Name)`n$body`n-- END MIGRATION: $($file.Name)`n"
}
$text += "`nCOMMIT;`n"
$target = Join-Path $root 'database\schema.sql'
if ($Check) {
    if (!(Test-Path -LiteralPath $target) -or [System.IO.File]::ReadAllText($target).Replace("`r`n", "`n") -cne $text) {
        throw 'database/schema.sql is stale. Run scripts/sync-schema.ps1 and review it with the migration.'
    }
    Write-Output 'Master schema is synchronized with every versioned migration.'
} else {
    [System.IO.File]::WriteAllText($target, $text, [System.Text.UTF8Encoding]::new($false))
    Write-Output 'Updated database/schema.sql.'
}
