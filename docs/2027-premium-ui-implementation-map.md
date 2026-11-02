# 2027 Premium UI — Full Audit & File Implementation Map

**Branch:** `feature/2027-premium-agent-ui-migration`  
**Audit baseline:** `7f42c54` (Phase A1 route-composition audit)  
**Scope:** Compose UI across commonMain, androidMain, desktopMain, iosMain and wasmJsMain.  
**Mode:** Read-only audit and implementation planning. No production UI source was changed while preparing this map.

## 1. Audit inventory and boundaries

The recursive Git tree at the audit baseline was complete (`truncated=false`).

| Inventory | Count | Notes |
|---|---:|---|
| UI Kotlin source files | 149 | All platform source sets; includes clipboard/URI/theme helpers and platform implementations, not only visible screens |
| Common UI files | 137 | Feature and shared UI files |
| Chat | 40 | Shell, timeline, composer, activity, tool cards, review, selectors |
| Settings | 31 | Navigation, sections, sheets, terminal, viewmodels |
| Shared components | 19 | Dialog, chips, badges, search, snackbar, etc. |
| Markdown | 16 | Parser, block/inline renderers, code, diff, math |
| Build | 11 | Project, setup, session, terminal, status |
| Sandbox | 9 | Tabs, file browser, packages, find/replace |
| Dynamic UI | 6 | Node model, parser, renderer, actions, TTS |
| Maps | 5 | Common/platform map implementations |
| Dashboard | 2 | Screen and models |
| Tests and screenshot test Kotlin files | 118 | Includes test fixtures/helpers; not 118 UI snapshot cases |

The earlier `137` figure refers to common UI Kotlin files. The expanded `149` count includes platform UI source files and shared root/platform helpers. These counts are not contradictory.

## 2. Architecture and theme contract

### Application ownership

- `App.kt` owns the application route shell and applies the selected Material color scheme to the root content.
- Top-level routes: Home → `ChatScreen`; Dashboard → `DashboardScreen`; Settings → `SettingsScreen`; Build → `KaiBuildScreen`.
- Sandbox is embedded in Chat/Build workspace flows, not a top-level route.
- Phase A1 source map: `docs/2027-premium-ui-phase-a1-composable-map.md`.

### Theme sources of truth — current state is inconsistent

| File / symbol | Current responsibility | Audit finding |
|---|---|---|
| `ui/theme/DesignTokens.kt` | 2027 base palette, spacing, radius | Static dark/OLED-oriented values; one global singleton |
| `ui/KaiAppTheme.kt` | Optional wrapper and LocalDesignTokens | Injects static DesignTokens regardless of colorScheme/useOLED; not the observed persisted-mode adapter |
| `ui/Theme.kt` | Material schemes, typography, legacy palette, shapes, borders, modifiers | 932-line mixed legacy/current theme file; mutable global KaiThemeColors plus CompositionLocal; multiple independent schemes |
| `ui/chat/ChatDesignTokens.kt` | Chat spacing/shapes/status/surface helpers | Duplicates global spacing and shape values; surface helpers partly depend on Material scheme |
| `ui/settings/SettingsDesignTokens.kt` | Settings spacing/shapes/status/surfaces | Duplicates global spacing/radii; colors mostly delegate to DesignTokens |
| `ui/build/BuildDesignTokens.kt` | Build spacing/shapes/status/surfaces/workspace tiers | Duplicates spacing/radii; terminal-specific surfaces and build status mapping are legitimate domain tokens |

The documented persisted theme contract in `docs/design-system/theme-contract.md` defines System, Light, Dark, Midnight and OLED Black. Midnight and OLED must remain distinct. Theme schemes also include branded/legacy alternatives (e.g. Lilac, Dusk, Seafoam, Graphite, Daybreak and provider-inspired palettes) in `Theme.kt`; before consolidating, all references and persisted settings must be traced. Do not remove a scheme or rename a persisted value as part of a visual refactor.

**High-risk issue:** `KaiAppTheme` currently provides the static `DesignTokens` object regardless of its selected `ColorScheme` and `useOLED` parameter. The active `Theme` path separately computes a legacy `KaiColorPalette`, mutates `KaiThemeColors`, then provides the same static DesignTokens. This can make components reading LocalDesignTokens disagree with Material light/OLED surfaces. Replace this with a mode-aware immutable token palette only after tracing all call sites and proving compatibility.

## 3. Design-system migration findings

### P0 — theme correctness / compatibility

1. Reconcile `DesignTokens`, `KaiColorPalette`, `KaiThemeColors`, `LocalKaiColors`, and `LocalDesignTokens` into a single mode-aware token contract.
2. Preserve all documented ThemeMode values and all still-referenced branded schemes.
3. Remove mutable global theme state only after replacing all property getter consumers with composition-local reads; global mutable state is unsafe for previews, parallel compositions, and multi-window desktop.
4. Make `useOLED` meaningful or remove it only after call-site and API compatibility review.
5. Keep Material colorScheme, content colors, typography and shapes coherent with the same selected mode.

### P1 — duplicated primitives / inconsistent raw values

- `ChatDesignTokens.kt`: duplicate 2–48dp spacing scale, chat-specific shapes, fallback purple and semantic status mapping.
- `SettingsDesignTokens.kt`: duplicate spacing/radii; retain settings-only layout constants (section/card gap) as feature aliases if needed.
- `BuildDesignTokens.kt`: duplicate common spacing/radii; preserve terminal shape, ANSI/status mapping and workspace width breakpoints as domain-specific values.
- `Theme.kt`: legacy global colors, shapes, borders, elevation helpers, card modifiers and text-field colors.
- `KaiDialog.kt`: raw 16dp shape, 24/20dp padding and 16dp gaps should map to semantic dialog tokens.
- `SettingsListItem.kt`: use shared spacing and shape aliases.
- `KaiSnackbar.kt`: keep status-dependent colors semantic; consolidate only geometry.
- Chat tool cards: `ToolExecutionCard.kt` general tool-call card already uses DesignTokens; preserve `ShellExecutionCard` terminal chrome, ANSI palette and window-control dots. `ToolConfirmationCard.kt` already uses semantic warning/error/success tokens and balanced action rows; review focus/disabled states before changing layout.
- `ModelSelectionSheet.kt`: general surfaces already use DesignTokens; preserve score bands as domain semantics unless product design explicitly redefines them. Verify loading/empty/error/retry/selected states.
- `CodeFenceBlock.kt`: hardcoded editor palette (#0A0E14, #161B22, #1E293B etc.) is a code-editor semantic palette; centralize as Markdown/code tokens, not generic brand surfaces.
- `DiffCard.kt`: added/removed/hunk colors are semantic diff colors; centralize as DiffPalette tokens and preserve contrast. Do not recolor as cyan brand UI.
- Terminal-specific ANSI colors, PTY cell metrics, terminal control dots and terminal canvas must remain isolated from generic theme migration.

### P2 — layout, typography and accessibility

- Audit all raw dp/sp and Material defaults in visible feature files; migrate only repeated visual primitives. Keep content-specific dimensions (terminal cells, icon sizes, chart/map dimensions, touch target constraints) explicit and documented.
- Confirm font scale, long localized strings, keyboard focus, hover, selected/disabled semantics, minimum 48dp targets, reduced-motion and screen-reader labels.
- Avoid changing interaction order, keyboard shortcuts, back handling, scrolling, state ownership, or accessibility role semantics in a visual-only change.

## 4. File-by-file implementation map

| Priority | File(s) | Planned change | Preserve / guardrail |
|---|---|---|---|
| P0 | `ui/theme/DesignTokens.kt` | Introduce immutable mode-aware token palette and typed semantic groups (surface/text/border/status/spacing/shape) | Keep stable names or compatibility aliases until all consumers migrate |
| P0 | `ui/Theme.kt` | Make theme composition-local driven; consolidate Material typography/shapes and legacy adapters | Preserve all active schemes and ThemeMode serialization; remove mutable globals only after call-site migration |
| P0 | `ui/KaiAppTheme.kt`, `App.kt` | Ensure selected theme mode and OLED setting feed the same token palette as Material | No navigation/persistence behavior change |
| P1 | `ui/chat/ChatDesignTokens.kt` | Convert duplicated geometry to aliases of shared tokens; retain chat-only max width/touch targets/status semantics | Preserve ChatAdaptivePolicy thresholds |
| P1 | `ui/settings/SettingsDesignTokens.kt` | Convert duplicated geometry to aliases; retain section/card spacing as named feature tokens | Preserve 840dp adaptive navigation and tab scroll |
| P1 | `ui/build/BuildDesignTokens.kt` | Convert generic geometry to aliases; keep terminal and workspace-specific tokens | Preserve PTY resize, terminal metrics, workspace tier thresholds |
| P1 | `ui/components/KaiDialog.kt`, `SettingsListItem.kt`, `KaiSnackbar.kt`, `OperationalComponents.kt`, `KaiBadge.kt`, `KaiChip.kt`, `KaiSearchField.kt` | Standardize surface/shape/spacing/focus/disabled styling | Preserve semantics, click actions, status meanings, touch target |
| P1 | `ui/chat/composables/ToolExecutionCard.kt`, `ToolConfirmationCard.kt`, `AgentHud.kt`, `AgentActivityTimeline.kt`, `AgentReviewComponents.kt`, `ChatComposer.kt`, `QuestionInput.kt`, `TopBar.kt`, `ConversationRail.kt`, `ChatTimeline.kt`, `UserMessage.kt`, `BotMessage.kt` | Normalize visible surfaces, typography, spacing, focus/hover/selected states | Preserve tool approval contract, message rendering, stream state, composer keyboard/attachment/slash actions, reduced motion |
| P1 | `ui/settings/ModelSelectionSheet.kt`, `AppearanceSettings.kt`, `SettingsNavigation.kt`, `SettingsDetailPane.kt`, all settings section files | Align card/section/selector/loading/error/retry/empty states | Preserve provider discovery, retry/cancel, ThemeMode, settings state and navigation |
| P1 | `ui/dashboard/DashboardScreen.kt`, `ui/build/KaiBuildScreen.kt`, `BuildProjectsContent.kt`, `BuildSetupContent.kt`, `BuildSessionBar.kt`, `BuildStatusPill.kt` | Align shell/card/header/empty/loading/error styling | Preserve project/session lifecycle and dashboard navigation |
| P1 | `ui/sandbox/SandboxTabsContent.kt`, `SandboxFileBrowserScreen.kt`, `SandboxPackagesScreen.kt`, `FindReplacePanel.kt`, `ReplaceAllConfirmDialog.kt`, `SandboxStatusText.kt` | Align tabs, file rows, package cards, dialogs and operation states | Preserve file operations, destructive confirmation, install/cancel/retry behavior |
| P1 | `ui/markdown/CodeFenceBlock.kt`, `DiffCard.kt`, `MarkdownRenderer.kt`, `MarkdownTextRenderer.kt`, `MathRenderer.kt` | Centralize code/diff semantic palettes and typography/shape geometry | Preserve syntax colors, diff semantics, math rendering, streaming tolerance and copy actions |
| P2 | `ui/dynamicui/KaiUiRenderer.kt`, `KaiUiNode.kt`, `KaiUiNodeBuilders.kt` | Audit renderer component styling and theme propagation | Preserve schema/parser/action contract and read-only historical rendering |
| P2 | `ui/maps/GoogleMapsView.kt` and platform map implementations | Review container/loading/error framing only | Do not alter provider SDK, map behavior or platform lifecycle |
| P2 | platform scrollbar implementations and root clipboard/URI helpers | Review only visible scrollbar chrome where applicable | Clipboard/URI behavior is out of scope; keep platform implementations intact |

The table is a change map, not permission to mechanically edit every listed file. A row is changed only when the implementation audit proves a concrete inconsistency.

## 5. Phase C regression matrix

Current test inventory includes 118 Kotlin test/fixture files across commonTest, desktopTest and screenshotTests. This does not mean every UI state has a screenshot. Existing relevant test classes include:

- Theme: `ThemeColorSchemeTest`
- Chat layout/state: `ChatAdaptivePolicyTest`, `AgentHudStateTest`, `ChatTimelineDataTest`
- Dashboard: `DashboardLayoutTest`
- Settings: `SettingsTabScrollTest`, `SettingsViewModelTest`
- Build: `KaiBuildViewModelTest`, `TerminalCellMetricsTest`, `TerminalKeyEncoderTest`, `TerminalMouseEncoderTest`
- Markdown/Dynamic UI: parsing, streaming tolerance and `KaiUiBlockIntegrationTest`
- Screenshots: `PremiumScreenScreenshotTest`, `KaiUiScreenshotTest`, `ScreenshotTest`, `MathScreenshotTest`, store/tablet screenshot suites.

| Surface | Required visual states | Required layout / theme dimensions |
|---|---|---|
| Theme/root | Light, Dark, Midnight, OLED, System | Verify selected Material scheme and LocalDesignTokens agree |
| Chat | empty, message, streaming, error, tool running/completed/failed, approval pending, expanded/collapsed | phone/tablet, compact/medium/expanded desktop, keyboard visible |
| Composer | empty/draft/multiline, attachment, slash/skill menu, sending/stopping, disabled | narrow width, large font, focus traversal |
| Model selection | loading, loaded, selected, empty, provider error, retry, free badge, score bands | compact and wide |
| Settings | all nine tabs, selected tab, scroll clipping, loading/error/unavailable | compact selector, 840dp rail transition, large font, Bengali/long labels |
| Dashboard | readiness, missing services, empty/recent conversations, projects | 1/2/4 column widths |
| Build | setup, installing/progress/cancel/error, project list, active session, terminal, unavailable | phone/tablet/desktop workspace tiers |
| Sandbox | setup, installing, failed/retry, terminal, files, packages, find, replace confirmation | compact/wide and destructive confirmation |
| Markdown | headings/lists/tables/code/copy/diff/math/invalid fallback/streaming | long code, horizontal overflow, light/dark |
| Dynamic UI | valid, legacy, malformed, oversized, interactive, historical read-only | action enabled/disabled and error fallback |
| Shared controls | hover, focus, pressed, selected, disabled, error, loading | mouse/keyboard/touch and 48dp minimum |
| Motion | normal and reduced motion | verify infinite/looping animation policy |

## 6. Known gaps / decisions required before source migration

1. `KaiAppTheme.useOLED` is currently not used to select tokens.
2. `LocalDesignTokens` is static and cannot distinguish Light, Dark, Midnight or OLED.
3. `KaiThemeColors` is mutable global state; all consumers must be inventoried before removal.
4. Branded/provider-specific schemes in `Theme.kt` must be checked against persisted selection and UI references before deciding whether to keep, adapt or deprecate.
5. Existing screenshot coverage is not a complete screen/state matrix. `PremiumScreenScreenshotTest` currently covers Dashboard dark, Build setup dark, tool execution running/failed dark, and pending tool confirmation dark.
6. The visual regression matrix document already defines Light/Dark/Midnight/OLED and device-size expectations, but those expectations exceed current screenshot evidence.
7. Existing coverage audit documents disagree in baseline commit/run references and have stale 137-file counts. Update them only after this new map is accepted as the current source of truth.
8. No runtime visual inspection can be claimed from source reads or Paparazzi alone. Desktop/Android runtime and accessibility checks need their own CI/manual evidence.

## 7. Execution sequence

1. Finish read-only call-site inventory for theme mode, palette providers, raw legacy token references and all visible interactive states.
2. Implement P0 theme contract first, with compatibility aliases and unit tests.
3. Migrate shared primitives and feature token aliases (P1) in one controlled batch.
4. Migrate screen surfaces and state visuals by feature; preserve all non-visual logic.
5. Add screenshot coverage for missing state/theme/layout cells. Review intentional golden diffs; never blanket-accept.
6. Run one CI workflow at a time. Wait for completion before creating another commit.
7. After all required checks are green, remove only obsolete compatibility aliases, temporary bootstrap markers and superseded audit artifacts after verifying no references remain.
8. Re-run targeted tests/CI after cleanup. Do not merge PR #11, release or deploy without separate approval.

## 8. Audit verdict

**Audit status: implementation map established; source migration not yet started.**

This document is the durable reference for where each change belongs and what must be preserved. It supersedes stale estimates only where its baseline and explicit inventory counts are newer. Phase A1's route-level composition map remains the authoritative route ownership reference.

No source/runtime behavior, navigation, persistence, provider, MCP, sandbox, terminal, permission, clipboard or URI behavior was changed during this audit.
