#requires -Version 7.0

$ErrorActionPreference = 'Stop'
$scriptsRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$projectRoot = [IO.Path]::GetFullPath((Join-Path $scriptsRoot '..'))
. (Join-Path $scriptsRoot 'common/project-path-safety.ps1')

$script:passed = 0

function Assert-Equal([string]$Actual, [string]$Expected, [string]$Case) {
    if ($Actual -ne $Expected) {
        throw "$Case failed: expected '$Expected', got '$Actual'"
    }
    $script:passed++
}

function Assert-Throws([scriptblock]$Action, [string]$ExpectedMessage, [string]$Case) {
    try {
        & $Action
    } catch {
        if ($_.Exception.Message -notlike "*$ExpectedMessage*") {
            throw "$Case failed with unexpected message: $($_.Exception.Message)"
        }
        $script:passed++
        return
    }
    throw "$Case failed: expected the action to throw"
}

$nested = [IO.Path]::GetFullPath((Join-Path $projectRoot 'artifacts/safe/result.json'))
Assert-Equal `
    (Resolve-ProjectChildPath -ProjectRoot $projectRoot -RequestedPath 'artifacts/safe/result.json' -Purpose 'Test output') `
    $nested 'nested relative path'
Assert-Equal `
    (Resolve-ProjectChildPath -ProjectRoot $projectRoot -RequestedPath $nested -Purpose 'Test output') `
    $nested 'absolute path inside project'

Assert-Throws {
    Resolve-ProjectChildPath -ProjectRoot $projectRoot -RequestedPath '../escape.json' -Purpose 'Test output'
} "must not contain '..'" 'parent traversal'
Assert-Throws {
    Resolve-ProjectChildPath -ProjectRoot $projectRoot -RequestedPath 'artifacts/../escape.json' -Purpose 'Test output'
} "must not contain '..'" 'normalized traversal'

$parent = [IO.Directory]::GetParent($projectRoot).FullName
$sibling = Join-Path $parent ((Split-Path $projectRoot -Leaf) + '-outside/escape.json')
Assert-Throws {
    Resolve-ProjectChildPath -ProjectRoot $projectRoot -RequestedPath $sibling -Purpose 'Test output'
} 'must stay inside' 'sibling prefix collision'
$otherDrive = if ([IO.Path]::GetPathRoot($projectRoot) -eq 'C:\') {
    'D:\outside\escape.json'
} else {
    'C:\outside\escape.json'
}
Assert-Throws {
    Resolve-ProjectChildPath -ProjectRoot $projectRoot -RequestedPath $otherDrive -Purpose 'Test output'
} 'must stay inside' 'absolute path on another drive'
Assert-Throws {
    Resolve-ProjectChildPath -ProjectRoot $projectRoot -RequestedPath $projectRoot -Purpose 'Test output'
} 'must stay inside' 'project root target'

Assert-Throws {
    & (Join-Path $scriptsRoot 'backup-sandbox.ps1') -OutputDirectory '../escape' -ProjectName 'unused'
} "must not contain '..'" 'backup script integration'
Assert-Throws {
    & (Join-Path $scriptsRoot 'export-openapi.ps1') -Output '../escape.json' -BaseUrl 'http://127.0.0.1:1'
} "must not contain '..'" 'OpenAPI script integration'

Write-Host "Path safety self-test passed: $script:passed assertions"
