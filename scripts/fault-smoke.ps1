#requires -Version 7.0

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ProjectName = "silver-shield",
    [ValidateRange(10, 180)][int]$RecoveryTimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

function Health-Status {
    try { (Invoke-WebRequest "$BaseUrl/actuator/health" -SkipHttpErrorCheck -TimeoutSec 5).StatusCode }
    catch { 0 }
}

function Wait-Healthy([string]$Dependency) {
    $deadline = (Get-Date).AddSeconds($RecoveryTimeoutSeconds)
    do {
        if ((Health-Status) -eq 200) { return }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "API did not recover after restarting $Dependency"
}

$results = [ordered]@{}
try {
    & docker compose -p $ProjectName stop redis | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Could not stop Redis" }
    Start-Sleep -Seconds 3
    $results.redisStoppedHealth = Health-Status
    if ($results.redisStoppedHealth -eq 200) { throw "Health stayed UP while Redis was stopped" }
} finally {
    & docker compose -p $ProjectName start redis | Out-Host
}
Wait-Healthy "Redis"
$results.redisRecoveredHealth = Health-Status

try {
    & docker compose -p $ProjectName stop mysql | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Could not stop MySQL" }
    Start-Sleep -Seconds 3
    $results.mysqlStoppedHealth = Health-Status
    if ($results.mysqlStoppedHealth -eq 200) { throw "Health stayed UP while MySQL was stopped" }
} finally {
    & docker compose -p $ProjectName start mysql | Out-Host
}
Wait-Healthy "MySQL"
$results.mysqlRecoveredHealth = Health-Status
$results.result = "PASS"
$results | ConvertTo-Json
