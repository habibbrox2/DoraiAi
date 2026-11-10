[CmdletBinding()]
param([ValidateSet('Acquire','Release','List')][string]$Action='List',[string]$TaskId,[string]$Agent,[string[]]$Paths=@())
$ErrorActionPreference='Stop'
$repo=(& git rev-parse --show-toplevel 2>$null).Trim();if($LASTEXITCODE -ne 0 -or !$repo){throw 'Run inside a Git worktree.'}
$common=(& git -C $repo rev-parse --path-format=absolute --git-common-dir).Trim();if($LASTEXITCODE -ne 0 -or !$common){throw 'Unable to locate shared Git directory.'}
$lockFile=Join-Path $common 'dorai-agent-locks.json'
function Normalize-Path([string]$Value){$slash=[string][char]92;$v=$Value.Replace($slash,'/').Trim().TrimStart('/').ToLowerInvariant();$v=[regex]::Replace($v,'[*?].*$','').TrimEnd('/');return $v}
function Test-Overlap([string]$A,[string]$B){return ($A -eq $B) -or $A.StartsWith($B+'/') -or $B.StartsWith($A+'/')}
$stream=$null;for($i=0;$i -lt 30;$i++){try{$stream=[IO.File]::Open($lockFile,[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None);break}catch{Start-Sleep -Milliseconds 100}}
if(!$stream){throw 'Could not acquire lock registry mutex; retry shortly.'}
try{
 $reader=New-Object IO.StreamReader($stream,[Text.Encoding]::UTF8,$true,1024,$true);$raw=$reader.ReadToEnd();$reader.Dispose();$locks=@();if($raw.Trim()){$locks=@(ConvertFrom-Json $raw)}
 if($Action -eq 'List'){$locks|Format-Table task_id,agent,paths,created_at -AutoSize;return}
 if(!$TaskId -or !$Agent){throw "$Action requires -TaskId and -Agent."}
 if($Action -eq 'Acquire'){
  if(!$Paths -or $Paths.Count -eq 0){throw 'Acquire requires one or more -Paths.'}
  if(@($locks|Where-Object{$_.task_id -eq $TaskId}).Count -gt 0){throw "Task already owns a lock: $TaskId"}
  $requested=@($Paths|ForEach-Object{Normalize-Path $_}|Where-Object{$_}|Select-Object -Unique)
  foreach($existing in $locks){foreach($want in $requested){foreach($held in @($existing.paths)){if(Test-Overlap $want (Normalize-Path ([string]$held))){throw "Path reservation conflict: '$want' overlaps '$held' held by $($existing.agent) / $($existing.task_id)."}}}}
  $locks+= [pscustomobject]@{task_id=$TaskId;agent=$Agent;paths=$requested;created_at=[DateTime]::UtcNow.ToString('o');machine=$env:COMPUTERNAME};Write-Output "Reserved $($requested -join ', ') for $Agent / $TaskId"
 }elseif($Action -eq 'Release'){
  $owned=@($locks|Where-Object{$_.task_id -eq $TaskId -and $_.agent -eq $Agent});if($owned.Count -eq 0){throw "No reservation owned by $Agent / $TaskId."};$locks=@($locks|Where-Object{!($_.task_id -eq $TaskId -and $_.agent -eq $Agent)});Write-Output "Released reservation for $Agent / $TaskId"
 }
 $json=ConvertTo-Json -InputObject @($locks) -Depth 8;$bytes=[Text.Encoding]::UTF8.GetBytes($json);$stream.SetLength(0);$stream.Position=0;$stream.Write($bytes,0,$bytes.Length);$stream.Flush()
}finally{if($stream){$stream.Dispose()}}
