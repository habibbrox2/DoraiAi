# Migration recon — UI v2 (Phase 1)

Scout report merged by Orchestrator. Branch: `feature/ui-v2-migration` (created from `main`, local changes in `.vscode/settings.json` + `iosApp/Configuration/Config.xcconfig` preserved, prototype copied to `docs/design/dorai-ui-v2-prototype.html`).

## Modules / source sets

| Path | Notes |
|---|---|
| `settings.gradle.kts` | root `dorai`; modules `:composeApp`, `:androidApp`, `:screenshotTests` |
| `composeApp/build.gradle.kts` | `android` lib `com.dorai.ai.shared`; `iosArm64`/`iosSimulatorArm64` framework `ComposeApp`; `jvm("desktop")` entry `com.dorai.ai.MainKt`; `wasmJs` (not release-ready) |
| `composeApp/src/commonMain/.../App.kt` | shared entry `App(navController, textToSpeech, isKoinStarted, onAppOpens, androidUpdateContent)` + `AppContent` |
| `androidApp/.../MainActivity.kt` | `ComponentActivity`, `rememberNavController`, `App(..., isKoinStarted=true)`; flavors `playStore`/`foss` (foss default), pkg `com.dorai.ai` |
| `composeApp/src/desktopMain/.../main.kt` | `application { Window(1280x800) { App(...) } }` |
| `composeApp/src/wasmJsMain/.../main.kt` | `ComposeViewport { App(...) }` + `bindToBrowserNavigation` |
| `composeApp/src/iosMain/.../MainViewController.kt` | `ComposeUIViewController { App(...) }` |
| `composeApp/src/commonMain/.../AppModule.kt` | Koin `appModule`, all `viewModel{}` registrations |
| `gradle/libs.versions.toml` | version catalog (do not add deps without approval) |

Source sets: `commonMain`, `androidMain` + `desktopMain` share `src/jvmShared/kotlin`, `iosMain`, `wasmJsMain`, `commonTest`, `desktopTest`. Android library target, iOS arm64 + simulator arm64, Desktop JVM.

## Checklist mapping (exists / partial / missing)

Main application: Dashboard (exists `ui/dashboard/DashboardScreen`), Chat empty (exists `ChatScreen` + `EmptyState`), Chat conversation (exists `ChatTimeline`/`BotMessage`/`UserMessage`), Chat dynamic UI (exists `ui/dynamicui/DoraiUiRenderer`, `isInteractiveMode`, `FrozenSubmission`), Chat composer (exists `ChatComposer`/`QuestionInput`).

Settings (7 tabs today, spec wants 8): General (exists), Appearance/Theme presets (exists `DoraiAppearanceSettings`), Agent (exists `AgentSettings`), Providers/models as `Services` (exists), Tools/Skills merged into `SettingsTab.Tools` (exists), MCP/Integrations folded into Tools Connectors (partial — no separate `SettingsTab.Mcp`), Linux Sandbox (exists, Android-gated via `SandboxViewModel.showSandbox`), Account & Data (exists merged `DataControlContent` + `AccountPlanContent`).

Workspace: Setup (exists `WorkspaceSetupContent`), Project list/ready (exists `WorkspaceProjectsContent`), Active project (exists `WorkspaceScreen`), Agent tab (exists `WorkspaceTab.Agent`), Editor (exists `WorkspaceCodeEditor`), Terminal (exists `WorkspaceTerminalContent`), Git (exists `WorkspaceGitContent`).

Agent interaction states: tool running/completed/failed (exists `ToolExecutionCard`), approval pending (exists `ToolConfirmationCard` Deny/Allow once/session/Always, `PendingToolConfirmation`), shell running/completed/failed (exists `ShellExecutionCard`), agent activity timeline (exists `AgentActivityTimeline` + `AgentActivityDisclosure`, `ActivityMapper`).

Appearance/responsive: Midnight dark / Light (exists via presets), Phone/Tablet/Desktop (exists but different breakpoints/popups; no 78dp rail / 224dp drawer / 250–300dp list pane as in v2).

## Theme (real identifiers)

- Presets: `theme/data/ThemePresets` (13: `dark`, `light`, `ogShine`, `peach`, `midnight`, `lilac`, `dusk`, `seafoam`, `graphite`, `daybreak`, `sherbet`, `paper`, `carbon`)
- State: `theme/data/ThemeState`/`ThemePalette`/`ThemeStyle{Flat,ThreeD}`; engine `theme/engine/DoraiTheme` + `LocalDoraiThemeState`/`LocalDoraiPalette`/`LocalDoraiThemeStyle`; Flat/3D via `theme/engine/ElevationStyles:Modifier.doraiSurfaceStyle`
- Legacy dual system: `ui/Theme.kt` (`*ColorScheme`, `DoraiTypography`, `DoraiShapes`, `Theme()` preview) vs `theme/engine/DoraiTheme` authoritative — `ThemeMode` deprecated in `AppSettings`/`SettingsUiState`
- Tokens: `ui/theme/DesignTokens` (`DesignColorTokens`, `SpacingXxs..Xxxl`, `MinTouchTarget`, `Radius*`); typography `DoraiTypography` M3 57/45/36/32/28/24/22/16/14/12/11sp
- Switch: `DoraiAppearanceSettings` → `ThemeViewModel.setThemeStyle()` → `LocalDoraiThemeStyle`

Ownership mapping (spec `ui/theme/**` etc. adjusted to real layout):

| Spec owner | Real paths |
|---|---|
| Theme | `theme/data/**`, `theme/engine/**`, `ui/theme/**` |
| Shell/nav | `App.kt`, `ui/chat/ChatAdaptivePolicy.kt`, `ui/chat/ChatModeScreen`, `ui/settings/SettingsScreen` |
| Components | `ui/components/**`, `ui/chat/composables/**`, `ui/dynamicui/**` |
| Home | `ui/dashboard/**` |
| Chat | `ui/chat/**` |
| Workspace | `ui/workspace/**` |
| Settings | `ui/settings/**` |

## Navigation / state holders

Typed routes in `App.kt`: `Dashboard` (dashboard), `Home` (home, start), `Settings(tab:String?)` + `tabOf/of()`, `Build(project:String="")` (build). `NavHost` + `composable<T>`. `SettingsTab` as `String` to avoid desktop/iOS NavType crash. Per-destination back stack exists via NavHost but no separate per-destination state store beyond ViewModels.

Holders (Koin `AppModule`): `ChatViewModel(DataRepository,TaskScheduler)` → `ChatUiState`/`ChatActions`; `SettingsViewModel` → `SettingsUiState`/`SettingsActions` + `SandboxViewModel`; `WorkspaceViewModel` (`WorkspaceUiState`, `WorkspaceGitState`) from `WorkspaceController`; `ThemeViewModel` → `ThemeState`; root hoisted `ChatViewModel` in `AppContent`.

Back: layered `PlatformBackHandler` in `ChatModeScreen` (sandbox → drawer → interactive → double-press exit); `Settings/Build navigateUp()`.

Approval badge: **no nav badge today** — `ToolConfirmationCard` inline; `pendingToolConfirmations` is the source to derive a Workspace dot count.

## Strings / i18n

`composeResources/values/strings.xml` (en) + `values-bn/strings.xml` + ~53 locales, `Res.*`. Switch: `data/AppLanguage{Bengali("bn"),English("en")}` default Bengali, `AppSettings.appLanguageFlow` → `App.kt LaunchedEffect { applyAppLanguage() }` (expect in `Platform.kt`, actuals `ApplyAppLanguage{Jvm,Ios,Wasm}`), `SettingsViewModel.onChangeAppLanguage`.

## Previews / screenshot tests

One `@Preview` (`Theme.kt`) + stateless `*ScreenContent` test surfaces. `screenshotTests/` Paparazzi `PIXEL_9A`, goldens in `src/test/snapshots/images/` (`ScreenshotTest.kt`, `PremiumScreenScreenshotTest.kt`, `AppNavigationScreenshotTest`, `DoraiUiScreenshotTest`, `R4MissingScreenScreenshotTest`).

## Build / test commands (not run in Phase 1)

Windows wrapper, Java 21:

```bat
gradlew.bat spotlessCheck
gradlew.bat :composeApp:desktopTest
gradlew.bat :androidApp:compileFossDebugKotlin :androidApp:testFossDebugUnitTest
gradlew.bat :androidApp:assembleFossDebug
gradlew.bat :composeApp:wasmJsBrowserDistribution
gradlew.bat :screenshotTests:verifyPaparazziDebug
```

iOS needs macOS/Xcode. Run narrowest first; only claim PASS on `BUILD SUCCESSFUL`.

## Risks / open questions

1. v2 shell is architectural (rail/drawer/list-detail vs current drawer + pill tabs + `NavigationBar`).
2. Theme duality legacy `ui/Theme.kt` vs authoritative `theme/engine`.
3. No composer mode state today (Ask/Plan/Build/Review must be data-driven new).
4. Timeline split `AgentActivityTimeline` vs `AgentActivityDisclosure`.
5. Workspace/Sandbox Android-gated (`isBuildAvailable`/`isSandboxAvailable`) — define desktop/wasm/iOS behaviour.
6. Nav approval badge needs new derived count.
7. Default language Bengali — v2 strings need bn + en (technical terms stay English in bn text).

Questions for user (section 9 of the instruction) must be answered before Phase 2.
