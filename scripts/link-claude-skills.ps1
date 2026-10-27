#Requires -Version 5.1
<#
.SYNOPSIS
    Creates a directory junction at .claude/skills pointing to .agents/skills
    so both Antigravity and Claude Code share the exact same project skills.
    Run once per clone or git worktree.
#>

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$SourceDir = Join-Path $RepoRoot ".agents\skills"
$ClaudeDir = Join-Path $RepoRoot ".claude"
$TargetLink = Join-Path $ClaudeDir "skills"

if (-not (Test-Path $SourceDir)) {
    Write-Error "Source skills directory not found at: $SourceDir"
    exit 1
}

if (-not (Test-Path $ClaudeDir)) {
    New-Item -ItemType Directory -Path $ClaudeDir | Out-Null
}

if (Test-Path $TargetLink) {
    Write-Host "Link already exists at $TargetLink" -ForegroundColor Green
    exit 0
}

New-Item -ItemType Junction -Path $TargetLink -Target $SourceDir | Out-Null
Write-Host "Linked $TargetLink -> $SourceDir" -ForegroundColor Green
