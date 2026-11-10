[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$repo=(& git rev-parse --show-toplevel 2>$null).Trim();if($LASTEXITCODE -ne 0 -or !$repo){throw 'Run from repository root.'}
$scriptDir=Join-Path $repo 'scripts'
foreach($file in Get-ChildItem $scriptDir -Filter '*.ps1' -File){$tokens=$null;$errors=$null;[System.Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$errors)|Out-Null;if($errors.Count){$errors|ForEach-Object{Write-Output $_};throw "PowerShell syntax error: $($file.Name)"}}
Write-Output 'PowerShell syntax: PASS'
$claudeSettings=Get-Content (Join-Path $repo '.claude/settings.json') -Raw | ConvertFrom-Json
if(!$claudeSettings.permissions.deny -or @($claudeSettings.permissions.deny).Count -eq 0){throw 'Shared Claude settings must retain destructive-command deny rules.'}
if(Test-Path (Join-Path $repo '.claude/settings.local.json')){throw 'Private Claude settings.local.json must not be tracked in the shared project tree.'}
$requiredAdapters=@('.github/copilot-instructions.md','.cursor/rules/dorai-project.mdc','.windsurf/rules/dorai-project.md','.agents/rules/dorai-project.md','.kilocode/rules/00-dorai-project.md','.opencode/agents/security-reviewer.md','.claude/settings.json')
foreach($adapter in $requiredAdapters){if(!(Test-Path (Join-Path $repo $adapter))){throw "Missing native agent adapter: $adapter"}}
Write-Output 'Native adapter and shared safety configuration: PASS'
$bash=Get-Command bash -ErrorAction SilentlyContinue;if($bash){& bash -n (Join-Path $scriptDir 'agent-worktree.sh');if($LASTEXITCODE -ne 0){throw 'Bash syntax check failed.'};Write-Output 'Bash syntax: PASS'}
& (Join-Path $scriptDir 'sync-agent-configs.ps1') -Check;if($LASTEXITCODE -ne 0){throw 'Agent config synchronization check failed.'}
$task='DORAI-TOOLING-SELFTEST';$agent='ci-selftest';$path='scripts/**'
& (Join-Path $scriptDir 'agent-lock.ps1') -Action Acquire -TaskId $task -Agent $agent -Paths $path
$conflict=$false;try{& (Join-Path $scriptDir 'agent-lock.ps1') -Action Acquire -TaskId ($task+'-conflict') -Agent 'conflict-test' -Paths 'scripts/agent-lock.ps1'}catch{$conflict=$true;Write-Output 'Lock overlap rejection: PASS'}
if(!$conflict){throw 'Overlapping path reservation was not rejected.'}
& (Join-Path $scriptDir 'agent-lock.ps1') -Action Release -TaskId $task -Agent $agent
$metadataPath=Join-Path $repo '.agent-task.json';$hadMetadata=Test-Path $metadataPath;$backup=$null;if($hadMetadata){$backup=Get-Content $metadataPath -Raw}
try{$metadata=@{task_id='DORAI-SCOPE-SELFTEST';base='HEAD';allowed_paths=@('**');baseline_files=@{}}|ConvertTo-Json -Depth 5;Set-Content -LiteralPath $metadataPath -Value $metadata -Encoding utf8;& (Join-Path $scriptDir 'verify-agent-scope.ps1');if($LASTEXITCODE -ne 0){throw 'Scope verification self-test failed.'}}finally{if($hadMetadata){Set-Content -LiteralPath $metadataPath -Value $backup -Encoding utf8}else{Remove-Item -LiteralPath $metadataPath -Force -ErrorAction SilentlyContinue}}
Write-Output 'Agent tooling self-test: PASS'

# Regression coverage for explicit validation profiles, path classification, and squash-merged worktrees.
$validationScript = Join-Path $scriptDir 'validate-changes.ps1'
$worktreeScript = Join-Path $scriptDir 'agent-worktree.ps1'
$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ("dorai-validation-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
try {
  & git -C $fixtureRoot init --quiet --initial-branch=main
  if ($LASTEXITCODE -ne 0) { throw 'Unable to initialize validation fixture repository.' }
  & git -C $fixtureRoot config user.name 'Dorai CI'
  & git -C $fixtureRoot config user.email 'dorai-ci@example.invalid'
  Set-Content -LiteralPath (Join-Path $fixtureRoot 'baseline.txt') -Value 'baseline' -Encoding utf8
  & git -C $fixtureRoot add baseline.txt
  & git -C $fixtureRoot commit --quiet -m 'fixture baseline'
  if ($LASTEXITCODE -ne 0) { throw 'Unable to commit validation fixture baseline.' }

  Push-Location $fixtureRoot
  try {
    $fullOutput = (& $validationScript -Profile Full -Base HEAD -WhatIf 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $fullOutput -notmatch 'Selected profile: Full' -or $fullOutput -notmatch ':androidApp:testFossDebugUnitTest') {
      throw 'Explicit Full profile did not run its dry-run plan on a clean tree.'
    }
    $wasmOutput = (& $validationScript -Profile Wasm -Base HEAD -WhatIf 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $wasmOutput -notmatch 'Selected profile: Wasm' -or $wasmOutput -notmatch ':composeApp:compileKotlinWasmJs') {
      throw 'Explicit Wasm profile did not run its dry-run plan on a clean tree.'
    }
    $autoOutput = (& $validationScript -Profile Auto -Base HEAD -WhatIf 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $autoOutput -notmatch 'Auto profile has nothing to validate') {
      throw 'Auto profile should remain a no-op on a clean tree.'
    }

    New-Item -ItemType Directory -Path (Join-Path $fixtureRoot 'screenshotTests') | Out-Null
    Set-Content -LiteralPath (Join-Path $fixtureRoot 'screenshotTests\fixture.png') -Value 'fixture' -Encoding utf8
    $screenshotOutput = (& $validationScript -Profile Auto -Base HEAD -WhatIf 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $screenshotOutput -notmatch 'Selected profile: Feature' -or $screenshotOutput -notmatch ':screenshotTests:verifyPaparazziDebug') {
      throw 'Screenshot-only changes must select Feature and Paparazzi verification.'
    }

    New-Item -ItemType Directory -Path (Join-Path $fixtureRoot 'frontend\src') -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $fixtureRoot 'frontend\src\fixture.ts') -Value 'export {}' -Encoding utf8
    $frontendOutput = (& $validationScript -Profile Auto -Base HEAD -WhatIf 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $frontendOutput -notmatch 'Selected profile: Frontend' -or $frontendOutput -notmatch 'pnpm frontend:check' -or $frontendOutput -match 'Gradle tasks:') {
      throw 'Frontend-only changes must select the Frontend validation profile.'
    }

    New-Item -ItemType Directory -Path (Join-Path $fixtureRoot '.github\workflows') -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $fixtureRoot '.github\workflows\fixture.yml') -Value 'name: fixture' -Encoding utf8
    $workflowOutput = (& $validationScript -Profile Auto -Base HEAD -WhatIf 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $workflowOutput -notmatch 'Selected profile: Full' -or $workflowOutput -notmatch 'pnpm frontend:check') {
      throw 'Workflow changes must retain Full validation and include frontend checks.'
    }
  }
  finally { Pop-Location }
  Write-Output 'Validation profile regression tests: PASS'

  $squashRepo = Join-Path $fixtureRoot 'squash-repo'
  $squashWorktree = Join-Path $fixtureRoot 'squash-worktree'
  New-Item -ItemType Directory -Path $squashRepo | Out-Null
  & git -C $squashRepo init --quiet --initial-branch=main
  if ($LASTEXITCODE -ne 0) { throw 'Unable to initialize squash-merge fixture repository.' }
  & git -C $squashRepo config user.name 'Dorai CI'
  & git -C $squashRepo config user.email 'dorai-ci@example.invalid'
  Set-Content -LiteralPath (Join-Path $squashRepo 'base.txt') -Value 'base' -Encoding utf8
  & git -C $squashRepo add base.txt
  & git -C $squashRepo commit --quiet -m 'base'
  & git -C $squashRepo remote add origin $squashRepo
  & git -C $squashRepo fetch --quiet origin main
  if ($LASTEXITCODE -ne 0) { throw 'Unable to configure squash-merge fixture remote.' }
  & git -C $squashRepo worktree add --quiet -b squash-fixture $squashWorktree origin/main
  if ($LASTEXITCODE -ne 0) { throw 'Unable to create squash-merge fixture worktree.' }
  Set-Content -LiteralPath (Join-Path $squashWorktree 'feature.txt') -Value 'feature' -Encoding utf8
  & git -C $squashWorktree add feature.txt
  & git -C $squashWorktree commit --quiet -m 'feature change'
  if ($LASTEXITCODE -ne 0) { throw 'Unable to commit squash-merge fixture change.' }
  $featureTree = (& git -C $squashWorktree rev-parse 'HEAD^{tree}').Trim()
  $mainParent = (& git -C $squashRepo rev-parse main).Trim()
  $squashCommit = (& git -C $squashRepo commit-tree $featureTree -p $mainParent -m 'squash merge fixture').Trim()
  if ($LASTEXITCODE -ne 0 -or !$squashCommit) { throw 'Unable to create squash-merge fixture commit.' }
  & git -C $squashRepo update-ref refs/heads/main $squashCommit
  if ($LASTEXITCODE -ne 0) { throw 'Unable to advance squash-merge fixture main.' }
  Push-Location $squashRepo
  try { & $worktreeScript -Action Remove -Path $squashWorktree }
  finally { Pop-Location }
  if (Test-Path $squashWorktree) { throw 'Clean squash-merged worktree was not removed.' }
  Write-Output 'PowerShell squash-merged worktree cleanup regression test: PASS'

  if ($bash) {
    $bashWorktreeScript = Join-Path $scriptDir 'agent-worktree.sh'
    $bashWorktree = Join-Path $fixtureRoot 'bash-squash-worktree'
    & git -C $squashRepo worktree add --quiet -b bash-squash-fixture $bashWorktree origin/main
    if ($LASTEXITCODE -ne 0) { throw 'Unable to create Bash squash-merge fixture worktree.' }
    Set-Content -LiteralPath (Join-Path $bashWorktree 'bash-feature.txt') -Value 'bash feature' -Encoding utf8
    & git -C $bashWorktree add bash-feature.txt
    & git -C $bashWorktree commit --quiet -m 'Bash feature change'
    if ($LASTEXITCODE -ne 0) { throw 'Unable to commit Bash squash-merge fixture change.' }
    $bashFeatureTree = (& git -C $bashWorktree rev-parse 'HEAD^{tree}').Trim()
    $bashMainParent = (& git -C $squashRepo rev-parse main).Trim()
    $bashSquashCommit = (& git -C $squashRepo commit-tree $bashFeatureTree -p $bashMainParent -m 'Bash squash merge fixture').Trim()
    if ($LASTEXITCODE -ne 0 -or !$bashSquashCommit) { throw 'Unable to create Bash squash-merge fixture commit.' }
    & git -C $squashRepo update-ref refs/heads/main $bashSquashCommit
    if ($LASTEXITCODE -ne 0) { throw 'Unable to advance Bash squash-merge fixture main.' }
    Push-Location $squashRepo
    try { & bash $bashWorktreeScript remove $bashWorktree main }
    finally { Pop-Location }
    if ($LASTEXITCODE -ne 0 -or (Test-Path $bashWorktree)) { throw 'Bash helper failed to remove the clean squash-merged worktree.' }
    Write-Output 'Bash squash-merged worktree cleanup regression test: PASS'
  }
}
finally {
  if (Test-Path $fixtureRoot) { Remove-Item -LiteralPath $fixtureRoot -Recurse -Force }
}
