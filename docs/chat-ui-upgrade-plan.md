# Brox Kai 2.0 — Chat UI Upgrade Plan

**Status:** Core chat workspace migration is present; responsive HUD and accessibility regression work is in progress.  
**Design direction:** 2027 AI Agent Workspace  
**Target platforms:** Android, Desktop, Web (Compose Multiplatform)

## 1. Objective

Evolve the existing chat-first interface into a responsive AI Agent Workspace with a compact agent HUD, a multimodal composer, readable execution activity, schema-driven generative UI, and a dockable coding workspace.

The redesign must preserve existing provider integrations, conversation persistence, ChatViewModel/ChatActions ownership, MCP and native tool execution, sandbox boundaries, attachments, speech output, interactive mode, and Kai Build/editor workflows.

This is a presentation and interaction migration first. Backend capabilities must be integrated only where the corresponding runtime contract already exists or is separately implemented and tested. Never ship decorative controls that imply unavailable functionality.

## 2. Existing architecture and constraints

The current UI is Kotlin Multiplatform + Compose, not a React application. The shared implementation lives under:

`composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/ui/chat/`

Known responsibilities:

- `ChatScreen.kt`: connects chat state/actions and coordinates timeline, composer, overlays, interactive mode, and sandbox visibility.
- `ChatUiState`: immutable presentation of conversation, loading, service, file, and notification state.
- `ChatViewModel` and `ChatActions`: own business actions and provider/runtime interactions.
- `TopBar`, `QuestionInput`, `BotMessage`, `UserMessage`, `EmptyState`, and `ChatTimeline`: existing presentation boundaries.
- `ChatShell`: adaptive layout for compact and expanded windows.
- `kai-ui`: existing JSON-based interactive UI format and Compose renderer.
- Existing screenshot tests cover multiple chat states and device/window sizes.

Before implementation, re-check these paths on the selected base commit. Treat this section as an architecture map, not permission to move business logic into composables.

## 3. Experience principles

1. **Chat-first:** the conversation remains the primary workspace and route.
2. **Agent-aware, not noisy:** status is visible at a glance; detailed activity is available on demand.
3. **Readable over decorative:** content hierarchy, contrast, and predictable scrolling take priority over blur and glow.
4. **Progressive disclosure:** advanced controls live in compact menus, sheets, or docked panels.
5. **Responsive by design:** compact phones, tablets, desktop, and web use one shared adaptive policy.
6. **Capability honesty:** show voice, file, MCP, sandbox, and terminal actions only when available in the active platform/runtime.
7. **Safe generative UI:** model output is untrusted data; render only supported, validated component schemas.
8. **Accessible motion:** every animation has a reduced-motion/static alternative.

## 4. Visual system

### Palette and surfaces

Initial design tokens (subject to contrast and screenshot validation):

| Token | Value | Usage |
|---|---|---|
| `background` | `#090A0F` | OLED/deep workspace background |
| `surface` | `rgba(255,255,255,0.04)` | Subtle elevated surface |
| `surfaceElevated` | `#171A23` | Cards, sheets, panels |
| `borderSubtle` | `rgba(255,255,255,0.08)` | Glass surface outline |
| `accentCyan` | `#00D7E8` | Primary action and active state |
| `accentViolet` | `#8A63F6` | Agent/tool activity |
| `success` | semantic green token | Connected/completed |
| `warning` | semantic amber token | Waiting/attention |
| `danger` | semantic red token | Failure/destructive action |

Use semantic theme tokens rather than scattering literal colors. Light theme remains supported. OLED black is an explicit theme/surface choice, not a forced replacement for all dark surfaces.

### Shape, typography, and motion

- Compact controls: 12–16dp radius; cards and sheets: 18–24dp radius.
- Use the existing bundled/platform-safe typography first; introduce Plus Jakarta Sans/Inter Display or JetBrains Mono only after licensing, packaging, and cross-platform fallback are checked.
- Terminal output uses a monospace typeface.
- Motion uses short, interruptible transitions; avoid perpetual animation except a restrained active-state pulse.
- Glass effects must degrade gracefully where blur is unsupported or expensive. Prefer translucent surfaces and borders over a blur dependency.

## 5. Core screen specifications

### 5.1 Agent HUD header

Replace the oversized floating header and vertical “AI Agent” badge with a compact adaptive HUD.

- **Leading:** history/sidebar toggle and compact Brox Kai brand.
- **Center:** current agent state: Idle, Thinking, Running tool, Waiting, Connected, or Error.
- **Trailing:** available workspace/terminal toggle and Settings.
- Use a small dot/orb plus concise status text; never use a tall vertical badge.
- Tool status may show a short safe label such as “Calling Context7” or “Running command”.
- On narrow screens, collapse secondary actions into an overflow menu.
- The header must not consume the status-bar/system-inset area or overlap content.

### 5.2 Home / empty conversation

- Keep the Brox Kai identity mark as the visual anchor.
- Use a concise welcome headline and a compact, horizontally scrollable row of contextual quick-action chips.
- Suggested chips: Resume RPG, Ask a question, Run sandbox check, Daily reminders. Render only actions backed by current state/capabilities.
- Avoid large fixed quick-action cards that push the composer below the fold.
- Keep the composer visible above the keyboard and system navigation area.

### 5.3 Conversation timeline

- Keep user messages compact and right-aligned.
- Render assistant content in a readable, width-constrained response column; avoid nested oversized cards around every markdown block.
- Dynamic UI blocks may own their own visual surface, but must align to the same content column and avoid horizontal overflow.
- Separate answer content from status/activity rows.
- Preserve streaming, cancellation, retry, copy, share, and speech actions where supported.
- Add a consistent message action bar with touch-friendly hit targets.
- Preserve scroll anchoring: streaming updates should not unexpectedly pull the user away when they have scrolled upward.
- Long code, tables, images, and dynamic widgets must be independently scrollable only when necessary.

### 5.4 Agent activity / execution trace

Provide a collapsible, user-visible activity disclosure associated with the relevant assistant turn.

Examples:
- “Used Context7”
- “Called MCP: filesystem”
- “Ran sandbox command”
- “Waiting for tool result”
- “Tool completed” / “Tool failed”

Each activity item may expose sanitized arguments, duration, status, and bounded output where the runtime permits. Redact secrets, credentials, private file contents, and sensitive command arguments. Do not expose hidden chain-of-thought or private internal reasoning. A provider-supplied reasoning summary, if any, must be treated as a separate optional data type and shown only when the provider contract and product policy allow it; it is not a substitute for execution logs.

### 5.5 Universal action composer

The bottom composer becomes a compact capsule with capability-aware controls:

- **Add (+):** camera, gallery, and file picker where supported.
- **Draft field:** multiline text, clear focus and selection behavior.
- **Slash (/):** searchable commands, skills, and MCP tool shortcuts that the active runtime actually exposes.
- **Voice:** show only when a working speech-to-text/voice input path is available on the current target.
- **Send/Stop:** mutually exclusive, explicit states.
- Show attachment previews with remove/retry affordances.
- Preserve draft and selected attachments when opening/closing sheets or workspace panels.
- Desktop: Enter sends; Shift+Enter inserts a newline, unless user-configurable behavior already exists.
- Android: IME action and keyboard insets must be correct.

### 5.6 Terminal and workspace

- Provide a dockable bottom sheet on compact screens and a split panel on expanded screens.
- Keep terminal process execution and sandbox permissions in existing runtime/platform layers.
- Offer “Open in terminal” only for commands/scripts that are safe and available to hand off.
- Use deep OLED terminal surface, monospace text, selectable output, copy action, and syntax highlighting where already supported.
- Clearly distinguish shell availability, sandbox state, and execution permissions.
- Do not imply that Android, Web, and Desktop have identical shell capabilities.

### 5.7 Settings and service management

- Use compact horizontally scrollable category navigation on narrow screens and a rail/sidebar on expanded screens.
- Keep selected category visible and provide clear focus/selection states.
- Service cards should prioritize provider/model, connection status, and relevant diagnostics. Show latency and token usage only when measured by the runtime; never fabricate metrics.
- Toggles must use shared semantic components and accessible labels.
- Avoid adding a second navigation system that duplicates the app sidebar.

## 6. Compose component boundaries

Keep shared UI in `commonMain`. Suggested presentation boundaries (adapt to existing packages rather than duplicating them):

```text
ui/chat/
├── ChatScreen
├── ChatShell
├── agenthud/
│   ├── AgentHud
│   ├── AgentStatusIndicator
│   └── AgentActivityDisclosure
├── timeline/
│   ├── ChatTimeline
│   ├── UserMessage
│   ├── BotMessage
│   └── MessageActionRow
├── composer/
│   ├── UniversalComposer
│   ├── AttachmentTray
│   └── SlashCommandSheet
├── workspace/
│   ├── WorkspaceHost
│   ├── TerminalDock
│   └── EditorPanel
└── components/
    ├── GlassSurface
    ├── StatusPill
    └── QuickActionChip
```

These are logical boundaries, not a mandate to move every existing file. Prefer incremental extraction with no duplicate state owners.

## 7. State and runtime contracts

- `ChatViewModel` remains the owner of conversation and runtime actions.
- `ChatUiState` exposes immutable state required by the UI.
- Composables render state and invoke typed callbacks; they do not call providers, execute shell commands, or persist messages.
- Add UI-only state for expanded activity disclosure, active dock, composer menu, and local animation only when it cannot be derived from existing state.
- Tool activity must come from actual runtime events, not inferred from assistant prose.
- Keep execution event IDs stable so streaming updates update the same row instead of duplicating it.
- Keep event payloads bounded and redact secrets before they reach presentation or persistence layers.

## 8. Migration phases

### Phase 1 — Foundation and theme
1. Audit current theme tokens, component usage, and screenshot fixtures.
2. Add semantic OLED/glass tokens and reusable surface/status primitives.
3. Verify light, dark, and OLED themes, contrast, large font scale, and reduced motion.
4. Capture baseline and updated screenshots.

### Phase 2 — Core shell and composer
1. Redesign the top bar as adaptive Agent HUD.
2. Replace oversized empty-state cards with contextual quick chips.
3. Refactor composer into capability-aware attachment, slash, voice, draft, and send/stop regions.
4. Preserve draft, keyboard, and attachment behavior.

### Phase 3 — Generative UI and activity
1. Stabilize the existing kai-ui schema and renderer.
2. Add safe component registry/versioning and validation where needed.
3. Add runtime-backed activity trace disclosure.
4. Improve game/recipe/data widget layouts and response actions.

### Phase 4 — Terminal and settings
1. Add dock/bottom-sheet workspace presentation around existing terminal/editor capabilities.
2. Add safe command handoff.
3. Refine settings navigation and service diagnostics.
4. Test platform capability differences.

### Phase 5 — Quality and rollout
1. Run unit, serialization, screenshot, and UI regression tests.
2. Build Android, Desktop, and Web targets supported by the current project.
3. Validate accessibility, performance, process lifecycle, and keyboard/IME behavior.
4. Document known platform gaps and prepare a reviewable PR.

## 9. Acceptance criteria

- No vertical AI Agent badge or oversized legacy header remains.
- Header status is accurate and reflects runtime state.
- Composer controls are capability-backed and preserve draft/attachments.
- Timeline alignment is stable during streaming and manual scrolling.
- Activity trace reports actual tool events and never leaks secrets or hidden reasoning.
- Dynamic UI remains schema-validated and safe for untrusted model output.
- Terminal docking does not change sandbox permissions or process ownership.
- Settings work on compact and expanded layouts without tab overflow.
- Existing providers, persistence, MCP, sandbox, interactive mode, and editor behavior remain functional.
- Android/Desktop/Web builds and applicable tests pass.

## 10. Non-goals

- Replacing Compose with React/TypeScript.
- Rewriting provider APIs or conversation persistence as part of a visual phase.
- Executing arbitrary model-generated code in the UI process.
- Displaying private chain-of-thought as a user-facing “thought log”.
- Showing fake latency, token usage, voice, camera, or terminal controls.


## 12. IDE-grade file review and run summary

The activity disclosure renders a bounded side-by-side diff only when the tool payload contains both before and after snapshots. Patch-only payloads are shown as supplied; new-content-only payloads are explicitly labelled as having no original snapshot. Missing source data is called out instead of fabricating a diff.

Per-file review annotations support **Mark reviewed** and **Needs changes** for the current screen session. These annotations do not apply, reject, or revert filesystem changes. The current agent runtime exposes shell/tool execution and a platform-specific sandbox file browser, but no cross-platform file-patch approval/write-back contract. True Accept/Reject patch actions remain deferred until such a contract exists.

The Agent Run Summary derives only observable history: activity counts, completed/running/failed counts, distinct file paths, terminal command count, and elapsed time. Diff calculation is capped at 250 lines per side.
