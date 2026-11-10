[CmdletBinding()]
param(
  [ValidateSet('Create','List','Status','Remove')][string]$Action = 'List',
  [string]$Agent,
  [string]$Task,
  [string]$Branch,
  [string]$Path,
  [string]$Base = 'main',
  [string[]]$AllowedPaths = @(),
  [string[]]$ReservedPaths = @(),
  [ValidateSet('Auto','Docs','Fast','Feature','Full')][string]$ValidationProfile = 'Auto'
)
$ErrorActionPreference = 'Stop'
$repo = (& git rev-parse --show-toplevel 2>$null).Trim()
if ($LASTEXITCODE -ne 0 -or !$repo) { throw 'Run this script from inside a Git worktree.' }
function Invoke-Git([string[]]$GitArgs) { & git -C $repo @GitArgs; if ($LASTEXITCODE -ne 0) { throw "git $($GitArgs -join ' ') failed with exit code $LASTEXITCODE" } }
switch ($Action) {
  'List' { Invoke-Git @('worktree','list','--porcelain'); break }
  'Create' {
    if (!$Agent -or !$Task) { throw 'Create requires -Agent and -Task.' }
    if ($Agent -notmatch '^[a-zA-Z0-9._-]+$' -or $Task -notmatch '^[a-zA-Z0-9._-]+$') { throw 'Agent and Task may contain only letters, numbers, dot, underscore, and hyphen.' }
    if (!$Branch) { $Branch = "agent/$($Agent.ToLowerInvariant())/$($Task.ToLowerInvariant())" }
    if (!$Path) { $root = Join-Path (Split-Path $repo -Parent) 'worktrees'; $Path = Join-Path $root "$($Agent.ToLowerInvariant())-$($Task.ToLowerInvariant())" }
    $Path = [IO.Path]::GetFullPath($Path)
    if (Test-Path -LiteralPath $Path) { throw "Refusing to overwrite existing worktree path: $Path" }
    Invoke-Git @('fetch','origin',$Base)
    $baseRef = "origin/$Base"
    $existing = ((& git -C $repo branch --list $Branch | Out-String).Trim())
    if ($existing) { throw "Branch already exists: $Branch. Choose a new task branch; existing work is never reused implicitly." }
    New-Item -ItemType Directory -Force -Path (Split-Path $Path -Parent) | Out-Null
    Invoke-Git @('worktree','add','-b',$Branch,$Path,$baseRef)
    # Capture pre-existing local edits in the new worktree. Scope verification ignores them only while their content remains byte-identical.
    $baselineFiles = @{}
    $preexisting = @(& git -C $Path diff --name-only HEAD)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to inspect pre-existing worktree changes.' }
    $preexisting += @(& git -C $Path ls-files --others --exclude-standard)
    foreach ($relative in ($preexisting | Where-Object { $_ } | Select-Object -Unique)) {
      $absolute = Join-Path $Path $relative
      if (Test-Path -LiteralPath $absolute -PathType Leaf) { $baselineFiles[$relative] = (Get-FileHash -LiteralPath $absolute -Algorithm SHA256).Hash }
      elseif (!(Test-Path -LiteralPath $absolute)) { $baselineFiles[$relative] = '__DELETED__' }
    }
    $excludePath = (& git -C $Path rev-parse --path-format=absolute --git-path info/exclude).Trim()
    if ($LASTEXITCODE -eq 0 -and $excludePath) {
      if (!(Test-Path $excludePath) -or !(Select-String -Path $excludePath -SimpleMatch '.agent-task.json' -Quiet -ErrorAction SilentlyContinue)) { Add-Content -LiteralPath $excludePath -Value "`n.agent-task.json" }
    }
    $metadata = [ordered]@{ task_id=$Task; agent=$Agent; branch=$Branch; base=$baseRef; allowed_paths=@($AllowedPaths); reserved_paths=@($ReservedPaths); validation_profile=$ValidationProfile; baseline_files=$baselineFiles; created_at=[DateTime]::UtcNow.ToString('o') }
    $metadata | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $Path '.agent-task.json') -Encoding utf8
    Write-Output "Created isolated worktree: $Path"
    Write-Output "Branch: $Branch (base: $baseRef)"
    Write-Output 'Local task metadata: .agent-task.json (ignored by Git)'
    break
  }
  'Status' {
    if (!$Path) { throw 'Status requires -Path.' }
    $target = [IO.Path]::GetFullPath($Path)
    if (!(Test-Path (Join-Path $target '.git'))) { throw "Not a Git worktree: $target" }
    & git -C $target status --short --branch
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read worktree status.' }
    break
  }
  'Remove' {
    if (!$Path) { throw 'Remove requires -Path.' }
    $target = [IO.Path]::GetFullPath($Path)
    if (!(Test-Path (Join-Path $target '.git'))) { throw "Not a Git worktree: $target" }
    $status = @(& git -C $target status --porcelain)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to inspect worktree status.' }
    if ($status.Count -gt 0) { throw 'Refusing to remove a dirty worktree. Commit or preserve its changes first.' }
    $branch = (& git -C $target branch --show-current).Trim()
    if (!$branch) { throw 'Refusing to remove a detached worktree automatically.' }
    Invoke-Git @('fetch','origin',$Base)
    & git -C $target merge-base --is-ancestor HEAD "origin/$Base"
    $isMerged = $LASTEXITCODE -eq 0
    if (!$isMerged) {
      # Squash merges do not preserve branch ancestry. Accept only an exact tree
      # match with a commit already reachable from the selected base; this remains
      # conservative when the PR required conflict-resolution edits or has unique work.
      $targetTree = (& git -C $target rev-parse 'HEAD^{tree}').Trim()
      if ($LASTEXITCODE -ne 0 -or !$targetTree) { throw 'Unable to inspect the worktree tree.' }
      $baseTrees = @(& git -C $repo log -r --format=%T "origin/$Base")
      if ($LASTEXITCODE -ne 0) { throw "Unable to inspect origin/$Base history." }
      $isMerged = $baseTrees -contains $targetTree
    }

    # Squash merges can require conflict-resolution edits, so the final tree may
    # differ from the feature branch. When available, verify a merged PR targeting
    # this base through the authenticated GitHub CLI before refusing cleanup.
    $mergedPullRequest = $false
    if (!$isMerged) {
      $gh = Get-Command gh -ErrorAction SilentlyContinue
      if ($gh) {
        $ghOutput = @(& $gh.Source pr list --head $branch --state merged --json baseRefName --limit 10 2>$null)
        $ghExitCode = $LASTEXITCODE
        if ($ghExitCode -eq 0 -and $ghOutput.Count -gt 0) {
          try {
            $mergedPullRequests = @(($ghOutput -join "`n") | ConvertFrom-Json -ErrorAction Stop)
            $mergedPullRequest = @($mergedPullRequests | Where-Object { $_.baseRefName -eq $Base }).Count -gt 0
          }
          catch {
            # Invalid or unavailable CLI output is not merge evidence.
            $mergedPullRequest = $false
          }
        }
      }
    }

    if (!$isMerged -and !$mergedPullRequest) {
      throw "Refusing to remove unmerged worktree branch '$branch': no merged ancestry, identical tree, or merged PR targeting '$Base' could be verified. Preserve it or merge it first."
    }
    Invoke-Git @('worktree','remove',$target)
    Write-Output "Removed clean, merged worktree: $target"
    break
  }
}
