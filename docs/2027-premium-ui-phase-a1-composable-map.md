# Phase A1 — Composable-to-Route and Feature Ownership Map

**Repository:** `habibbrox2/brox-kai-2.0`  
**Branch:** `feature/2027-premium-agent-ui-migration`  
**Audited head before this document:** `6af4d4e77eece8bce749608896400edeb8c76235`  
**Scope:** 137 Kotlin files under `composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/ui`  
**Method:** Git tree inventory plus source inspection of App navigation and the major route/surface composables. This is a structural ownership map, not a claim that every private composable or runtime state has been visually tested.

## 1. Verified application route graph

Source of truth: `composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/App.kt`.

| Route | Navigation destination | Root Composable | Owner / state boundary |
|---|---|---|---|
| Home | `Home` (start destination) | `ChatScreen` | `ChatViewModel` / `ChatUiState` / `ChatActions` |
| Dashboard | `Dashboard` | `DashboardScreen` | Dashboard content derives from chat state plus build/sandbox readiness controllers |
| Settings | `Settings(tab)` | `SettingsScreen` | `SettingsViewModel`, `SandboxViewModel`, `SettingsActions` |
| Build | `Build(project)` | `KaiBuildScreen` | `KaiBuildViewModel` / build controller and terminal session state |

Route facts verified in source:
- `NavHost` starts at `Home`.
- Home navigates to Dashboard and Settings; its shell also exposes workspace/sandbox flows.
- Dashboard can navigate to Home, Settings, and Build, and can open a conversation or start a design chat.
- Settings tab selection is serialized as a string route argument and resolved through `Settings.tabOf`.
- Build receives an optional project identifier through its typed route.
- Sandbox is not a top-level NavHost destination in `App.kt`; its UI is embedded in the supported Chat/workspace flow.
- Full-screen image viewing is hosted above the NavHost by `FullScreenImageHost`.

## 2. Route → parent → child surface map

### Home / Chat

`App.Home → ChatScreen → ChatScreenContent → ChatModeScreen → ChatShell`

| Parent / feature owner | Child surfaces identified from source |
|---|---|
| Chat shell | `ChatShell`, `TopBar`, `AgentHud`, `AgentStatusOrb`, `AppSidebar`, `ConversationRail` |
| Conversation timeline | `ChatTimeline`, `UserMessage`, `BotMessage`, `ErrorMessage`, `MessageActionRow`, `HeartbeatBanner`, `PendingSmsBanners` |
| Composer | `ChatComposer`, `QuestionInput`, `SkillAutocomplete`, `ServiceSelector`, `TrailingIcon`, `CircleIconButton` |
| Agent activity | `AgentActivityDisclosure`, `AgentActivityTimeline`, `AgentActivityRow`, `ActivityCodePreview`, `ToolExecutionCard`, `ShellExecutionCard`, `ToolConfirmationCard`, `ToolMessage`, `AgentReviewComponents` |
| Chat overlays / support | `ChatHistorySheet`, `FullScreenImageViewer`, `FreeProviderSuggestionsPanel`, `StatusPill`, `SmallIconButton`, `PanelHeading`, `PremiumChatBackdrop` |
| Chat state / mapping (non-UI helpers) | `ChatUiState`, `ChatTimelineData`, `ChatActions`, `ChatAdaptivePolicy`, `ActivityMapper`, `AgentActivity`, `AgentReviewModels` |

Primary owner: `ChatViewModel` and `ChatActions`. Presentation components receive state and callbacks; they must not take ownership of provider, persistence, MCP, sandbox execution, or permission business logic.

### Dashboard

`App.Dashboard → DashboardScreen → DashboardScreenContent`

Children verified in source:
- `DashboardHeader`
- `ReadinessCard`
- `DashboardActionGrid` / `DashboardActionCard`
- `RecentWorkSection`
- `ConversationRow` / `ProjectRow`

Models: `DashboardModels.kt`. Navigation callbacks return to Chat, open Build/Settings, start Design chat, or load a conversation.

### Settings

`App.Settings(tab) → SettingsScreen → SettingsScreenContent → SettingsNavRail / SettingsOverview / SettingsDetailPane`

| Settings owner | Child section / surface files |
|---|---|
| Navigation and layout | `SettingsNavigation.kt`, `SettingsOverview.kt`, `SettingsDetailPane.kt`, `SettingsTabScroll.kt`, `SettingsSectionHeader.kt` |
| General / appearance | `GeneralSettings.kt`, `AppearanceSettings.kt`, `HeartbeatSection.kt` |
| Agent / model | `AgentSettings.kt`, `ModelSelectionSheet.kt` |
| Services / integrations | `ServicesSettings.kt`, `IntegrationsSettings.kt` |
| Tools / MCP / skills | `ToolsSettings.kt`, `McpSection.kt`, `SkillsSection.kt` |
| Sandbox / terminal | `SandboxSettings.kt`, `SandboxViewModel.kt`, `TerminalSheet.kt`, `AnsiParser.kt` |
| Data / account | `ExportImportSection.kt`, `DataControlContent`, `AccountPlanContent` |
| Settings support | `SettingsActions.kt`, `SettingsUiState.kt`, `SettingsViewModel.kt`, `SettingsConfirmDialog.kt`, `SettingsStatusBanner.kt`, `SettingsStatusRow.kt`, `SplinterlandsComposables.kt`, `SplinterlandsUiState.kt`, `SplinterlandsViewModel.kt`, `SettingsDesignTokens.kt` |

The Settings root also receives optional Android update content. Sandbox settings state is supplied separately from `SandboxViewModel`.

### Build workspace

`App.Build(project) → KaiBuildScreen → KaiBuildScreenContent`

Children:
- `BuildSetupContent` (setup/install steps and setup errors)
- `BuildProjectsContent` (project list, create/rename/delete dialogs, system/agent cards)
- `BuildSessionBar`
- `BuildStatusPill`
- `BuildSectionHeader`
- `BuildTerminalContent` → terminal hyperlink bar and terminal grid
- `TerminalKeyRow` / `TerminalKeyIcons`

State and actions: `KaiBuildViewModel`, `KaiBuildUiState`, `KaiBuildActions`, build session/terminal models. Terminal-specific ANSI and terminal canvas presentation are intentionally separate from generic app surfaces.

### Sandbox (embedded Chat/workspace feature, not a top-level App route)

Parent: Chat/workspace sandbox content → `SandboxTabsContent`.

Children:
- `SandboxFileBrowserScreen.kt` → `SandboxFilesContent`, path bar, file list/row/menu, editor body, unopenable-file view, delete/rename dialogs.
- `SandboxPackagesScreen.kt` → `SandboxPackagesContent`, packages list/row, upgrade row, uninstall confirmation.
- `FindReplacePanel.kt`, `ReplaceAllConfirmDialog.kt`, `SandboxStatusText.kt`.
- State owners: `SandboxFileBrowserViewModel`, `SandboxPackagesViewModel`, `SandboxSessionViewModel`, plus Settings sandbox state/view model.

### Dynamic UI / interactive response renderer

Parent: Chat assistant message rendering → `KaiUiRenderer` → typed `KaiUiNode` tree.

Renderer children include column, row, card, text, button, text input, checkbox, select, image, table, list, switch, slider, radio group, progress, countdown, alert, chip group, icon, code, quote, badge, stat, avatar, box, tabs, and accordion renderers.

Supporting files: `KaiUiNode.kt`, `KaiUiNodeBuilders.kt`, `KaiUiParser.kt`, `UiAction.kt`, `KaiUiTts.kt`. Renderer actions must remain typed/allowlisted and must not execute arbitrary model-provided code.

### Markdown / response content

Parent: Chat message content → `MarkdownContent` / `MarkdownRenderer`.

Supporting files:
- Parsing: `BlockScanner.kt`, `MarkdownParser.kt`, `MarkdownNode.kt`, `InlineTokenizer.kt`, `EmojiShortcodes.kt`.
- Rendering: `MarkdownRenderer.kt`, `MarkdownTextRenderer.kt`, `MarkdownInlineRenderer.kt`, `InlineContent.kt`.
- Blocks: `CodeFenceBlock.kt`, `CodeHighlighter.kt`, `DiffCard.kt`.
- Math: `MathAtom.kt`, `MathParser.kt`, `MathRenderer.kt`, `MathSymbols.kt`.

### Shared primitives / platform support

Shared presentation primitives used by the route trees:
- `KaiDialog`, `SettingsListItem`, `KaiBadge`, `KaiChip`, `KaiSearchField`, `KaiSlider`, `KaiSnackbar`, `KaiLoadingSpinner`, `OperationalComponents`, `RefreshIconButton`, `KaiAvatar`, `AnimatedGradientBorder`, `LogoAnimation`, `DesktopScrollbar`, `FullScreenImageHost`.
- Theme roots: `Theme.kt`, `KaiAppTheme.kt`, `DesignTokens.kt`.
- Other UI/platform helpers: `Clipboard.kt`, `SandboxUriHandler.kt`, `KaiIcons.kt`, `GoogleMapsView.kt`.

## 3. Complete 137-file source inventory

This inventory is derived from the recursive Git tree at the audited branch head. Each file is assigned to its feature owner above; helper/model/view-model files are listed with their owning surface, not represented as independent navigation routes.

### Theme and shared roots (4)
- `Clipboard.kt`
- `KaiAppTheme.kt`
- `SandboxUriHandler.kt`
- `Theme.kt`

### Build (11)
- `build/BuildDesignTokens.kt`
- `build/BuildProjectsContent.kt`
- `build/BuildSectionHeader.kt`
- `build/BuildSessionBar.kt`
- `build/BuildSetupContent.kt`
- `build/BuildStatusPill.kt`
- `build/BuildTerminalContent.kt`
- `build/KaiBuildScreen.kt`
- `build/KaiBuildViewModel.kt`
- `build/TerminalKeyIcons.kt`
- `build/TerminalKeyRow.kt`

### Chat (40)
- `chat/ChatActions.kt`
- `chat/ChatAdaptivePolicy.kt`
- `chat/ChatDesignTokens.kt`
- `chat/ChatScreen.kt`
- `chat/ChatShell.kt`
- `chat/ChatTimelineData.kt`
- `chat/ChatUiState.kt`
- `chat/ChatViewModel.kt`
- `chat/activities/ActivityMapper.kt`
- `chat/activities/AgentActivity.kt`
- `chat/activities/AgentReviewModels.kt`
- `chat/composables/AgentActivityDisclosure.kt`
- `chat/composables/AgentActivityTimeline.kt`
- `chat/composables/AgentHud.kt`
- `chat/composables/AgentReviewComponents.kt`
- `chat/composables/AppSidebar.kt`
- `chat/composables/BotMessage.kt`
- `chat/composables/ChatComposer.kt`
- `chat/composables/ChatHistorySheet.kt`
- `chat/composables/ChatTimeline.kt`
- `chat/composables/ConversationRail.kt`
- `chat/composables/EmptyState.kt`
- `chat/composables/ErrorMessage.kt`
- `chat/composables/FreeProviderSuggestionsPanel.kt`
- `chat/composables/FullScreenImageViewer.kt`
- `chat/composables/HeartbeatBanner.kt`
- `chat/composables/MessageActionRow.kt`
- `chat/composables/PanelHeading.kt`
- `chat/composables/PendingSmsBanner.kt`
- `chat/composables/PremiumChatBackdrop.kt`
- `chat/composables/QuestionInput.kt`
- `chat/composables/ServiceSelector.kt`
- `chat/composables/SkillAutocomplete.kt`
- `chat/composables/SmallIconButton.kt`
- `chat/composables/StatusPill.kt`
- `chat/composables/ToolConfirmationCard.kt`
- `chat/composables/ToolExecutionCard.kt`
- `chat/composables/ToolMessage.kt`
- `chat/composables/TopBar.kt`
- `chat/composables/UserMessage.kt`

### Shared components (15)
- `components/AnimatedGradientBorder.kt`
- `components/DesktopScrollbar.kt`
- `components/FullScreenImageHost.kt`
- `components/KaiAvatar.kt`
- `components/KaiBadge.kt`
- `components/KaiChip.kt`
- `components/KaiDialog.kt`
- `components/KaiLoadingSpinner.kt`
- `components/KaiSearchField.kt`
- `components/KaiSlider.kt`
- `components/KaiSnackbar.kt`
- `components/LogoAnimation.kt`
- `components/OperationalComponents.kt`
- `components/RefreshIconButton.kt`
- `components/SettingsListItem.kt`

### Dashboard (2)
- `dashboard/DashboardModels.kt`
- `dashboard/DashboardScreen.kt`

### Dynamic UI (6)
- `dynamicui/KaiUiNode.kt`
- `dynamicui/KaiUiNodeBuilders.kt`
- `dynamicui/KaiUiParser.kt`
- `dynamicui/KaiUiRenderer.kt`
- `dynamicui/KaiUiTts.kt`
- `dynamicui/UiAction.kt`

### Icons and maps (2)
- `icons/KaiIcons.kt`
- `maps/GoogleMapsView.kt`

### Markdown (16)
- `markdown/BlockScanner.kt`
- `markdown/CodeFenceBlock.kt`
- `markdown/CodeHighlighter.kt`
- `markdown/DiffCard.kt`
- `markdown/EmojiShortcodes.kt`
- `markdown/InlineContent.kt`
- `markdown/InlineTokenizer.kt`
- `markdown/MarkdownInlineRenderer.kt`
- `markdown/MarkdownNode.kt`
- `markdown/MarkdownParser.kt`
- `markdown/MarkdownRenderer.kt`
- `markdown/MarkdownTextRenderer.kt`
- `markdown/math/MathAtom.kt`
- `markdown/math/MathParser.kt`
- `markdown/math/MathRenderer.kt`
- `markdown/math/MathSymbols.kt`

### Sandbox (9)
- `sandbox/FindReplacePanel.kt`
- `sandbox/ReplaceAllConfirmDialog.kt`
- `sandbox/SandboxFileBrowserScreen.kt`
- `sandbox/SandboxFileBrowserViewModel.kt`
- `sandbox/SandboxPackagesScreen.kt`
- `sandbox/SandboxPackagesViewModel.kt`
- `sandbox/SandboxSessionViewModel.kt`
- `sandbox/SandboxStatusText.kt`
- `sandbox/SandboxTabsContent.kt`

### Settings (31)
- `settings/AgentSettings.kt`
- `settings/AnsiParser.kt`
- `settings/AppearanceSettings.kt`
- `settings/ExportImportSection.kt`
- `settings/GeneralSettings.kt`
- `settings/HeartbeatSection.kt`
- `settings/IntegrationsSettings.kt`
- `settings/McpSection.kt`
- `settings/ModelSelectionSheet.kt`
- `settings/SandboxSettings.kt`
- `settings/SandboxViewModel.kt`
- `settings/ServicesSettings.kt`
- `settings/SettingsActions.kt`
- `settings/SettingsConfirmDialog.kt`
- `settings/SettingsDesignTokens.kt`
- `settings/SettingsDetailPane.kt`
- `settings/SettingsNavigation.kt`
- `settings/SettingsOverview.kt`
- `settings/SettingsScreen.kt`
- `settings/SettingsSectionHeader.kt`
- `settings/SettingsStatusBanner.kt`
- `settings/SettingsStatusRow.kt`
- `settings/SettingsTabScroll.kt`
- `settings/SettingsUiState.kt`
- `settings/SettingsViewModel.kt`
- `settings/SkillsSection.kt`
- `settings/SplinterlandsComposables.kt`
- `settings/SplinterlandsUiState.kt`
- `settings/SplinterlandsViewModel.kt`
- `settings/TerminalSheet.kt`
- `settings/ToolsSettings.kt`

### Theme tokens (1)
- `theme/DesignTokens.kt`

## 4. Phase A1 findings and gaps

### Verified
- All 137 files are present in the current UI source inventory.
- The four application navigation destinations and their root Composables are verified from `App.kt`.
- Major parent/child feature ownership is verified for Chat, Dashboard, Settings, Build, Sandbox, Dynamic UI, Markdown, and shared primitives.
- Sandbox is an embedded workspace flow, not a separate top-level NavHost destination.
- UI state ownership boundaries are identified for Chat, Settings, Build, and Sandbox.

### Not yet fully verified
- Exact call-site parent for every private Composable in all 137 files (especially shared utilities, settings subsections, Markdown leaf renderers, and dynamically selected sections).
- Every SettingsTab enum value → exact section Composable mapping.
- Every conditional branch and platform-specific visibility path.
- Full set of nested Dialog/Sheet/Popover ownership links.
- Runtime-only routes opened by platform-specific integrations outside the commonMain NavHost.
- Accessibility semantics and per-state coverage; these are Phase C work, not Phase A1 closure evidence.

### Next audit actions
1. Finish exact SettingsTab → section call-site mapping.
2. Trace every ChatScreen conditional slot and the full ChatShell parent-child composition.
3. Trace Build and Sandbox content switch branches to exact child composables.
4. Complete shared-component consumer map and dynamic Markdown/Dynamic UI embedding map.
5. Update the coverage tracker only after every unresolved mapping row is either source-verified or explicitly marked not applicable.


## 6. Verified composition call-site trace (Phase A1 closure)

The following call-site trace was checked against the current branch source, not inferred from file names:

### Chat route and responsive shell

- `AppContent` wraps `NavHost` in `FullScreenImageHost`; Home resolves to `ChatScreen`, Dashboard to `DashboardScreen`, Settings to `SettingsScreen`, and Build to `KaiBuildScreen`.
- `ChatScreen` delegates to `ChatScreenContent`, then `ChatModeScreen`. `ChatModeScreen` owns local presentation state (drawer, sandbox visibility, composer prefill, snackbar/scroll state) while actions remain on `ChatUiState.actions`.
- `ChatModeScreen` supplies `ChatShell` slots: `TopBar` as header; `AppSidebar` as the expanded conversation rail and compact modal drawer content; timeline/empty-state plus `ChatComposer` as main content; `SandboxTabsContent` as compact main content or expanded side panel; snackbar/error/free-provider UI in overlays.
- `ChatShell` places header above content. Only Expanded width renders optional rail and side panel; Compact/Medium renders main content full-width. The caller owns the compact drawer and decides whether sandbox is shown.
- Main-content branches verified: empty history → `EmptyState`; non-empty history → `ChatTimeline`; compact sandbox-open → `SandboxTabsContent` replaces timeline/composer; composer is omitted while sandbox is open.

### Settings route and all tab branches

- `SettingsScreen` → adaptive settings shell/navigation → `SettingsDetailPane`.
- Exact `SettingsTab` dispatch in `SettingsDetailPane`:
  - General → `SettingsOverview` + `GeneralContent` (Android update slot is passed here).
  - Appearance → `AppearanceContent`.
  - Agent → `AgentContent`.
  - Services → `ServicesContent`.
  - Tools → `ToolsTabContent` (receives sandbox state).
  - Integrations → `IntegrationsContent`.
  - Sandbox → `SandboxSettingsCard` when sandbox UI is available; otherwise `SandboxUnavailableContent`.
  - Data → `DataControlContent`.
  - Account → `AccountPlanContent`.
- Navigation rail labels/icons and grouping were checked against the same nine enum values in `SettingsNavigation.kt`. Groups: Assistant (General/Appearance/Agent), Models & services (Services), Connections (Tools/Integrations), Workspace (Sandbox), Account (Data/Account).

### Dashboard route

- `DashboardScreen` collects SandboxController and KaiBuildController state, builds `DashboardUiState`, then calls `DashboardScreenContent`.
- `DashboardScreenContent` composes exactly: `DashboardHeader` → `ReadinessCard` → `DashboardActionGrid` → `RecentWorkSection`.
- `RecentWorkSection` conditionally shows conversation rows when conversations exist and project rows only when projects exist and Build is available. Grid columns are 1 below 600dp, 2 below 1000dp, and 4 otherwise.

### Build route

- `KaiBuildScreen` owns ViewModel collection/actions and delegates presentation to `KaiBuildScreenContent`.
- With an open project: `BuildSessionBar` is shown; then Files → `SandboxFilesContent`; else a matching/fallback session → `BuildTerminalContent`; else a starting-session placeholder is shown and a session is requested.
- Without an open project: `KaiBuildTopBar` is shown; ready state → `BuildProjectsContent`; not-ready state → `BuildSetupContent`. Create-project dialog is an overlay controlled by the screen.

### Sandbox embedded workspace

- `ChatModeScreen` is the verified call site for `SandboxTabsContent` in both compact and expanded layouts. Build also reuses `SandboxFilesContent` as its project file view.
- Ready state: Terminal → optional `SessionChipRow` + `TerminalContent`; Files → `SandboxFilesContent`; Packages → `SandboxPackagesContent`.
- Not-ready state: setup card reports Needs setup / Installing / Failed, optionally shows progress/cancel or error, and offers setup/retry when not working.
- The tab selection is session-backed when `SandboxSessionViewModel` exists, otherwise local state is used (e.g. preview).

### Markdown and Dynamic UI embedding

- `MarkdownContent` converts source text to a parsed `MarkdownDocument` (with a safe plain-text fallback) and delegates each block to `BlockRenderer`.
- `BlockRenderer` dispatches headings/paragraphs/lists/quotes/tables/math/code/diff to their leaf renderers. A `KaiUiBlock` is the direct embedding point for `KaiUiRenderer`; malformed Kai-UI JSON is rendered as a JSON code fence.
- `KaiUiRenderer` recursively renders the typed `KaiUiNode` tree and passes callback data through its typed callback contract. Historical/read-only messages set `isInteractive = false` at the Markdown boundary.

### Platform and overlay ownership

- Top-level route set is commonMain NavHost; Android-only capability flags are passed from `AppContent` for Sandbox and Kai Build. Android update UI is an optional slot passed through Settings → General.
- Full-screen image hosting is above NavHost. Chat snackbar/error/free-provider overlays are owned by the chat route; Settings dialogs/sheets are owned by Settings; Build create-project dialog is owned by Build; Sandbox progress/setup UI is inline within its embedded workspace.
- Platform back handlers are attached to Dashboard, Settings, and Build route boundaries; Chat handles its own back/drawer/sandbox state.

### A1 coverage boundary

This closes the route/feature-level composition map for all top-level routes and the cross-feature surfaces that materially affect the premium UI migration. It is not a line-by-line inventory of every private leaf composable invocation or every shared primitive consumer; those are implementation-detail references and are intentionally not represented as a false 100% exhaustive call graph. All 137 source files remain assigned to their owning feature in the inventory above. Visual/accessibility state coverage remains Phase C verification.

## 7. Phase A1 status

**Status: Complete — all top-level route roots, route ownership, responsive Chat slots, nine Settings tab branches, Dashboard composition, Build state branches, Sandbox tabs/readiness branches, Markdown → Dynamic UI embedding, and major overlay/platform boundaries are source-verified.**

The closure is scoped to route and feature-level composition; it does not claim a complete private-composable call graph or runtime visual verification. No runtime behavior, navigation, provider, persistence, MCP, sandbox, or permission logic was changed by this audit. No merge, release, or deployment is included.
