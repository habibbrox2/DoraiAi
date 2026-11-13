# Validate locale resources: well-formed XML, duplicate keys, en<->bn parity,
# placeholder parity across all locales, mojibake / U+FFFD scan.
# Enforced for en<->bn; other locales are reported (staleness) but never fail.
# See docs/development/bengali-language-and-rendering-policy.md sections 4 and 11.

[CmdletBinding()]
param(
  [string]$RepoRoot
)

$ErrorActionPreference = 'Stop'

if (!$RepoRoot) { $RepoRoot = (& git rev-parse --show-toplevel 2>$null).Trim() }
if (!$RepoRoot -or !(Test-Path $RepoRoot)) { throw 'Run inside a Git worktree or pass -RepoRoot.' }

$resDir = Join-Path $RepoRoot 'composeApp\src\commonMain\composeResources'
if (!(Test-Path $resDir)) { throw "composeResources not found at $resDir" }

$failures = New-Object System.Collections.Generic.List[string]
$placeholderRegex = [regex]'%(\d+\$)?[a-z]'
$mojibakeRegexes = @(
  ([regex]::Escape([char]0x00E0 + [char]0x00A6)),   # à¦ — Bengali double-encoding lead
  'Ã[\u0080-\u00BF]',                               # Ã + continuation byte — Latin double-encoding
  ([char]0x00EF + [char]0x00BF + [char]0x00BD),      # ï¿½ — U+FFFD misencoded
  ([char]0xFFFD)                                     # literal replacement char
)

$locales = Get-ChildItem $resDir -Directory | Where-Object { $_.Name -like 'values*' }
$data = @{}

foreach ($dir in $locales) {
  $file = Join-Path $dir.FullName 'strings.xml'
  if (!(Test-Path $file)) { $failures.Add("$($dir.Name): missing strings.xml") | Out-Null; continue }

  $raw = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

  foreach ($rx in $mojibakeRegexes) {
    if ([regex]::IsMatch($raw, $rx)) { $failures.Add("$($dir.Name): contains double-encoded UTF-8 or U+FFFD signature") | Out-Null; break }
  }

  try {
    $doc = New-Object System.Xml.XmlDocument
    $doc.Load($file)   # throws on malformed XML
  } catch {
    $failures.Add("$($dir.Name): malformed XML: $($_.Exception.Message)") | Out-Null
    continue
  }

  $stringKeys = @()
  foreach ($el in $doc.SelectNodes('//string')) { $stringKeys += $el.GetAttribute('name') }

  $dup = $stringKeys | Group-Object | Where-Object { $_.Count -gt 1 }
  foreach ($d in $dup) { $failures.Add("$($dir.Name): duplicate <string name=`"$($d.Name)`">") | Out-Null }

  $plurals = @{}
  $pluralsNames = @()
  foreach ($el in $doc.SelectNodes('//plurals')) {
    $name = $el.GetAttribute('name')
    $pluralsNames += $name
    $quantities = @()
    foreach ($item in $el.SelectNodes('item')) { $quantities += $item.GetAttribute('quantity') }
    $plurals[$name] = ($quantities -join ',')
  }
  $dupPlurals = $pluralsNames | Group-Object | Where-Object { $_.Count -gt 1 }
  foreach ($d in $dupPlurals) { $failures.Add("$($dir.Name): duplicate <plurals name=`"$($d.Name)`">") | Out-Null }

  $placeholders = @{}
  foreach ($el in $doc.SelectNodes('//string')) {
    $tokens = @($placeholderRegex.Matches($el.InnerText) | ForEach-Object { $_.Value })
    $placeholders[$el.GetAttribute('name')] = ($tokens -join '|')
  }
  foreach ($el in $doc.SelectNodes('//plurals')) {
    $tokens = @()
    foreach ($item in $el.SelectNodes('item')) { $tokens += @($placeholderRegex.Matches($item.InnerText) | ForEach-Object { $_.Value }) }
    $tokens = $tokens | Sort-Object
    $placeholders["plurals:$($el.GetAttribute('name'))"] = ($tokens -join '|')
  }

  $data[$dir.Name] = [pscustomobject]@{
    StringKeys   = $stringKeys
    Plurals      = $plurals
    Placeholders = $placeholders
  }
}

$en = $data['values']
$bn = $data['values-bn']

if (-not $en) { $failures.Add('values/ (en) not found') | Out-Null }
if (-not $bn) { $failures.Add('values-bn/ not found') | Out-Null }

if ($en -and $bn) {
  $enSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$en.StringKeys)
  $bnSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$bn.StringKeys)

  foreach ($k in $enSet) { if (-not $bnSet.Contains($k)) { $failures.Add("values-bn: missing key '$k'") | Out-Null } }
  foreach ($k in $bnSet) { if (-not $enSet.Contains($k)) { $failures.Add("values-bn: extra key '$k'") | Out-Null } }

  foreach ($name in $en.Plurals.Keys) {
    if (-not $bn.Plurals.ContainsKey($name)) { $failures.Add("values-bn: missing plurals '$name'") | Out-Null }
    elseif ($en.Plurals[$name] -ne $bn.Plurals[$name]) { $failures.Add("values-bn: plurals '$name' quantities differ from en") | Out-Null }
  }
  foreach ($name in $bn.Plurals.Keys) { if (-not $en.Plurals.ContainsKey($name)) { $failures.Add("values-bn: extra plurals '$name'") | Out-Null } }
}

if ($en) {
  foreach ($name in $en.Placeholders.Keys) {
    foreach ($locale in $data.Keys) {
      $other = $data[$locale]
      if ($other.Placeholders.ContainsKey($name) -and $other.Placeholders[$name] -ne $en.Placeholders[$name]) {
        $failures.Add("${locale}: placeholder mismatch for '$name' (en: '$($en.Placeholders[$name])' vs '$($other.Placeholders[$name])')") | Out-Null
      }
    }
  }
}

# Report-only: staleness of other locales (never fails).
$enCount = if ($en) { $en.StringKeys.Count } else { 0 }
Write-Output "Locale staleness report (key count vs values/ = $enCount):"
foreach ($name in ($data.Keys | Where-Object { $_ -ne 'values' -and $_ -ne 'values-bn' } | Sort-Object)) {
  Write-Output ("  {0}: {1}/{2} keys" -f $name, $data[$name].StringKeys.Count, $enCount)
}

if ($failures.Count -gt 0) {
  Write-Output 'Locale validation FAILED:'
  $failures | ForEach-Object { Write-Output " - $_" }
  exit 1
}

Write-Output 'Locale validation passed (en<->bn parity, placeholders, XML, mojibake scan).'
