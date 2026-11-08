# DORAI AI — UI v2 Wiring Audit & Reachability Trace

**Document:** `docs/design/wiring-audit.md`  
**Phase:** Phase A — Tracer (Read-Only Baseline)  
**Date:** 2026-11-08  
**Repository Branch:** `fix/ui-v2-wiring` (tracking `origin/main` at commit `22a87e2920d87b5ddb88a673a73d256a4e43acb1`)

---

## A1. Which Code is Being Built and Installed

- **Git Status:**
  - Branch: `fix/ui-v2-wiring` based directly on `origin/main` (`22a87e29`).
  - Pre-existing user modification preserved: `iosApp/Configuration/Config.xcconfig` (`APP_VERSION=2.1.17`).
- **Merge Status in `main`:**
  - Pull Request #48 (`b5d53e26 ui-v2: implement UI v2 layout, adaptive scaffold, screens, and Paparazzi screenshots`) was merged into `main`.
- **Application Identifiers & Version Configuration:**
  - Application ID: `com.dorai.ai` (verified in `androidApp/build.gradle.kts` and `androidApp/src/main/AndroidManifest.xml`).
  - `versionName`: `2.1.17` (`gradle/libs.versions.toml`).
  - `versionCode`: `229` (`gradle/libs.versions.toml`).
  - Flavors: `foss` (default: `isDefault = true`) and `playStore`.
- **Target Device / Emulator Status:**
  - Tested host `adb devices`: No active emulator or physical USB device currently connected on this host (`adb devices` output: empty).
  - Note: GitHub Release `v2.1.16` was tagged *before* PR #48 was merged. If an APK was downloaded from GitHub Releases or built from an earlier tag, it does not contain the v2 code. If built from `main`, the runtime issues below explain why the old UI was still displayed.

---

## A2. Feature Flags and Switches

- **Flag Name:** `KEY_UI_V2 = "ui_v2_enabled"`
- **Implementation:** `composeApp/src/commonMain/kotlin/com/dorai/ai/data/AppSettings.kt`
  ```kotlin
  private val _uiV2Flow = MutableStateFlow(settings.getBoolean(KEY_UI_V2, true))
  val uiV2Flow: StateFlow<Boolean> = _uiV2Flow
  fun isUiV2Enabled(): Boolean = _uiV2Flow.value
  fun setUiV2Enabled(enabled: Boolean) {
      settings.putBoolean(KEY_UI_V2, enabled)
      _uiV2Flow.value = enabled
  }
  ```
- **Defaults:**
  - `settings.getBoolean(KEY_UI_V2, true)` returns `true` for a clean install.
  - If a device had a pre-existing SharedPreferences file where `ui_v2_enabled` was saved or defaulted to `false`, it evaluates to `false` and falls back to the legacy `NavHost`.
  - There is currently **no UI toggle** in the app allowing users to inspect or re-enable `ui_v2_enabled`.
- **App Entry Root Switch:**
  - In `composeApp/src/commonMain/kotlin/com/dorai/ai/App.kt` line 204:
    ```kotlin
    val isUiV2 by appSettings.uiV2Flow.collectAsStateWithLifecycle()
    if (isUiV2) {
        DoraiAppShell(...)
    } else {
        NavHost(navController, startDestination = Home, ...) { ... }
    }
    ```

---

## A3. Reachability Trace (Core Audit)

The following table details the **actual composable rendered at runtime** on the `isUiV2 == true` path in `App.kt`:

| Screen group | Composable really rendered | File | v2? | Evidence (call chain / grep) |
|---|---|---|---|---|
| **Home / Dashboard** | `DoraiHomeScreen` | `ui/home/DoraiHomeScreen.kt` | **YES** | `App.kt:228` renders `DoraiHomeScreen(state = homeState, ...)` |
| **Chat: empty** | `ChatScreen` (Legacy) | `ui/chat/ChatScreen.kt` | **NO** | `App.kt:246` calls legacy `ChatScreen(viewModel, ...)`; `DoraiChatScreen` is **NEVER** called! |
| **Chat: conversation** | `ChatScreen` (Legacy) | `ui/chat/ChatScreen.kt` | **NO** | `App.kt:246` routes to legacy `ChatScreen`. |
| **Chat: dynamic UI response** | Legacy `ChatScreen` rendering | `ui/chat/ChatScreen.kt` | **NO** | `DoraiDynamicUiBlock` is in `DoraiChatScreen.kt:294` which is uncalled. |
| **Chat: composer + modes** | Legacy `ChatScreen` composer | `ui/chat/ChatScreen.kt` | **NO** | `DoraiComposer` & `DoraiModeSelector` in `DoraiChatScreen` are uncalled. |
| **Settings: General** | `GeneralSettingsContent` (Placeholder) | `ui/settings/DoraiSettingsScreen.kt` | **PARTIAL** | Rendered inside `DoraiSettingsScreen:281`, but contains only static hardcoded text; missing app update controls. |
| **Settings: Appearance** | `AppearanceSettingsContent` | `ui/settings/DoraiSettingsScreen.kt` | **YES** | `DoraiSettingsScreen:288` renders Flat/3D toggle & 13 preset grid. |
| **Settings: Agent** | `AgentSettingsContent` (Mock) | `ui/settings/DoraiSettingsScreen.kt` | **NO** | `DoraiSettingsScreen:371` has local `remember { mutableStateOf }` disconnected from `AppSettings` or `AgentSettings`. |
| **Settings: AI Providers** | `ProvidersSettingsContent` (Mock) | `ui/settings/DoraiSettingsScreen.kt` | **NO** | `DoraiSettingsScreen:410` renders static hardcoded rows ("Free FAST", "Ollama Local"); real `ServicesSettings` is not wired! |
| **Settings: Tools / Skills** | `ToolsSettingsContent` (Mock) | `ui/settings/DoraiSettingsScreen.kt` | **NO** | `DoraiSettingsScreen:445` renders static hardcoded cards ("edit_file", "run_shell"); real tools/skills unreached. |
| **Settings: Integrations / MCP** | `McpSettingsContent` (Mock) | `ui/settings/DoraiSettingsScreen.kt` | **NO** | `DoraiSettingsScreen:472` dummy text field with empty `onClick = {}`; real MCP repository unreached. |
| **Settings: Linux Sandbox** | `SandboxSettingsContent` (Mock) | `ui/settings/DoraiSettingsScreen.kt` | **NO** | `DoraiSettingsScreen:514` static mock text ("Debian 12", "1.2 GB / 4.0 GB used"); real sandbox status unreached. |
| **Settings: Account & Data** | `AccountSettingsContent` (Mock) | `ui/settings/DoraiSettingsScreen.kt` | **NO** | `DoraiSettingsScreen:533` dummy delete modal with empty `onClick = {}`; real data export/account unreached. |
| **Workspace: setup** | `WorkspaceSetupView` (Mock) | `ui/workspace/DoraiWorkspaceScreen.kt` | **NO** | `App.kt:273` hardcodes `isSetupCompleted = true`, bypassing setup entirely; callbacks `onStartSandbox = {}` are empty. |
| **Workspace: project list / ready** | `WorkspaceProjectsView` (Mock) | `ui/workspace/DoraiWorkspaceScreen.kt` | **NO** | `App.kt:262` hardcodes a mock project with Windows path `"g:/Web/DoraiAi-migration-worktree"`. |
| **Workspace: active project tabs** | `ActiveProjectView` (Mock) | `ui/workspace/DoraiWorkspaceScreen.kt` | **NO** | Rendered with static dummy diff blocks and fake shell blocks; real `WorkspaceViewModel`, code editor & terminal unreached. |
| **Agent states: timeline / approvals** | `DoraiAgentTimeline` / `DoraiToolCard` | `ui/components/v2/` | **NO** | Defined in `DoraiComponentsPreview.kt` & `DoraiChatScreen.kt`, but never reached from production runtime. |

---

## A4. Orphan Detection

The following composables were implemented in PR #48, tested in `screenshotTests`, but have **0 production references** in the application call tree:

1. **`DoraiChatScreen`** (`composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/DoraiChatScreen.kt`):
   - Grep for `DoraiChatScreen`: referenced only in `DoraiChatScreenTest.kt` and `DoraiUiV2ScreenshotTest.kt`.
   - **Production references: 0**.
2. **`DoraiAgentTimeline`** (`composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/DoraiAgentTimeline.kt`):
   - **Production references: 0**.
3. **`DoraiApprovalActions`** (`composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/DoraiApprovalActions.kt`):
   - **Production references: 0**.
4. **`DoraiComposer`** (`composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/DoraiComposer.kt`):
   - Only referenced in orphan `DoraiChatScreen` and previews.
5. **`DoraiModeSelector`** (`composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/DoraiModeSelector.kt`):
   - Only referenced in orphan `DoraiChatScreen` and previews.
6. **`DoraiDynamicUiBlock`** (`composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/DoraiDynamicUiBlock.kt`):
   - Only referenced in orphan `DoraiChatScreen` and previews.
7. **`DoraiDiffBlock`** (`composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/DoraiDiffBlock.kt`):
   - Only referenced in mock `DoraiWorkspaceScreen` and previews.

---

## A5. Other Known Ways This Fails (Verification Matrix)

1. **New scaffold exists but `App()` renders old one?**
   - *NO.* `App.kt` renders `DoraiAppShell` when `isUiV2 == true`.
2. **New screen body exists but old route still registered?**
   - *YES.* In `App.kt:246`, `chatContent` explicitly constructs legacy `ChatScreen`, completely ignoring `DoraiChatScreen`.
3. **Platform source set overrides the shared screen?**
   - *NO.* `androidMain` delegates directly to common `App()`.
4. **Settings / Workspace opens through separate dialog / NavHost?**
   - *YES.* When user is inside legacy `ChatScreen`, tapping top bar actions or links triggers legacy navigation callbacks or dialogs that bypass `DoraiAppShell`.
5. **Width logic only switches at ≥ 600dp?**
   - *NO.* `DoraiAppShell` properly provides compact bottom bar at phone widths (<600dp).
6. **Two token systems?**
   - *YES.* `DoraiTheme` / `LocalDoraiColors` was added alongside `ThemeState` / `LocalDesignColorTokens` without full unification.
7. **Koin or ScreenFactory still building old screen?**
   - *NO.* Screens are constructed directly in `App.kt`.
8. **Saved state forces old screen?**
   - *YES.* If `ui_v2_enabled` was persisted as `false` in a prior run, it defaults to the legacy `NavHost`.
9. **Old and new screens share a name in different packages?**
   - *NO.* Names are distinct (`ChatScreen` vs `DoraiChatScreen`, `SettingsScreen` vs `DoraiSettingsScreen`).
10. **Build on phone was made before migration commits?**
    - *YES.* A downloaded GitHub Release APK (e.g. `v2.1.16`) predated PR #48, and even with PR #48 merged, `DoraiChatScreen` was not wired in `App.kt`.

---

## A6. Conclusion

### Confirmed Causes
1. **Unwired Chat (`DoraiChatScreen` Orphan):** `App.kt` line 246 passed legacy `ChatScreen` into `DoraiAppShell.chatContent`. `DoraiChatScreen` had 0 callers in production.
2. **Mock Settings Data & Dead Navigation:** `DoraiSettingsScreen` was wired with an empty `onCategoryChanged = {}` lambda and hardcoded `SettingsCategory.General`. Its 7 non-appearance tabs were static placeholder mocks not connected to real repositories/ViewModels.
3. **Mock Workspace Data:** `DoraiWorkspaceScreen` was wired with hardcoded static project data (`"g:/Web/DoraiAi-migration-worktree"`) and empty lambdas, disconnected from real workspace sessions.
4. **Fallback Branch Persistence Risk:** If `AppSettings` had `KEY_UI_V2` persisted as `false`, the entire v2 shell was bypassed for legacy `NavHost`.

### Ruled Out
- Missing v2 Composables: All v2 screens, components, and design tokens exist and pass screenshot tests in isolation.
- Missing Manifest/Android permissions or Edge-to-Edge window handling.

### Could Not Verify
- On-device visual output via ADB: No physical device or running emulator is connected to the build machine's ADB host. Verification must rely on Robolectric/Paparazzi screenshot tests and verified call-chain analysis.

---

## B. Completed UI v2 Rewiring & Resolution Verification (Phases B, C, D, E)

### B1. Updated Post-Fix Reachability Trace

| Screen group | Composable really rendered | File | v2? | Production Call Chain |
|---|---|---|---|---|
| **App Root Scaffold** | `DoraiAppShell` | `ui/adaptive/DoraiAppShell.kt` | **YES** | `App.kt` -> `DoraiTheme` -> `DoraiAppShell` with `statusBarsPadding()` safe area insets |
| **Home / Dashboard** | `DoraiHomeScreen` | `ui/home/DoraiHomeScreen.kt` | **YES** | `App.kt` -> `DoraiAppShell.homeContent` -> `DoraiHomeScreen` (live model + approvals count) |
| **Chat: empty** | `DoraiChatScreen` | `ui/chat/DoraiChatScreen.kt` | **YES** | `App.kt` -> `DoraiChatScreenConnected` -> `DoraiChatScreen` -> `ChatEmptyState` |
| **Chat: conversation** | `DoraiChatScreen` | `ui/chat/DoraiChatScreen.kt` | **YES** | `App.kt` -> `DoraiChatScreenConnected` -> `DoraiChatScreen` -> `ChatMessageRow` |
| **Chat: dynamic UI response** | `DoraiDynamicUiBlock` | `ui/components/v2/DoraiDynamicUiBlock.kt` | **YES** | `DoraiChatScreen` -> `ChatMessageRow` -> `DoraiDynamicUiBlock` |
| **Chat: composer + modes** | `DoraiComposer` & `DoraiModeSelector` | `ui/components/v2/` | **YES** | `DoraiChatScreen` -> `ChatDetailPane` -> `DoraiComposer` + `DoraiModeSelector` |
| **Settings: General** | `GeneralContent` + Build Stamp | `ui/settings/GeneralSettings.kt` | **YES** | `App.kt` -> `DoraiSettingsScreenConnected` -> `DoraiSettingsScreen` -> `GeneralContent` + FOSS Update Card + Build Stamp |
| **Settings: Appearance** | `AppearanceSettingsContent` | `ui/settings/DoraiSettingsScreen.kt` | **YES** | `DoraiSettingsScreen` -> `AppearanceSettingsContent` (Flat/3D + 13 ThemePresets) |
| **Settings: Agent** | `AgentContent` | `ui/settings/AgentSettings.kt` | **YES** | `DoraiSettingsScreen` -> `AgentContent` (Approval policy, Timeline, Memory, Automation tasks, Heartbeat) |
| **Settings: AI Providers** | `ServicesContent` | `ui/settings/ServicesSettings.kt` | **YES** | `DoraiSettingsScreen` -> `ServicesContent` (Configured AI providers, API keys, test connection) |
| **Settings: Tools / Skills** | `PluginsSection` & `SkillsSection` | `ui/settings/ToolsSettings.kt` | **YES** | `DoraiSettingsScreen` -> `PluginsSection` + `SkillsSection` (with GitHub skill installer) |
| **Settings: Integrations / MCP**| `McpServersSection` | `ui/settings/McpSection.kt` | **YES** | `DoraiSettingsScreen` -> `McpServersSection` (GitHub Copilot MCP, popular servers, custom MCP URL) |
| **Settings: Linux Sandbox** | `SandboxSettingsCard` | `ui/settings/SandboxSettings.kt` | **YES** | `DoraiSettingsScreen` -> `SandboxSettingsCard` (Debian 12 distro selector, setup, start, reset, packages) |
| **Settings: Account & Data** | `DataControlContent` | `ui/settings/SettingsDetailPane.kt` | **YES** | `DoraiSettingsScreen` -> `DataControlContent` + `AccountPlanContent` (Clear chats, export/import data) |
| **Workspace: setup** | `WorkspaceSetupView` | `ui/workspace/DoraiWorkspaceScreen.kt` | **YES** | `App.kt` -> `DoraiWorkspaceScreenConnected` -> `DoraiWorkspaceScreen` (checks `uiState.build.isReady`) |
| **Workspace: project list** | `WorkspaceProjectsView` | `ui/workspace/DoraiWorkspaceScreen.kt` | **YES** | `DoraiWorkspaceScreenConnected` -> `DoraiWorkspaceScreen` -> `WorkspaceProjectsView` (real projects from `/root/projects`) |
| **Workspace: active project tabs**| `ActiveProjectView` | `ui/workspace/DoraiWorkspaceScreen.kt` | **YES** | `DoraiWorkspaceScreenConnected` -> `ActiveProjectView` (live `WorkspaceTerminalContent`, editor, git diffs) |
| **Agent states: timeline / cards**| `DoraiToolCard` | `ui/components/v2/DoraiToolCard.kt` | **YES** | `DoraiChatScreen` -> `ChatMessageRow` -> `DoraiToolCard` (Running, Completed, Failed, PendingApproval with allow/deny actions) |

### B2. Orphan Elimination Proof

All 7 previously orphaned composables are now fully reached from production runtime:
1. `DoraiChatScreen`: Invoked by `DoraiChatScreenConnected` in `App.kt` line 256. (Production callers: 1).
2. `DoraiComposer`: Invoked in `DoraiChatScreen.kt` line 521. (Production callers: 1).
3. `DoraiModeSelector`: Invoked in `DoraiChatScreen.kt` line 515. (Production callers: 1).
4. `DoraiDynamicUiBlock`: Invoked in `DoraiChatScreen.kt` line 617. (Production callers: 1).
5. `DoraiToolCard`: Invoked in `DoraiChatScreen.kt` line 604 and `DoraiWorkspaceScreen.kt` lines 473 & 482. (Production callers: 3).
6. `DoraiDiffBlock`: Invoked in `DoraiWorkspaceScreen.kt` line 573. (Production callers: 1).
7. `DoraiSettingsScreen`: Invoked by `DoraiSettingsScreenConnected` in `App.kt` line 269. (Production callers: 1).

### B3. Safe Area Insets Verification
- Top padding: `DoraiAdaptiveScaffold` has `statusBarsPadding()` applied across Compact, Medium/Expanded, and Large width classes.
- Bottom padding: `DoraiBottomNavBar` has `WindowInsets.navigationBars` padding.
- Visual regressions verified: Paparazzi screenshot test suite passes 100% (`BUILD SUCCESSFUL`).

### B4. Verification Gates Matrix

| Verification Check | Command | Result | Notes |
|---|---|---|---|
| Kotlin Desktop Compilation | `gradlew.bat compileKotlinDesktop` | **PASS** | 0 errors |
| Reachability Test Suite | `gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.ui.adaptive.DoraiUiV2ReachabilityTest"` | **PASS** | Shell state, 8 settings categories, backstack |
| Paparazzi Screenshot Golden Recording | `gradlew.bat :screenshotTests:recordPaparazziDebug --tests "com.dorai.ai.screenshots.DoraiUiV2ScreenshotTest"` | **PASS** | Status bar padding goldens updated |
| Paparazzi Screenshot Verification | `gradlew.bat :screenshotTests:verifyPaparazziDebug` | **PASS** | 100% pixel matching across themes and form factors |
| Spotless Formatting Gate | `gradlew.bat spotlessCheck` | **PASS** | ktlint & formatting rules satisfied |
| Android FOSS Build Gate | `gradlew.bat :androidApp:assembleFossDebug` | **PASS** | Output APK generated cleanly |


