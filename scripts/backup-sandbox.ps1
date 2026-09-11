#requires -Version 7.0

param(
    [string]$OutputDirectory = "artifacts/backups",
    [string]$ProjectName = "silver-shield"
)

$ErrorActionPreference = "Stop"
$resolvedRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "common/project-path-safety.ps1")
$targetDirectory = Resolve-ProjectChildPath -ProjectRoot $resolvedRoot `
    -RequestedPath $OutputDirectory -Purpose "Backup output"
[IO.Directory]::CreateDirectory($targetDirectory) | Out-Null
$target = Join-Path $targetDirectory ("silver-shield-sandbox-{0}.sql" -f (Get-Date -Format "yyyyMMdd-HHmmss"))

Push-Location $resolvedRoot
try {
    & docker compose -p $ProjectName exec -T mysql sh -lc 'exec mysqldump --single-transaction --no-tablespaces -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' |
        Set-Content -LiteralPath $target -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw "mysqldump failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

Write-Host "Sandbox backup created: $target"
