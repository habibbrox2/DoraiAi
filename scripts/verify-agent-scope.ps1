[CmdletBinding()]
param([string[]]$AllowedPaths=@(),[string]$ContractPath='.agent-task.json')
$ErrorActionPreference='Stop'
$repo=(& git rev-parse --show-toplevel 2>$null).Trim();if($LASTEXITCODE -ne 0 -or !$repo){throw 'Run inside a Git worktree.'}
$contractFile=Join-Path $repo $ContractPath;$contract=$null;if(Test-Path $contractFile){$contract=Get-Content $contractFile -Raw|ConvertFrom-Json}
if((!$AllowedPaths -or $AllowedPaths.Count -eq 0) -and $contract){$AllowedPaths=@($contract.allowed_paths)}
if(!$AllowedPaths -or $AllowedPaths.Count -eq 0){throw 'No allowed paths provided. Create a worktree with -AllowedPaths or pass -AllowedPaths explicitly.'}
$changed=@();if($contract -and $contract.base){$baseRef=[string]$contract.base;$changed+=@(& git -C $repo diff --name-only ($baseRef+'...HEAD'));if($LASTEXITCODE -ne 0){throw "Unable to inspect committed changes against $baseRef."}}
$changed+=@(& git -C $repo diff --name-only HEAD);if($LASTEXITCODE -ne 0){throw 'Unable to inspect working diff.'};$changed+=@(& git -C $repo ls-files --others --exclude-standard);if($LASTEXITCODE -ne 0){throw 'Unable to inspect untracked files.'};$changed=@($changed|Where-Object{$_}|Select-Object -Unique)
function Match-Allowed([string]$Path,[string]$Pattern){$slash=[string][char]92;$bs=[string][char]92;$escaped=[regex]::Escape($Pattern.Replace($slash,'/'));$escaped=$escaped.Replace(($bs+'*'+$bs+'*'),'.*').Replace(($bs+'*'),'[^/]*').Replace(($bs+'?'),'[^/]');$rx='^'+$escaped+'$';return $Path.Replace($slash,'/') -match $rx}
$outside=@();foreach($file in $changed){
 $baselineValue=$null
 if($contract -and $contract.baseline_files){$property=$contract.baseline_files.PSObject.Properties[$file];if($property){$baselineValue=[string]$property.Value}}
 if($baselineValue -eq '__DELETED__' -and !(Test-Path (Join-Path $repo $file))){continue}
 $absolute=Join-Path $repo $file
 if($baselineValue -and $baselineValue -ne '__DELETED__' -and (Test-Path -LiteralPath $absolute -PathType Leaf)){$currentHash=(Get-FileHash -LiteralPath $absolute -Algorithm SHA256).Hash;if($currentHash -eq $baselineValue){continue}}
 $ok=$false;foreach($pattern in $AllowedPaths){if(Match-Allowed $file ([string]$pattern)){$ok=$true;break}};if(!$ok){$outside+=$file}
}
if($outside.Count){Write-Output 'SCOPE VIOLATION — changed paths outside allowed_paths:';$outside|ForEach-Object{Write-Output " - $_"};exit 2};Write-Output "Scope check passed. Changed files: $($changed.Count)"
