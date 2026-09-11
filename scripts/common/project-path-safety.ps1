#requires -Version 7.0

function Resolve-ProjectChildPath {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot,

        [Parameter(Mandatory = $true)]
        [string]$RequestedPath,

        [Parameter(Mandatory = $true)]
        [string]$Purpose
    )

    if ([string]::IsNullOrWhiteSpace($RequestedPath)) {
        throw "$Purpose path must not be empty"
    }
    if (($RequestedPath -split '[\\/]') -contains '..') {
        throw "$Purpose path must not contain '..' traversal segments"
    }

    $canonicalRoot = [IO.Path]::GetFullPath($ProjectRoot)
    $candidate = if ([IO.Path]::IsPathFullyQualified($RequestedPath)) {
        [IO.Path]::GetFullPath($RequestedPath)
    } else {
        [IO.Path]::GetFullPath([IO.Path]::Combine($canonicalRoot, $RequestedPath))
    }
    $relative = [IO.Path]::GetRelativePath($canonicalRoot, $candidate)
    $parentPrefix = "..$([IO.Path]::DirectorySeparatorChar)"
    $alternateParentPrefix = "..$([IO.Path]::AltDirectorySeparatorChar)"
    $escapesProject = [IO.Path]::IsPathFullyQualified($relative) -or
        $relative -eq '..' -or
        $relative.StartsWith($parentPrefix, [StringComparison]::Ordinal) -or
        $relative.StartsWith($alternateParentPrefix, [StringComparison]::Ordinal)
    if ($escapesProject -or $relative -eq '.') {
        throw "$Purpose path must stay inside the project directory"
    }

    # Do not follow an existing junction or symbolic link to a destination outside
    # the lexical project boundary. New descendants are checked up to the first
    # path component that does not exist yet.
    $cursor = $canonicalRoot
    foreach ($segment in ($relative -split '[\\/]')) {
        $cursor = [IO.Path]::Combine($cursor, $segment)
        if (-not [IO.File]::Exists($cursor) -and -not [IO.Directory]::Exists($cursor)) {
            break
        }
        $item = Get-Item -LiteralPath $cursor -Force
        if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "$Purpose path must not traverse a symbolic link or junction"
        }
    }

    return $candidate
}
