[CmdletBinding()]
param(
  [ValidateSet('Auto','Docs','Fast','Feature','Full','Wasm','Frontend')][string]$Profile = 'Auto',
  [string]$Base = 'origin/main',
  [switch]$NoGradle,
  [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$repo = (& git rev-parse --show-toplevel 2>$null).Trim()
if ($LASTEXITCODE -ne 0 -or !$repo) { throw 'Run inside a Git worktree.' }

$changed = @(& git -C $repo diff --name-only $Base)
if ($LASTEXITCODE -ne 0) { throw "Unable to diff against $Base; fetch the base ref or choose -Base." }
$changed += @(& git -C $repo diff --cached --name-only)
$changed += @(& git -C $repo ls-files --others --exclude-standard)
$changed = @($changed | Where-Object { $_ } | Select-Object -Unique)

# Auto is intentionally a no-op for a clean tree. Explicit profiles are commands,
# so they must still run when there are no changed paths (e.g. Full/Wasm diagnostics).
if (!$changed.Count -and $Profile -eq 'Auto') {
  Write-Output 'No changed files detected; Auto profile has nothing to validate.'
  return
}

$docsOnly = $true
$high = $false
$ui = $false
$screenshots = $false
$android = $false
$desktop = $false
$common = $false
$wasmOnly = $false
$frontend = $false
$locales = $false

foreach ($file in $changed) {
  if ($file -notmatch '^(docs/|.*\.md$|.*\.mdx$|.*\.txt$|\.github/ISSUE_TEMPLATE/)') { $docsOnly = $false }
  if ($file -match '^(gradle/|gradlew|gradlew\.bat|settings\.gradle|build\.gradle|.*build\.gradle\.kts$|gradle\.properties$|\.github/workflows/|hybrid/contracts/|.*sqldelight/|.*Security.*|.*security.*|.*AppModule\.kt$|.*expect.*|.*actual.*)') { $high = $true }
  if ($file -match '^composeApp/src/commonMain/.*/(ui|theme|designsystem)/|composeApp/src/commonMain/composeResources/') { $ui = $true }
  if ($file -match '^screenshotTests/') { $screenshots = $true }
  if ($file -match '^composeApp/src/androidMain/|^androidApp/') { $android = $true }
  if ($file -match '^composeApp/src/desktopMain/|^composeApp/src/jvmShared/') { $desktop = $true }
  if ($file -match '^composeApp/src/commonMain/|^composeApp/src/commonTest/|^composeApp/src/iosMain/|^composeApp/src/wasmJsMain/') { $common = $true }
  if ($file -match '^composeApp/src/wasmJsMain/|^composeApp/src/wasmJsTest/') { $wasmOnly = $true }
  if ($file -match '^(frontend/|hybrid/contracts/|\.github/workflows/|package\.json$|pnpm-lock\.yaml$|pnpm-workspace\.yaml$)') { $frontend = $true }
  if ($file -match '^composeApp/src/commonMain/composeResources/') { $locales = $true }
}

if ($Profile -eq 'Auto') {
  if ($docsOnly) { $Profile = 'Docs' }
  elseif ($high) { $Profile = 'Full' }
  elseif ($wasmOnly) { $Profile = 'Wasm' }
  elseif ($frontend) { $Profile = 'Frontend' }
  elseif ($ui -or $screenshots -or $common -or $android -or $desktop) { $Profile = 'Feature' }
  else { $Profile = 'Fast' }
}

$tasks = @()
$runFrontend = $frontend -or $Profile -eq 'Frontend'
if ($Profile -ne 'Docs') {
  if ($Profile -ne 'Frontend') { $tasks += 'spotlessCheck' }
  switch ($Profile) {
    'Fast' {
      if ($common -or $desktop) { $tasks += ':composeApp:desktopTest' }
      if ($android) { $tasks += ':androidApp:compileFossDebugKotlin' }
    }
    'Feature' {
      if ($common -or $desktop) { $tasks += ':composeApp:desktopTest' }
      if ($common -or $android) {
        $tasks += ':androidApp:compileFossDebugKotlin'
        $tasks += ':androidApp:testFossDebugUnitTest'
      }
      if ($ui -or $screenshots) { $tasks += ':screenshotTests:verifyPaparazziDebug' }
    }
    'Full' {
      $tasks += ':composeApp:desktopTest'
      $tasks += ':androidApp:compileFossDebugKotlin'
      $tasks += ':androidApp:testFossDebugUnitTest'
      if ($ui -or $screenshots) { $tasks += ':screenshotTests:verifyPaparazziDebug' }
    }
    'Wasm' { $tasks += ':composeApp:compileKotlinWasmJs' }
    'Frontend' { }
  }
}

if ($locales) {
  Write-Output 'Locale resources changed: running scripts/validate-locales.ps1'
  & (Join-Path $repo 'scripts\validate-locales.ps1') -RepoRoot $repo
  if ($LASTEXITCODE -ne 0) { throw "Locale validation failed with exit code $LASTEXITCODE" }
}

Write-Output "Changed files ($($changed.Count)):"
$changed | ForEach-Object { Write-Output " - $_" }
Write-Output "Selected profile: $Profile"
if ($tasks.Count) { Write-Output "Gradle tasks: $($tasks -join ', ')" }
if ($runFrontend) { Write-Output 'Frontend tasks: pnpm frontend:check' }
if (!$tasks.Count -and !$runFrontend) { Write-Output 'No validation tasks selected.' }

if ($Profile -eq 'Docs') {
  Write-Output 'Documentation-only validation: git diff --check'
  & git -C $repo diff --check
  if ($LASTEXITCODE -ne 0) { throw "Documentation validation failed with exit code $LASTEXITCODE" }
  return
}
if ($NoGradle -or $WhatIf) {
  Write-Output 'Dry run only; no Gradle or frontend process started.'
  return
}

if ($tasks.Count) {
  $wrapper = Join-Path $repo 'gradlew.bat'
  if ($env:OS -ne 'Windows_NT' -or !(Test-Path $wrapper)) {
    $wrapper = Join-Path $repo 'gradlew'
    if (!(Test-Path $wrapper)) { throw 'Gradle wrapper not found.' }
  }
  Push-Location $repo
  try {
    & $wrapper @tasks '--no-daemon' '--stacktrace'
    $code = $LASTEXITCODE
    if ($code -ne 0) { throw "Gradle validation failed with exit code $code" }
  }
  finally { Pop-Location }
}

if ($runFrontend) {
  if (!(Get-Command pnpm -ErrorAction SilentlyContinue)) {
    throw 'pnpm is required for frontend validation. Install the pinned workspace package manager and run pnpm install --frozen-lockfile.'
  }
  Push-Location $repo
  try {
    & pnpm frontend:check
    $code = $LASTEXITCODE
    if ($code -ne 0) { throw "Frontend validation failed with exit code $code" }
  }
  finally { Pop-Location }
}

Write-Output "Validation passed: $Profile"
