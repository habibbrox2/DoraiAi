[CmdletBinding(SupportsShouldProcess=$true)]
param([switch]$Check)
$ErrorActionPreference='Stop'
$repo=(& git rev-parse --show-toplevel 2>$null).Trim(); if($LASTEXITCODE -ne 0 -or !$repo){throw 'Run inside a Git worktree.'}
$problems=@()
function Sync-File([string]$Source,[string]$Destination,[string]$Label){
  $different=(!(Test-Path $Destination))
  if(!$different){$different=(Get-FileHash $Destination -Algorithm SHA256).Hash -ne (Get-FileHash $Source -Algorithm SHA256).Hash}
  if($different){if($Check){$script:problems+="$Label is missing/stale: $Destination"}elseif($PSCmdlet.ShouldProcess($Destination,"Synchronize $Label")){New-Item -ItemType Directory -Force (Split-Path $Destination -Parent)|Out-Null;Copy-Item $Source $Destination -Force}}
}
$claudeAgents=Join-Path $repo '.claude\agents';New-Item -ItemType Directory -Force $claudeAgents|Out-Null
foreach($file in Get-ChildItem (Join-Path $repo 'agents') -Filter '*.md' -File){
  $content=Get-Content $file.FullName -Raw
  if($content -match '(?ms)^---\s*\r?\n(?:(?!\r?\n---).)*?^name:\s*[^\r\n]+(?:(?!\r?\n---).)*?^description:\s*[^\r\n]+'){
    Sync-File $file.FullName (Join-Path $claudeAgents $file.Name) 'Claude agent copy'
  }
}
$claudeSkills=Join-Path $repo '.claude\skills';$sharedSkills=Join-Path $repo '.agents\skills'
New-Item -ItemType Directory -Force $claudeSkills,$sharedSkills|Out-Null
foreach($skill in Get-ChildItem (Join-Path $repo 'skills') -Directory){
  $source=Join-Path $skill.FullName 'SKILL.md';if(!(Test-Path $source)){continue}
  Sync-File $source (Join-Path (Join-Path $claudeSkills $skill.Name) 'SKILL.md') 'Claude skill copy'
  Sync-File $source (Join-Path (Join-Path $sharedSkills $skill.Name) 'SKILL.md') 'Shared agent skill copy'
}
if($Check -and $problems.Count){$problems|ForEach-Object{Write-Output $_};exit 1}
if($Check){Write-Output 'Claude and shared agent skill copies are synchronized.'}else{Write-Output 'Synchronized Claude agent/skill copies and shared .agents/skills from canonical sources.'}
