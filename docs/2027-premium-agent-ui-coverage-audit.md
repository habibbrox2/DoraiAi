# 2027 Premium Agent UI — Migration Coverage Audit

**Audit baseline:** `feature/2027-premium-agent-ui-migration`  
**Audit scope:** `composeApp/src/commonMain/kotlin/com/dorai/ai/ui`
**Inventory size:** 137 Kotlin source files  
**Method:** source inventory + navigation mapping + targeted legacy-style/state review. This is not a claim that every runtime state has been visually inspected.

## Phase A — UI inventory

| Area | Kotlin files | Coverage notes |
|---|---:|---|
| App theme and shared roots | 4 | Theme, app theme, platform URI/clipboard helpers |
| Dashboard | 2 | Dashboard screen and models |
| Chat | 40 | Screen/shell, composer, timeline, HUD, tool activity, review, history, selectors |
| Shared components | 15 | Dialogs, badges, chips, search, status, loading, settings rows |
| Settings | 31 | General, Agent, Services, Tools, Integrations, MCP, Skills, Sandbox, Data/Account navigation, sheets |
| DORAI Build | 11 | Build shell, setup, projects, terminal, session and status components |
| Sandbox | 9 | File browser, packages, find/replace, tabs and status |
| Dynamic UI | 6 | Schema/node model, parser, renderer, actions and TTS |
| Markdown | 16 | Parser, renderer, code, diff, inline and math |
| Other | 3 | Icons, maps and remaining UI utilities |
| **Total** | **137** | Source inventory |

### Navigation entry points

- `Home` → `ChatScreen` (application start destination)
- `Dashboard` → `DashboardScreen`
- `Settings` → `SettingsScreen`
- `Build` → `KaiBuildScreen`

The Sandbox is reached through supported chat/workspace flows rather than a separate top-level route.

## Phase B — Legacy-style review and changes

### Completed in this pass

- `ToolExecutionCard.kt`: migrated the general tool-call card from a separate hardcoded palette to shared premium tokens for surface, border, typography, cyan activity accent and semantic success/error states. Shell terminal chrome remains intentionally terminal-styled.
- `ToolConfirmationCard.kt`: migrated approval emphasis and action colors to semantic design tokens; changed the four approval actions from one horizontal row to two balanced rows to avoid compact-width overflow; applied shared card radius.

### Intentionally retained

- ANSI terminal palette and terminal canvas colors in terminal-specific components.
- Terminal window control dots.
- Semantic success, warning, error and diff colors.
- Model Arena score bands in `ModelSelectionSheet.kt`, pending a separate semantic-score token decision.

### Remaining Phase B review

- Visually compare `ModelSelectionSheet` score/error states against the 2027 surface and semantic color system.
- Review `BuildTerminalContent`, `TerminalSheet` and shell output at compact and expanded sizes; preserve ANSI output fidelity.
- Review `ToolExecutionCard` and `ToolConfirmationCard` screenshots after the latest changes.

## Phase C — UI state coverage

| Component / flow | States that need explicit regression coverage |
|---|---|
| Tool execution | Running, completed, failed, expanded/collapsed, long output, copy feedback |
| Tool approval | Deny, allow once, allow session, always allow; compact and expanded widths |
| Model selector | Loading, empty, provider error, retry, score bands, selected model |
| Settings | Every category, selected tab visibility, large font, narrow width |
| Composer | Empty, draft, multiline, attachment, slash/skill menu, sending, stop |
| Agent activity | Waiting approval, running, success, failure, cancellation, redacted preview |
| Sandbox | Empty, loading, package install, install error, file selection, find/replace confirmation |
| Build workspace | Setup, project list, active session, terminal resize, unavailable capability |
| Dynamic UI | Valid schema, legacy schema, invalid payload, oversized tree, interactive actions |
| Themes/platforms | Light, dark, OLED, reduced motion; Android/Desktop/Web supported targets |

## Verification status

- The previously reported CI run `41420878139` passed all five jobs before this audit's new changes.
- The 44 existing Paparazzi golden baselines do not establish coverage for every screen or state.
- A fresh CI run is required after the current changes; it has not been started by this audit.
- No merge, release or deployment is included in this work.

## Remaining acceptance criteria

1. Add or extend Paparazzi tests for tool execution and approval states.
2. Add explicit screenshot coverage for model selector loading/error/retry and compact settings navigation.
3. Verify compact and expanded screenshots for chat, workspace, settings and sandbox.
4. Run formatting, unit tests, screenshot regression, Android and Desktop checks.
5. Re-audit all 137 source files and close every unresolved row before declaring full migration coverage.

**Current verdict:** primary navigation and most major surfaces use the 2027 design direction. Phase A inventory is established; Phase B has targeted fixes; Phase C state coverage remains incomplete. Do not mark application-wide migration as complete yet.
