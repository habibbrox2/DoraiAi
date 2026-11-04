# DORAI AI — UI Upgrade Plan 2: Settings

## Objective

Turn Settings into a clear control center for the DORAI AI workspace. The interface should make common actions quick, while keeping services, agent behavior, integrations, tools, skills, and sandbox configuration easy to find without overwhelming the user.

## Current scope

`SettingsScreenContent` already receives immutable `SettingsUiState` and `SettingsActions`, supports General, Agent, Services, Integrations, Tools, and conditional Sandbox tabs, and uses snackbar-based undo for destructive actions. The main improvement needed is information hierarchy: a single horizontally scrolling tab strip and long vertical sections make desktop settings feel like a phone screen stretched wider.

## Target experience

- Present a concise settings overview first: active service, selected model, tool availability, connected integrations, and sandbox status.
- On expanded layouts, use a persistent left navigation rail with grouped categories and a detail pane.
- On compact layouts, retain a scrollable category selector or navigation sheet.
- Use a shared page heading with title, contextual summary, status indicator, and primary action when useful.
- Group long settings into cards with a clear purpose, current value, and single visible action.
- Use consistent empty, loading, error, connected, and attention-needed states.

## Information architecture

```text
Settings
├── Overview
├── Assistant
│   ├── General
│   ├── Agent behavior
│   └── Memory and scheduled work
├── Models and services
├── Connections
│   ├── Integrations
│   ├── Tools and MCP servers
│   └── Skills
└── Workspace
    └── Sandbox
```

The existing tab values can stay as the state model in the first migration. The new grouping is a presentation concern, so it does not require changing persisted preferences.

## Component structure

```text
SettingsScreen
├── SettingsShell
│   ├── SettingsHeader
│   ├── SettingsNavigation
│   ├── SettingsOverview
│   └── SettingsDetailPane
│       ├── SettingsSectionHeader
│       ├── SettingsCard
│       ├── SettingsStatusRow
│       └── SettingsEmptyState
└── SettingsFeedback
    ├── UndoSnackbar
    └── ConfirmationDialog
```

Keep state and mutations in `SettingsViewModel` and `SettingsActions`. Local UI state should be limited to navigation presentation, expanded-card state, search input, and dialog visibility.

## Implementation phases

### Phase 1 — Settings foundations

1. Establish page header, section header, status chip, key-value row, and action-card components.
2. Define common card spacing, icons, divider behavior, and Cyan/Electric Blue active states.
3. Add preview fixtures for connected, empty, loading, and error states.

### Phase 2 — Adaptive navigation

1. Extract the current selector into `SettingsNavigation`.
2. Use a left navigation rail on expanded windows and the existing horizontal selector or a modal sheet on compact windows.
3. Keep selection in `SettingsUiState.currentTab`; do not duplicate selected-tab state in child composables.
4. Add a Settings Overview surface that summarizes the user’s configuration before detailed sections.

### Phase 3 — Section redesign

1. General and Agent: use concise preference rows with explanatory secondary text.
2. Services: show the active service/model first, then configured services, health, and management actions.
3. Integrations: use connected/disconnected cards with clear next actions.
4. Tools, MCP, and Skills: show counts, connection state, and grouped actions before detailed lists.
5. Sandbox: show install state, selected distribution, storage status, and recovery actions in a dedicated workspace card.

### Phase 4 — Feedback and safety

1. Use confirmation dialogs for destructive actions and preserve the existing undo workflow.
2. Make progress, installation, sync, and error state explicit inside the affected card.
3. Keep in-progress actions disabled only within their relevant card, leaving unrelated settings usable.

### Phase 5 — Accessibility and validation

1. Add semantic headings and content descriptions for icon-only controls.
2. Verify tab/rail keyboard navigation and focus order.
3. Validate compact, tablet, and expanded desktop layouts at large font scale.
4. Add screenshot cases for overview, services, integrations, tools, sandbox setup, and undo feedback.

## Acceptance criteria

- Users can reach any settings section in one intentional navigation action.
- Desktop settings use available width for navigation and detail instead of stretching a mobile tab strip.
- Every card explains its purpose, current state, and available action.
- Existing settings actions, undo behavior, and conditional Sandbox visibility continue to work.
- Settings visual states are testable without live service calls.

## Risks and controls

- Some settings screens include platform-dependent features. Keep common presentation in `commonMain` and inject platform capabilities through UI state.
- Preserve existing tab identifiers until any migration is explicitly tested against saved preferences.
- Do not let the overview become a second source of state; derive all summaries from `SettingsUiState`.
