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

- [ ] Loading / progress
- [ ] Empty / no data
- [ ] Error / retry
- [ ] Success / completed
- [ ] Disabled / unavailable capability
- [ ] Selected / focused
- [ ] Expanded / collapsed
- [ ] Confirmation / destructive action
- [ ] Compact / expanded layout and keyboard-visible layout

## Known code-level migration targets

1. `ToolExecutionCard.kt`: replace legacy general tool-card palette with shared surface/text/status tokens; retain the separate shell terminal's intentional terminal palette.
2. `ToolConfirmationCard.kt`: use shared warning/accent/success semantic tokens and premium card/surface styling; preserve approval action behavior.
3. `ModelSelectionSheet.kt`: align discovery error and free-tier badge colors with shared semantic tokens; keep score bands semantically legible.
4. Terminal-specific palettes remain intentionally isolated and are not blanket-converted to brand colors.

## Verification record

| Check | Baseline evidence | Current status |
|---|---|---|
| Android FOSS compile/unit tests | CI run 41420878139 | Passed |
| Desktop tests | CI run 41420878139 | Passed |
| Screenshot regression | CI run 41420878139; 44 Paparazzi baselines | Passed; not exhaustive state coverage |
| Quality/Web assets | CI run 41420878139 | Passed |
| Full route/state screenshot matrix | Not present at baseline | Pending |

**Completion rule:** Phase A–C are complete only when the unchecked inventory/state items are resolved or explicitly documented as unsupported/not applicable, relevant tests are added, and CI is green.
