param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "demo.admin",
    [string]$Password = "Admin@2026!"
)

$ErrorActionPreference = "Stop"

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$metadata = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/v1/sandbox/reset" `
    -Headers @{ Authorization = "Bearer $($login.accessToken)" }

[ordered]@{
    datasetVersion = $metadata.datasetVersion
    randomSeed = $metadata.randomSeed
    baselineDate = $metadata.baselineDate
    datasetDigest = $metadata.datasetDigest
    rowCounts = $metadata.rowCounts
} | ConvertTo-Json -Depth 4
