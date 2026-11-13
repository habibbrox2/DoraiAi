<#
.SYNOPSIS
  Reports locale coverage for the Compose Resources tree and can emit a translation
  worklist for locales that are behind the default (English) resource file.

.DESCRIPTION
  The English file (values/strings.xml) is the source of truth. Every other locale may
  lag behind; Compose Resources falls back to English for missing keys at runtime, so a
  lagging locale degrades to English instead of crashing. This script makes that backlog
  visible and actionable:

    - coverage report: present / missing key counts per locale
    - worklist export:  the exact key names a locale still needs
    - regression check: compared against scripts/locale-coverage-baseline.json, it fails
                        when a locale LOSES keys it already had (a real regression)

  It never writes translations. Filling the backlog is a human translation task; this
  script only produces the input for it.

.PARAMETER RepoRoot
  Repository root. Defaults to the current git top level.

.PARAMETER Out
  Optional path for a Markdown worklist. Without it, only the console report is printed.

.PARAMETER Locale
  Optional locale filter (e.g. 'values-bn'). Limits the worklist to one locale.

.PARAMETER FailOnRegression
  Fail (exit 1) when a locale has fewer keys than the recorded baseline.

.EXAMPLE
  pwsh scripts/locale-coverage.ps1
  pwsh scripts/locale-coverage.ps1 -Locale values-bn -Out build/locale-worklist-bn.md
#>
[CmdletBinding()]
param(
  [string]$RepoRoot,
  [string]$Out,
  [string]$Locale,
  [switch]$FailOnRegression
)

$ErrorActionPreference = 'Stop'

if (!$RepoRoot) { $RepoRoot = (& git rev-parse --show-toplevel 2>$null).Trim() }
if (!$RepoRoot -or !(Test-Path $RepoRoot)) { throw 'Run inside a Git worktree or pass -RepoRoot.' }

$resDir = Join-Path $RepoRoot 'composeApp\src\commonMain\composeResources'
if (!(Test-Path $resDir)) { throw "composeResources not found at $resDir" }

$baselinePath = Join-Path $RepoRoot 'scripts\locale-coverage-baseline.json'

function Get-StringKeys([string]$file) {
  $doc = New-Object System.Xml.XmlDocument
  $doc.Load($file)
  $keys = @()
  foreach ($el in $doc.SelectNodes('//string')) { $keys += $el.GetAttribute('name') }
  return $keys
}

$defaultFile = Join-Path $resDir 'values\strings.xml'
if (!(Test-Path $defaultFile)) { throw 'values/strings.xml (English source) not found.' }
$defaultKeys = @(Get-StringKeys $defaultFile)
$defaultSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$defaultKeys)

$locales = Get-ChildItem $resDir -Directory | Where-Object { $_.Name -like 'values*' } | Sort-Object Name

$report = @()
$regressions = @()
$worklist = @{}

foreach ($dir in $locales) {
  $file = Join-Path $dir.FullName 'strings.xml'
  if (!(Test-Path $file)) { continue }
  $keys = @(Get-StringKeys $file)
  $present = [System.Collections.Generic.HashSet[string]]::new([string[]]$keys)
  $missing = @($defaultKeys | Where-Object { -not $present.Contains($_) })
  $extra = @($keys | Where-Object { -not $defaultSet.Contains($_) })

  $report += [pscustomobject]@{
    Locale  = $dir.Name
    Present = $keys.Count
    Missing = $missing.Count
    Extra   = $extra.Count
  }

  if ($missing.Count -gt 0 -and (!$Locale -or $dir.Name -eq $Locale)) {
    $worklist[$dir.Name] = $missing
  }

  if ($Locale -and $dir.Name -eq $Locale) {
    Write-Output ("{0}: {1}/{2} keys present, {3} missing, {4} extra" -f $dir.Name, $keys.Count, $defaultKeys.Count, $missing.Count, $extra.Count)
  }
}

Write-Output ''
Write-Output ('Locale coverage (English source has {0} keys):' -f $defaultKeys.Count)
foreach ($row in $report) {
  $status = if ($row.Missing -eq 0) { 'complete' } else { 'behind' }
  Write-Output ('  {0,-16} {1,5} keys present, {2,4} missing, {3} {4}' -f $row.Locale, $row.Present, $row.Missing, $row.Extra, $status)
}

# Regression guard: a locale must never lose keys it already had.
if (Test-Path $baselinePath) {
  $baseline = Get-Content $baselinePath -Raw | ConvertFrom-Json
  $baselineMap = @{}
  foreach ($prop in $baseline.locales.PSObject.Properties) { $baselineMap[$prop.Name] = [int]$prop.Value }
  foreach ($row in $report) {
    if ($baselineMap.ContainsKey($row.Locale) -and $row.Present -lt $baselineMap[$row.Locale]) {
      $regressions += ("{0}: {1} keys now, baseline recorded {2} (lost {3})" -f $row.Locale, $row.Present, $baselineMap[$row.Locale], ($baselineMap[$row.Locale] - $row.Present))
    }
  }
} else {
  Write-Output ''
  Write-Output "No baseline file at scripts/locale-coverage-baseline.json; regression check skipped."
}

if ($worklist.Count -gt 0) {
  $lines = @('# Locale translation worklist', '', ('English source: values/strings.xml ({0} keys).' -f $defaultKeys.Count), '', 'Keys listed here are missing from the locale and currently fall back to English at runtime.', '')
  foreach ($name in ($worklist.Keys | Sort-Object)) {
    $lines += "## $name ($($worklist[$name].Count) missing)"
    $lines += ''
    foreach ($key in $worklist[$name]) { $lines += ('- `{0}`' -f $key) }
    $lines += ''
  }
  if ($Out) {
    $outDir = Split-Path -Parent $Out
    if ($outDir -and !(Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
    $joined = [string]::Join([char]10, $lines)
    Set-Content -LiteralPath $Out -Value $joined -Encoding UTF8
    Write-Output ''
    Write-Output "Worklist written to $Out"
  }
}

if ($regressions.Count -gt 0) {
  Write-Output ''
  Write-Output 'Locale coverage REGRESSED:'
  $regressions | ForEach-Object { Write-Output " - $_" }
  if ($FailOnRegression) { exit 1 }
} else {
  Write-Output ''
  Write-Output 'No locale lost keys relative to the recorded baseline.'
}
