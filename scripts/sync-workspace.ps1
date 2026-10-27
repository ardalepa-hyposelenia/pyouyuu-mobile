#Requires -Version 5.1
<#
.SYNOPSIS
    Manages the 'workspace' Git submodule and root directory junctions (docs/, contracts/, tasks/)
    inside any standalone Pyouyuu lane repository (android, api, web).

.EXAMPLE
    .\scripts\sync-workspace.ps1 link
    Initializes submodule (if needed) and creates root junctions: docs/, contracts/, tasks/, .claude/skills

.EXAMPLE
    .\scripts\sync-workspace.ps1 status
    Shows uncommitted changes in workspace/ (e.g., BOARD.md, PROGRESS.md, CHANGE_REQUESTS.md)

.EXAMPLE
    .\scripts\sync-workspace.ps1 pull
    Pulls the latest 'main' branch inside workspace/

.EXAMPLE
    .\scripts\sync-workspace.ps1 push "docs(T-B02): update BOARD and PROGRESS"
    Commits and pushes changes inside workspace/ back to the workspace repository's main branch
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet("link", "status", "pull", "push")]
    [string]$Action = "link",

    [Parameter(Position = 1)]
    [string]$CommitMessage = "chore(workspace): sync task board and progress log"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$WorkspaceDir = Join-Path $RepoRoot "workspace"

function Ensure-Junction {
    param([string]$LinkPath, [string]$TargetPath)
    if (-not (Test-Path $TargetPath)) {
        return
    }
    if (Test-Path $LinkPath) {
        $item = Get-Item $LinkPath -Force
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            return
        }
        Write-Warning "$LinkPath exists and is not a junction; skipping."
        return
    }
    New-Item -ItemType Junction -Path $LinkPath -Target $TargetPath | Out-Null
    Write-Host "Linked $LinkPath -> $TargetPath" -ForegroundColor Green
}

switch ($Action) {
    "link" {
        if (-not (Test-Path (Join-Path $WorkspaceDir "docs"))) {
            Write-Host "Initializing workspace submodule..." -ForegroundColor Cyan
            git -C $RepoRoot submodule update --init --recursive
        }

        Ensure-Junction -LinkPath (Join-Path $RepoRoot "docs")      -TargetPath (Join-Path $WorkspaceDir "docs")
        Ensure-Junction -LinkPath (Join-Path $RepoRoot "contracts") -TargetPath (Join-Path $WorkspaceDir "contracts")
        Ensure-Junction -LinkPath (Join-Path $RepoRoot "tasks")     -TargetPath (Join-Path $WorkspaceDir "tasks")

        $SkillsSource = Join-Path $RepoRoot ".agents\skills"
        if (-not (Test-Path $SkillsSource)) {
            $SkillsSource = Join-Path $WorkspaceDir ".agents\skills"
            if (Test-Path $SkillsSource) {
                $AgentsDir = Join-Path $RepoRoot ".agents"
                if (-not (Test-Path $AgentsDir)) {
                    New-Item -ItemType Directory -Path $AgentsDir | Out-Null
                }
                Ensure-Junction -LinkPath (Join-Path $AgentsDir "skills") -TargetPath $SkillsSource
            }
        }

        if (Test-Path (Join-Path $RepoRoot ".agents\skills")) {
            $ClaudeDir = Join-Path $RepoRoot ".claude"
            if (-not (Test-Path $ClaudeDir)) {
                New-Item -ItemType Directory -Path $ClaudeDir | Out-Null
            }
            Ensure-Junction -LinkPath (Join-Path $ClaudeDir "skills") -TargetPath (Join-Path $RepoRoot ".agents\skills")
        }

        Write-Host "Workspace junctions are ready." -ForegroundColor Green
    }

    "status" {
        Write-Host "=== Workspace Submodule Status ===" -ForegroundColor Cyan
        git -C $WorkspaceDir status -s -b
    }

    "pull" {
        Write-Host "Pulling latest 'main' in workspace submodule..." -ForegroundColor Cyan
        git -C $WorkspaceDir checkout main
        git -C $WorkspaceDir pull origin main
        Write-Host "Workspace updated to latest main." -ForegroundColor Green
    }

    "push" {
        Write-Host "Committing and pushing workspace changes to main..." -ForegroundColor Cyan
        git -C $WorkspaceDir checkout main
        git -C $WorkspaceDir add -A
        $changes = git -C $WorkspaceDir status --porcelain
        if (-not $changes) {
            Write-Host "No changes in workspace/ to commit." -ForegroundColor Yellow
            exit 0
        }
        git -C $WorkspaceDir commit -m $CommitMessage
        git -C $WorkspaceDir push origin main
        Write-Host "Workspace changes pushed to origin/main." -ForegroundColor Green
    }
}
