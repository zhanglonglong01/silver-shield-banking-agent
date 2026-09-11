#requires -Version 7.0

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Output = "artifacts/openapi.json"
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "common/project-path-safety.ps1")
$target = Resolve-ProjectChildPath -ProjectRoot $projectRoot `
    -RequestedPath $Output -Purpose "OpenAPI output"
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
Invoke-WebRequest "$BaseUrl/v3/api-docs" -OutFile $target
Write-Host "OpenAPI exported: $target"
