[CmdletBinding()]
param([ValidateSet('Auto','Docs','Fast','Feature','Full','Wasm')][string]$Profile='Auto',[string]$Base='origin/main',[switch]$NoGradle,[switch]$WhatIf)
$ErrorActionPreference='Stop'
$repo=(& git rev-parse --show-toplevel 2>$null).Trim();if($LASTEXITCODE -ne 0 -or !$repo){throw 'Run inside a Git worktree.'}
$changed=@(& git -C $repo diff --name-only $Base);if($LASTEXITCODE -ne 0){throw "Unable to diff against $Base; fetch the base ref or choose -Base."}
$changed+=@(& git -C $repo diff --cached --name-only);$changed+=@(& git -C $repo ls-files --others --exclude-standard);$changed=@($changed|Where-Object{$_}|Select-Object -Unique)
if(!$changed.Count -and $Profile -eq 'Auto'){Write-Output 'No changed files detected; Auto profile has nothing to validate.';exit 0}
$docsOnly=$true;$high=$false;$ui=$false;$android=$false;$desktop=$false;$common=$false;$wasmOnly=$false;$frontend=$false
foreach($f in $changed){
 if($f -notmatch '^(docs/|.*\.md$|.*\.mdx$|.*\.txt$|\.github/ISSUE_TEMPLATE/)'){$docsOnly=$false}
 if($f -match '^(gradle/|gradlew|gradlew\.bat|settings\.gradle|build\.gradle|.*build\.gradle\.kts$|gradle\.properties$|\.github/workflows/|hybrid/contracts/|.*sqldelight/|.*Security.*|.*security.*|.*AppModule\.kt$|.*expect.*|.*actual.*)'){$high=$true}
 if($f -match '^(screenshotTests/|composeApp/src/commonMain/.*/(ui|theme|designsystem)/|composeApp/src/commonMain/composeResources/)'){$ui=$true}
 if($f -match '^(frontend/|package.json$|pnpm-lock.yaml$|pnpm-workspace.yaml$|hybrid/contracts/)'){$frontend=$true}
 if($f -match 'composeApp/src/androidMain/|androidApp/'){$android=$true}
 if($f -match 'composeApp/src/desktopMain/|composeApp/src/jvmShared/'){$desktop=$true}
 if($f -match 'composeApp/src/commonMain/|composeApp/src/commonTest/|composeApp/src/iosMain/|composeApp/src/wasmJsMain/'){$common=$true}
 if($f -match 'composeApp/src/wasmJsMain/|composeApp/src/wasmJsTest/'){$wasmOnly=$true}
}
if($Profile -eq 'Auto'){if($docsOnly){$Profile='Docs'}elseif($high){$Profile='Full'}elseif($wasmOnly){$Profile='Wasm'}elseif($ui -or $common -or $android -or $desktop){$Profile='Feature'}else{$Profile='Fast'}}
$tasks=@()
if($Profile -eq 'Full'){$frontend=$true}
if($Profile -ne 'Docs'){
 $tasks+='spotlessCheck'
 switch($Profile){
  'Fast' {if($common -or $desktop){$tasks+=':composeApp:desktopTest'};if($android){$tasks+=':androidApp:compileFossDebugKotlin'}}
  'Feature' {if($common -or $desktop){$tasks+=':composeApp:desktopTest'};if($common -or $android){$tasks+=':androidApp:compileFossDebugKotlin';$tasks+=':androidApp:testFossDebugUnitTest'};if($ui){$tasks+=':screenshotTests:verifyPaparazziDebug'}}
  'Full' {$tasks+=':composeApp:desktopTest';$tasks+=':androidApp:compileFossDebugKotlin';$tasks+=':androidApp:testFossDebugUnitTest';$tasks+=':screenshotTests:verifyPaparazziDebug'}
  'Wasm' {$tasks+=':composeApp:compileKotlinWasmJs'}
 }
}
Write-Output "Changed files ($($changed.Count)):";$changed|ForEach-Object{Write-Output " - $_"};Write-Output "Selected profile: $Profile";if($tasks.Count){Write-Output "Gradle tasks: $($tasks -join ', ')"}else{Write-Output 'No Gradle tasks selected.'};if($frontend){Write-Output 'Frontend task: pnpm run frontend:check'}
if($Profile -eq 'Docs'){Write-Output 'Documentation-only validation: git diff --check';& git -C $repo diff --check;if($LASTEXITCODE -ne 0){exit $LASTEXITCODE};exit 0}
if($WhatIf){Write-Output 'Dry run only; no validation process started.';exit 0}
if($NoGradle -and $tasks.Count){Write-Output 'Skipping Gradle tasks (-NoGradle).'}
elseif($tasks.Count){
 $wrapper=Join-Path $repo 'gradlew.bat';if($env:OS -ne 'Windows_NT' -or !(Test-Path $wrapper)){$wrapper=Join-Path $repo 'gradlew';if(!(Test-Path $wrapper)){throw 'Gradle wrapper not found.'}}
 Push-Location $repo;try{& $wrapper @tasks '--no-daemon' '--stacktrace';$code=$LASTEXITCODE;if($code -ne 0){throw "Gradle validation failed with exit code $code"}}finally{Pop-Location}
}
if($frontend){$pnpm=Get-Command pnpm -ErrorAction SilentlyContinue;if(!$pnpm){throw 'pnpm is required for frontend validation. Install the repository-pinned pnpm version with Corepack.'};Push-Location $repo;try{& pnpm run frontend:check;$code=$LASTEXITCODE;if($code -ne 0){throw "Frontend validation failed with exit code $code"}}finally{Pop-Location}}
Write-Output "Validation passed: $Profile"
