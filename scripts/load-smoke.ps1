#requires -Version 7.0

param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateRange(1, 2000)][int]$Requests = 100,
    [ValidateRange(1, 100)][int]$Concurrency = 10
)

$ErrorActionPreference = "Stop"
$loginBody = @{ username = "demo.alice"; password = "Demo@2026!" } | ConvertTo-Json
$login = Invoke-RestMethod -NoProxy -Method Post -Uri "$BaseUrl/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$token = $login.accessToken

$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.UseProxy = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$client.DefaultRequestHeaders.Authorization =
    [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $token)
$results = [System.Collections.Generic.List[object]]::new()

try {
    $remaining = $Requests
    while ($remaining -gt 0) {
        $batchSize = [Math]::Min($Concurrency, $remaining)
        $watch = [Diagnostics.Stopwatch]::StartNew()
        $tasks = @(
            1..$batchSize | ForEach-Object {
                $client.GetAsync("$BaseUrl/api/v1/accounts")
            }
        )
        [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)
        $watch.Stop()

        foreach ($task in $tasks) {
            $response = $task.GetAwaiter().GetResult()
            try {
                $results.Add([pscustomobject]@{
                    status = [int]$response.StatusCode
                    durationMs = $watch.ElapsedMilliseconds
                })
            } finally {
                $response.Dispose()
            }
        }
        $remaining -= $batchSize
    }
} finally {
    $client.Dispose()
}

$ordered = @($results | Sort-Object durationMs)
$failures = @($results | Where-Object status -ne 200)
$p95Index = [Math]::Max(0, [Math]::Ceiling($ordered.Count * 0.95) - 1)
$summary = [ordered]@{
    requests = $Requests
    concurrency = $Concurrency
    successes = $Requests - $failures.Count
    failures = $failures.Count
    minMs = $ordered[0].durationMs
    p95Ms = $ordered[$p95Index].durationMs
    maxMs = $ordered[-1].durationMs
}
$summary | ConvertTo-Json
if ($failures.Count -gt 0) { throw "Load smoke test had $($failures.Count) failures" }
