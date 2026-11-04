# DORAI AI — UI Upgrade Plan 4: Tools and Sandbox Workspace

## Objective

Create a coherent operational workspace for tools, MCP servers, skills, files, packages, and terminal sessions. It should make power features approachable without hiding their status or risks.

## Current scope

- `ToolsContent` combines MCP servers, Skills, and native tools in one long settings section.
- `SandboxTabsContent` supports setup, terminal sessions, files, and packages.
- Sandbox state is already supplied through `SandboxUiState`; session and tab state are managed by their view models where available.

The redesign should keep tool configuration separate from sandbox operation, while linking them through obvious status and setup actions.

## Target experience

- Treat Tools as a management page: users connect, enable, refresh, and review capabilities.
- Treat Sandbox as an operational workspace: users run commands, browse files, manage packages, and inspect active sessions.
- Provide an at-a-glance capability summary: enabled tools, connected MCP servers, installed skills, sandbox readiness, and active sessions.
- Clearly separate safe routine actions from destructive or environment-changing actions.
- Use status badges for Connected, Disabled, Installing, Needs setup, Error, and Active.

## Information architecture

```text
Tools and workspace
├── Tools overview
├── MCP servers
│   ├── Connected servers
│   ├── Popular servers
│   └── Add server
├── Skills
│   ├── Installed skills
│   └── Discover and install
├── Native tools
└── Sandbox workspace
    ├── Overview and setup
    ├── Terminal sessions
    ├── Files
    └── Packages
```

## Target structure

```text
ToolsScreen
├── ToolsOverview
├── McpServerManager
├── SkillsManager
└── NativeToolsGrid

SandboxWorkspace
├── SandboxHeader
├── SandboxReadinessCard
├── SandboxNavigation
├── SessionBar
└── SandboxContent
    ├── TerminalPane
    ├── FileBrowserPane
    └── PackageManagerPane
```

## State ownership

- Preserve `SettingsUiState`/`SettingsActions` for tool, MCP, and skill mutations.
- Preserve `SandboxUiState`, `SandboxSessionViewModel`, file-browser state, and packages state for operational sandbox information.
- Keep UI-only state local: filters, expanded details, selected compact subtab, and confirmation dialog visibility.
- Do not let cards query controllers or mutate servers directly; all actions flow through existing state holders.

## Implementation phases

### Phase 1 — Shared operational components

1. Create status badges, connection cards, capability count cards, row overflow actions, and confirmation patterns.
2. Standardize working, failed, disabled, and installed states across MCP, skills, tools, and sandbox setup.
3. Add a dedicated empty state for each collection instead of a generic blank list.

### Phase 2 — Tools management page

1. Add a Tools Overview header that summarizes enabled native tools, connected MCP servers, and installed skills.
2. Redesign MCP servers as cards with connection status, refresh time/status, enable switch, detail view, and remove action.
3. Present Popular Servers as a separate discovery area, not mixed with active connections.
4. Redesign Skills as installed and discoverable groups with installation progress and actionable error states.
5. Keep Native Tools as a responsive card grid, but add grouping and search/filter when the collection is large.

### Phase 3 — Sandbox setup

1. Turn the current setup card into a readiness flow with environment explanation, storage/session expectations, selected distribution, and install progress.
2. Make cancellation, retry, reset, and package installation status clear without burying error text.
3. Link skills that require a sandbox to the setup flow using a clear prerequisite state.

### Phase 4 — Sandbox workspace

1. Give Terminal, Files, and Packages a common workspace header and shared navigation.
2. On desktop, show session selection above or beside the terminal and keep Files/Packages available as supporting panes.
3. On compact layouts, retain the tab model with larger targets and selected-tab visibility.
4. Present terminal sessions as named, stateful tabs with active/running status and close action.
5. Keep terminal, file, and package controls independently scrollable and keyboard accessible.

### Phase 5 — Accessibility, safety, and validation

1. Give every stateful control an accessible name and state description.
2. Require an explicit confirmation UI before reset, removal, or other irreversible environment actions.
3. Validate touch targets, focus flow, large fonts, terminal contrast, and keyboard navigation.
4. Add screenshot cases for disconnected, connected, installing, error, sandbox setup, multiple sessions, files, and packages.

## Acceptance criteria

- Users can see the difference between a tool that is available, enabled, connected, installing, or failed.
- MCP, skills, and native tools are manageable without entering the sandbox workspace.
- Sandbox setup and operation are understandable at a glance on compact and desktop screens.
- Existing controller/view-model ownership stays intact.
- Destructive actions have explicit confirmation and existing undo behavior remains where supported.

## Risks and controls

- MCP and skill installation may be asynchronous. Every mutation must render a durable in-progress or error state from the view model.
- Sandbox availability is platform-dependent. Keep capability flags in state and avoid platform checks inside shared cards.
- Terminal performance and focus can regress if nested scroll or gesture handlers are added carelessly; keep the terminal pane isolated from surrounding scroll containers.

## Progress status

All five phases are implemented, each compiled against the desktop target and committed separately.

### Phase 1 — Shared operational components (done)

- New `ui/components/OperationalComponents.kt`: `OperationalStatus` vocabulary (Connected / Connecting / Disabled / Installing / NeedsSetup / Error / Active / Ready), a shared status color ramp, `StatusBadge` (dot + text so status never rides on color alone), `CapabilityCountCard`, and `CollectionEmptyState`.
- MCP server cards show a text status badge (including `Disabled` and `Not checked`, not just the four connection colors) and a dedicated empty state.
- Skills show a `Needs setup` badge on the sandbox prerequisite plus an empty-state card; native tools use the empty-state card too.

### Phase 2 — Tools management page (done)

- Tools Overview header: count cards for enabled native tools (`X / Y`), connected MCP servers (`X / Y`), and installed skills, each with an attention/needs-setup status badge. Responsive 3/2/1-column layout.
- Native tools gain a search/filter field when the collection has 8+ items; localized display names are resolved first so localized names match. No-match shows a dedicated empty state.
- Popular servers remain a separate discovery area (inside the add-server dialog, never mixed with active connections). Skills keep the installed/discoverable split with progress and error banners from Plan 2.

### Phase 3 — Sandbox setup (done)

- The setup card is a readiness flow: distribution name comes from state (fixing a stale hard-coded "Alpine Linux"), a Needs setup / Installing / Failed status badge, the distro's own storage-expectation copy, a session-persistence line, progress with cancel, and an error that is never the only content on screen.
- After a failure the primary action becomes "Retry setup" instead of repeating the original invitation.
- Skills already link to the sandbox setup flow via the Phase 1 prerequisite badge + button.

### Phase 4 — Sandbox workspace (done)

- Common workspace header (semantic `heading()` + `Ready` status badge) shared by Terminal, Files, and Packages.
- Subtab selector: previously every tab was primary-colored, making the active tab invisible; now only the selected tab is primary + semibold, with `Role.Tab` and `selected` semantics.
- Session chips get `Role.Tab`/`selected` semantics and a larger (8dp vertical) touch target.
- Session selection stays above the terminal on all sizes (existing model); each pane keeps its own scroll container.

### Phase 5 — Accessibility, safety, and validation (done, with notes)

- Skill uninstall now requires explicit confirmation via `SettingsConfirmDialog`, matching file delete and package uninstall (which already confirmed). MCP server removal and service deletion keep their Plan 2 confirmations.
- Stateful controls in the workspace carry accessible names/state: tab selectors and session chips expose `Role.Tab` + selected; file/package action icons have either a content description or are decorative next to labeled text.
- Deferred / out of scope: a per-session close action (the session view model has no close operation — adding one changes behavior, not just UI), screenshot automation, and a full keyboard-navigation audit pass. These stay open as follow-ups.
