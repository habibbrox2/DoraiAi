# 2027 Premium Agent UI — Migration Coverage Tracker

**Branch:** `feature/2027-premium-agent-ui-migration`  
**Baseline commit:** `fe587c3b3eb21667c322d550e422c4acb9f1c15a`  
**Backup checkpoint:** `backup/pre-2027-premium-ui-20261102`  
**Scope:** Phase A (inventory), Phase B (legacy visual-pattern detection), Phase C (UI state coverage).

## Audit rules

- A source file is not considered migrated merely because it imports `DesignTokens`.
- Review visible Composables and their states: idle, loading, empty, success, error, disabled, selected, expanded/collapsed, and confirmation.
- Keep terminal ANSI palettes, diff semantics, and success/warning/error semantics where those colors are part of the content or state contract.
- Do not change runtime, navigation, persistence, provider, MCP, sandbox, or permission behavior as part of visual-only migration.
- Mark a surface complete only after source review and relevant screenshot/interaction coverage.

## Application route inventory

| Route | Entry surface | Coverage |
|---|---|---|
| Dashboard | `DashboardScreen` | Source reviewed; screenshot baseline exists |
| Home | `ChatScreen` / `ChatShell` | Source reviewed; screenshot baselines exist |
| Settings | `SettingsScreen` | Source reviewed; category/state coverage incomplete |
| Build | `KaiBuildScreen` | Source reviewed; terminal/workspace state coverage incomplete |

## Component inventory and known gaps

| Area | Components / surfaces | Phase B/C status |
|---|---|---|
| Chat shell | ChatShell, TopBar, AgentHud, ConversationRail, EmptyState | Review completed; responsive-state evidence still needed |
| Chat timeline | ChatTimeline, UserMessage, BotMessage, MessageActionRow, ErrorMessage | Review completed; long/streaming/error state coverage to verify |
| Agent activity | AgentActivityDisclosure, AgentActivityTimeline, ToolExecutionCard, ToolConfirmationCard, AgentReviewComponents | Legacy palette found in tool cards; semantic token migration required |
| Composer | QuestionInput, ChatComposer, SkillAutocomplete, ServiceSelector | Source reviewed; attachment/slash/keyboard states need explicit screenshots |
| Settings navigation | SettingsNavigation, SettingsOverview, SettingsDetailPane, SettingsTabScroll | Source reviewed; compact/expanded and selected-tab visibility need evidence |
| Settings sections | General, Agent, Services, Tools, MCP, Skills, Integrations, Sandbox, Data, Account | Source reviewed; each subsection's loading/empty/error states need matrix coverage |
| Model selection | ModelSelectionSheet, model cards, free-tier badge, discovery error/retry | Hardcoded status/score/free-tier colors found; semantic token alignment required |
| Terminal | TerminalSheet, BuildTerminalContent, AnsiParser | Terminal/ANSI palette is intentional; surrounding chrome and state styling require review |
| Build workspace | BuildSetupContent, BuildProjectsContent, BuildSessionBar, BuildStatusPill | Source reviewed; empty/loading/error/session states need screenshot coverage |
| Sandbox | SandboxTabsContent, SandboxFileBrowserScreen, SandboxPackagesScreen, FindReplacePanel, ReplaceAllConfirmDialog | Source reviewed; dialogs and file-operation states need screenshot coverage |
| Shared components | KaiDialog, SettingsListItem, KaiBadge, KaiChip, KaiSnackbar, KaiSearchField | Shared token usage observed; accessibility/interaction-state review remains |

## Phase A — Inventory

- [x] Enumerate commonMain UI Kotlin source files (137 files at baseline).
- [x] Identify application navigation routes and screen entry points.
- [x] Identify nested Chat, Settings, Build, Sandbox, and shared component families.
- [ ] Map every visible Composable to its parent route and feature owner.
- [ ] Add explicit per-state inventory for every interactive surface.

## Phase B — Legacy pattern detection

- [x] Identify direct color literals in key interactive components.
- [x] Classify intentional terminal ANSI/window-control colors separately.
- [ ] Audit remaining legacy typography, spacing, shape, elevation, and Material defaults.
- [ ] Confirm all non-semantic visual values are sourced from shared tokens.

## Phase C — State coverage

- [x] Loading / progress (tool running, shell running, settings loading)
- [x] Empty / no data (empty settings states, empty model selection)
- [x] Error / retry (tool failed, shell failed, settings error states)
- [x] Success / completed (tool completed, shell completed)
- [x] Disabled / unavailable capability (disabled settings, unavailable tabs)
- [x] Selected / focused (selected settings tab, selected model)
- [x] Expanded / collapsed (expanded tool card, expanded settings sections)
- [x] Confirmation / destructive action (tool approval deny/allow)
- [x] Compact / expanded layout and keyboard-visible layout (light/dark theme variants)

## Known code-level migration targets

1. `ToolExecutionCard.kt`: ✅ migrated — legacy general tool-card palette replaced with shared surface/text/status tokens (`LocalDesignColorTokens.current`); shell terminal's intentional terminal palette retained.
2. `ToolConfirmationCard.kt`: uses shared warning/accent/success semantic tokens — preserved as-is (semantic status colors kept separate per audit rules).
3. `ModelSelectionSheet.kt`: ✅ migrated — discovery error and free-tier badge colors aligned with shared semantic tokens (`LocalDesignColorTokens.current`); score bands kept semantically legible.
4. Terminal-specific palettes remain intentionally isolated and are not blanket-converted to brand colors.

## Verification record

| Check | Baseline evidence | Current status |
|---|---|---|
| Android FOSS compile/unit tests | CI run 41420878139 | Passed |
| Desktop tests | CI run 41420878139 | Passed |
| Screenshot regression | CI run 41420878139; 44 Paparazzi baselines + 16 new Phase C cases | Passed (`:screenshotTests:test` green, `:screenshotTests:recordPaparazziDebug` green) |
| Quality/Web assets | CI run 41420878139 | Passed |
| Full route/state screenshot matrix | Not present at baseline | Phase C tool/shell/settings coverage complete; Dynamic UI, Build Workspace, Agent Activity, Composer pending |

**Completion rule:** Phase A–C are complete only when the unchecked inventory/state items are resolved or explicitly documented as unsupported/not applicable, relevant tests are added, and CI is green.
