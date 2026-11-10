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
$validateScript=Join-Path $scriptDir 'validate-changes.ps1'
$profileRoot=Join-Path ([IO.Path]::GetTempPath()) ('dorai-validation-profile-selftest-'+[guid]::NewGuid().ToString('N'))
$profileRepo=Join-Path $profileRoot 'repo'
try{
 New-Item -ItemType Directory -Path (Join-Path $profileRepo 'scripts') -Force | Out-Null
 New-Item -ItemType Directory -Path (Join-Path $profileRepo 'screenshotTests') -Force | Out-Null
 New-Item -ItemType Directory -Path (Join-Path $profileRepo 'frontend') -Force | Out-Null
 & git init -b main $profileRepo | Out-Null;if($LASTEXITCODE -ne 0){throw 'Unable to initialize validation-profile self-test repository.'}
 & git -C $profileRepo config user.name 'Dorai Tooling Self Test'
 & git -C $profileRepo config user.email 'tooling-selftest@localhost'
 Copy-Item -LiteralPath $validateScript -Destination (Join-Path $profileRepo 'scripts/validate-changes.ps1')
 & git -C $profileRepo add scripts/validate-changes.ps1
 & git -C $profileRepo commit -m 'test: initialize validation profile self-test' | Out-Null
 Push-Location $profileRepo
 try{
  $fullOutput=(& $validateScript -Profile Full -Base HEAD -WhatIf 2>&1 | Out-String)
  if($LASTEXITCODE -ne 0 -or $fullOutput -notmatch ':screenshotTests:verifyPaparazziDebug' -or $fullOutput -notmatch 'pnpm run frontend:check'){throw 'Explicit Full profile must run with no changed files and include screenshot plus frontend validation.'}
  Write-Output 'Explicit Full profile with clean tree: PASS'
  $wasmOutput=(& $validateScript -Profile Wasm -Base HEAD -WhatIf 2>&1 | Out-String)
  if($LASTEXITCODE -ne 0 -or $wasmOutput -notmatch 'Selected profile: Wasm' -or $wasmOutput -notmatch ':composeApp:compileKotlinWasmJs'){throw 'Explicit Wasm profile must run with no changed files.'}
  Write-Output 'Explicit Wasm profile with clean tree: PASS'
  $screenshotProbe=Join-Path $profileRepo 'screenshotTests/.agent-validation-selftest'
  Set-Content -LiteralPath $screenshotProbe -Value 'screenshot change'
  $screenshotOutput=(& $validateScript -Profile Auto -Base HEAD -WhatIf 2>&1 | Out-String)
  if($LASTEXITCODE -ne 0 -or $screenshotOutput -notmatch 'Selected profile: Feature' -or $screenshotOutput -notmatch ':screenshotTests:verifyPaparazziDebug'){throw 'Screenshot-only changes must select screenshot validation in Auto profile.'}
  Remove-Item -LiteralPath $screenshotProbe -Force
  Write-Output 'Screenshot-only Auto profile: PASS'
  $frontendProbe=Join-Path $profileRepo 'frontend/.agent-validation-selftest'
  Set-Content -LiteralPath $frontendProbe -Value 'frontend change'
  $frontendOutput=(& $validateScript -Profile Auto -Base HEAD -WhatIf 2>&1 | Out-String)
  if($LASTEXITCODE -ne 0 -or $frontendOutput -notmatch 'Selected profile: Fast' -or $frontendOutput -notmatch 'pnpm run frontend:check'){throw 'Frontend-only changes must select frontend validation in Auto profile.'}
  Remove-Item -LiteralPath $frontendProbe -Force
  Write-Output 'Frontend-only Auto profile: PASS'
 }finally{Pop-Location}
}finally{if(Test-Path $profileRoot){Remove-Item -LiteralPath $profileRoot -Recurse -Force}}
# Reproduce a squash merge: the feature commit is not an ancestor, but its final tree is present on main.
$tempRoot=Join-Path ([IO.Path]::GetTempPath()) ('dorai-agent-worktree-selftest-'+[guid]::NewGuid().ToString('N'))
$bare=Join-Path $tempRoot 'remote.git';$testRepo=Join-Path $tempRoot 'repo';$target=Join-Path $tempRoot 'feature-worktree'
try{
 New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
 & git init --bare $bare | Out-Null;if($LASTEXITCODE -ne 0){throw 'Unable to initialize self-test remote.'}
 & git init -b main $testRepo | Out-Null;if($LASTEXITCODE -ne 0){throw 'Unable to initialize self-test repository.'}
 & git -C $testRepo config user.name 'Dorai Tooling Self Test'
 & git -C $testRepo config user.email 'tooling-selftest@localhost'
 Set-Content -LiteralPath (Join-Path $testRepo 'README.md') -Value 'base'
 & git -C $testRepo add README.md
 & git -C $testRepo commit -m 'test: initialize self-test repository' | Out-Null
 & git -C $testRepo remote add origin $bare
 & git -C $testRepo push -u origin main | Out-Null;if($LASTEXITCODE -ne 0){throw 'Unable to publish self-test base.'}
 & git -C $testRepo checkout -b feature | Out-Null
 Set-Content -LiteralPath (Join-Path $testRepo 'feature.txt') -Value 'squash merged content'
 & git -C $testRepo add feature.txt
 & git -C $testRepo commit -m 'feat: add feature' | Out-Null
 & git -C $testRepo checkout main | Out-Null
 Set-Content -LiteralPath (Join-Path $testRepo 'feature.txt') -Value 'squash merged content'
 & git -C $testRepo add feature.txt
 & git -C $testRepo commit -m 'feat: squash merge feature' | Out-Null
 & git -C $testRepo push origin main | Out-Null;if($LASTEXITCODE -ne 0){throw 'Unable to publish self-test squash merge.'}
 & git -C $testRepo worktree add $target feature | Out-Null;if($LASTEXITCODE -ne 0){throw 'Unable to create self-test feature worktree.'}
 & git -C $target merge-base --is-ancestor HEAD origin/main 2>$null
 if($LASTEXITCODE -eq 0){throw 'Self-test setup failed: feature unexpectedly became an ancestor.'}
 Push-Location $testRepo
 try{& (Join-Path $scriptDir 'agent-worktree.ps1') -Action Remove -Path $target -Base main;if($LASTEXITCODE -ne 0){throw 'Squash-merged worktree cleanup self-test failed.'}}finally{Pop-Location}
 if(Test-Path $target){throw 'Squash-merged worktree was not removed.'}
}finally{
 if(Test-Path $target){& git -C $testRepo worktree remove $target 2>$null | Out-Null}
 if(Test-Path $tempRoot){Remove-Item -LiteralPath $tempRoot -Recurse -Force}
}
Write-Output 'Squash-merged worktree cleanup: PASS'
Write-Output 'Agent tooling self-test: PASS'
