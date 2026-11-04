# DORAI AI — UI Upgrade Plan 6: Home Dashboard

## Objective

Create a calm starting screen for DORAI AI that gives users immediate access to Chat, Build, Design, and Tools, plus recent work and service readiness. The dashboard should shorten the path to a useful action without replacing the focused chat workspace.

## Current scope

- The `Home` navigation route currently opens `ChatScreen` directly.
- Desktop navigation has only Chat and Settings. On mobile, settings is reached from the chat UI.
- DORAI Build opens as a transient surface from Chat on supported Android devices.
- Recent conversations, provider selection, service status, build projects, and tools already have separate state holders or screens.

The dashboard is therefore a new presentation and navigation layer. It should compose existing capabilities rather than recreate their business logic.

## Target experience

- On launch, users see a clear next step: start a chat, create a build project, open Design, or manage tools.
- Recent conversations and projects are visible without overwhelming the page.
- A small readiness summary shows the selected AI service and any actionable setup issue.
- The first screen remains fast, sparse, and useful whether the user has no history or many saved projects.
- Selecting a card opens the existing destination with the appropriate context; it does not create a parallel feature flow.

## Information architecture

```text
Home Dashboard
├── Welcome and service readiness
├── Primary actions
│   ├── Chat
│   ├── Build
│   ├── Design
│   └── Tools
├── Recent work
│   ├── Conversations
│   └── Build projects
├── Quick access
│   ├── Services
│   ├── Settings
│   └── Sandbox when available
└── Empty-state guidance
```

## Target structure

```text
HomeDashboardScreen
├── DashboardHeader
│   ├── Brand greeting
│   └── ServiceReadinessPill
├── PrimaryActionGrid
│   ├── ChatActionCard
│   ├── BuildActionCard
│   ├── DesignActionCard
│   └── ToolsActionCard
├── RecentWorkSection
│   ├── RecentConversationList
│   └── RecentProjectList
├── QuickAccessRow
└── DashboardEmptyState
```

## Navigation model

1. Keep Chat as its own focused workspace. Introduce a separate `Dashboard` route instead of making the existing `Home` route carry two incompatible responsibilities.
2. Make Dashboard the launch destination after the feature is complete; retain Chat as a first-class primary destination.
3. Add navigation destinations through a typed route and one central navigation coordinator. Dashboard cards dispatch navigation events rather than calling feature view models.
4. Route Build only where it is currently supported. Unsupported platforms show a disabled, explanatory card or omit Build according to the product capability policy.
5. Treat Design as a destination contract. Until a dedicated design workspace exists, its action may open the existing interactive creation flow with a Design-focused prompt or show an honest “coming soon” state; it must not claim a separate editor exists.
6. Open Tools through Settings with the Tools tab selected, and Services through Settings with the Services tab selected. The selected-tab action remains owned by `SettingsViewModel`.

## Dashboard content rules

### Primary actions

- Show Chat first, then Build, Design, and Tools in a stable order.
- Each card has one title, a short purpose statement, a recognisable icon, and optional readiness state.
- Use Cyan or Electric Blue only for the primary action and current focus. Keep other cards Midnight Navy with clear outlines and text contrast.
- If setup is needed, keep the main card actionable and surface the requirement in a small supporting label.

### Recent work

- Show up to four recent conversations and four recent build projects on wide layouts; show two of each on compact layouts.
- Each conversation row includes title, last activity, selected service/model when available, and a compact resume affordance.
- Each project row includes project name, last activity, and build or environment status when known.
- Place `View all` only when more saved items exist. It opens the existing history or project hub rather than loading an unbounded list into the dashboard.
- Do not show sensitive prompt previews by default. Use a generic title or the existing conversation title.

### Readiness and empty states

- If a usable service is configured, show its name and selected model in a compact readiness pill.
- If no service is configured, replace the pill with a clear `Set up a service` action that opens Services.
- With no saved work, retain the primary action grid and show a short starter suggestion beneath it.
- If Build, Sandbox, or Tools are unavailable, use capability state supplied by the platform/view model; do not infer availability in a composable.

## Responsive layout

| Width | Primary actions | Recent work | Navigation |
| --- | --- | --- | --- |
| Compact | Two-column cards, stacked sections | Two rows per collection | Bottom/top compact navigation |
| Medium | Two-column cards with balanced spacing | Two-column recent sections | Compact rail or top navigation |
| Expanded | Four action cards in one row | Conversations and projects side by side | Persistent sidebar or top navigation |

- Keep the dashboard as one vertically scrolling surface with section spacing rather than nested scrolling lists.
- Let primary action cards grow with text scale; never truncate the primary action label.
- Maintain an obvious keyboard focus order: header, primary actions, recent work, then quick access.

## State ownership

- Create `DashboardUiState` as a read-only aggregation of existing feature summaries: selected service, service readiness, recent conversation summaries, recent project summaries, and capability flags.
- Keep full chat history in `ChatViewModel`, full project lifecycle in `KaiBuildViewModel`, and settings mutations in `SettingsViewModel`.
- A dashboard coordinator/repository maps small, display-safe summaries from those sources. It contains no prompt content, credentials, terminal output, or mutable service configuration.
- Keep UI-only state local: temporary loading shimmer visibility, a dismissed welcome hint, and small-screen section expansion if needed.
- Dashboard clicks emit navigation and resume events through interfaces; cards do not access data stores or view models directly.

## Implementation phases

### Phase 1 — Foundation and routes

1. Add a typed Dashboard route and central navigation events for Chat, Build, Settings tabs, history, and projects.
2. Define `DashboardUiState` and display-safe recent-work summary models.
3. Add capability flags for Build, Sandbox, and Design destinations.

### Phase 2 — Dashboard shell

1. Build the header, service-readiness pill, primary action cards, and empty state using DORAI AI colors and typography.
2. Add responsive spacing and grid policy for compact, medium, and expanded layouts.
3. Connect Chat and Settings cards first, then gate platform-specific destinations from capability state.

### Phase 3 — Recent work

1. Map conversation and project data to display-safe dashboard summaries.
2. Add resume actions and `View all` routes to existing history and project-hub experiences.
3. Ensure opening a recent item preserves the selected conversation or project instead of creating a duplicate session.

### Phase 4 — Design and tools pathways

1. Define the Design destination contract and use the existing interactive flow only when it satisfies that contract.
2. Add Tools, Services, and Sandbox quick-access actions with selected settings tabs.
3. Make unavailable capabilities clear without presenting broken navigation.

### Phase 5 — Accessibility and validation

1. Give action cards, readiness status, recent-item actions, and unavailable states descriptive semantics.
2. Verify 48 dp targets, keyboard navigation, focus restoration after returning from a destination, and large-font layouts.
3. Add screenshot cases for first launch, configured service, no service, populated history, populated projects, loading summaries, and unavailable Build/Design states.

## Acceptance criteria

- The first screen lets a new user reach Chat, service setup, and available creation tools in one interaction.
- Returning users can resume recent conversations and projects without navigating through unrelated settings.
- Dashboard summaries do not duplicate provider, chat, or build business logic.
- Unsupported capabilities are represented truthfully and never lead to a dead end.
- Compact and desktop layouts preserve clear hierarchy, accessibility, and low visual clutter.

## Progress status

- [x] Phase 1 — Foundation and routes (Dashboard route + Build route, DashboardUiState + mapper, capability flags)
- [x] Phase 2 — Dashboard shell (header, readiness card, primary action grid responsive, Chat/Settings/Build gating)
- [x] Phase 3 — Recent work (conversations and projects, resume, View all)
- [x] Phase 4 — Design and tools pathways (Design prefill contract, Tools/Services via Settings tabs, unavailable capabilities honest)
- [x] Phase 5 — Accessibility and validation (basic semantics on cards; screenshot cases to be validated on device)

---

## Risks and controls

- Aggregating feature state can create startup delay. Render the action grid immediately and independently load recent-work summaries.
- Recent conversations may expose private content. Display only existing safe titles and metadata, never prompt excerpts by default.
- Design does not yet have a dedicated workspace. Keep the Design entry behind an explicit destination contract until the workflow exists.
- Route changes can break back-stack behavior. Add navigation tests for launch, returning from Chat, Settings, Build, and recent-item resume actions.
