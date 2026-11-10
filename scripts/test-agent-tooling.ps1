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
