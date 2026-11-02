# Brox Kai 2.0 — Premium AI Agent Workspace UI (2027 Direction)

**Document type:** Product/UI migration plan  
**Status:** Core 2027 UI foundation is present in `main`; responsive, accessibility, reduced-motion, and visual-regression refinements are in progress.  
**Platforms:** Android, Desktop, Web via Kotlin Multiplatform + Compose

## 1. Product vision

Brox Kai 2.0 should feel like a premium AI Agent Workspace: chat-first, calm, responsive, and capable of presenting interactive work products inline. The interface combines a compact Agent HUD, multimodal composer, safe generative micro-canvases, visible execution activity, and a dockable coding/terminal workspace.

The design borrows interaction patterns from modern AI products without copying their implementation. The app remains Compose Multiplatform and preserves its existing runtime and platform boundaries.

## 2. Non-negotiable product decisions

- **Brand:** “Brox Kai 2.0” remains the product name.
- **Primary destination:** Chat remains the home experience.
- **Navigation:** history/sidebar and Settings remain accessible without a crowded tab strip in the header.
- **Dashboard:** preserve its existing route/entry point; do not remove features as part of the visual refresh.
- **Interactive UI:** remains part of the same chat and uses the existing kai-ui renderer.
- **Code editor:** keep the unified editor/workspace flow; mobile may use a full-screen route, expanded desktop may use a split panel.
- **Runtime integrity:** provider calls, persistence, MCP, sandbox execution, and tool permissions remain outside composables.
- **Capability honesty:** no fake mic, camera, attachment, terminal, latency, or token-usage controls.
- **Privacy:** show observable agent activity, not hidden chain-of-thought.

## 3. Current-state baseline

Previously inspected repository documentation describes:

- An adaptive `ChatShell` with a conversation rail and optional side panel on expanded windows.
- A compact header with history/new-chat/workspace/speech/settings controls.
- A fixed bottom `QuestionInput` with attachment handling, skill autocomplete, service selection, and send/stop behavior.
- Assistant response actions such as copy, retry, share, and speech.
- A JSON-based `kai-ui` renderer for interactive layouts in chat.
- A unified editor workspace with file tree, editable code view, and terminal tab.
- Settings categories covering General, Agent, Services, Tools, Integrations, Sandbox, Data, and Account.

This is a baseline from existing docs, not a guarantee that every item remains unchanged in the current branch. Re-audit source and tests before implementation.

## 4. Visual direction

### 4.1 Design language

- Deep Obsidian/OLED foundation with layered charcoal surfaces.
- Restrained cyan for primary actions and active controls.
- Electric violet for agent/tool activity, used sparingly.
- Fine borders and subtle depth; avoid excessive blur and neon.
- Typography optimized for readability and platform availability.
- JetBrains Mono or another bundled monospace font for terminal/editor content after licensing and packaging checks.
- Motion should be responsive, short, and optional under reduced-motion settings.

### 4.2 Initial tokens

| Token | Initial value |
|---|---|
| `color.background` | `#090A0F` |
| `color.surface` | `rgba(255,255,255,0.04)` |
| `color.surfaceElevated` | `#171A23` |
| `color.borderSubtle` | `rgba(255,255,255,0.08)` |
| `color.accentPrimary` | `#00D7E8` |
| `color.accentAgent` | `#8A63F6` |
| `color.terminalBackground` | `#0B0D11` |
| `shape.control` | 12–16dp |
| `shape.card` | 18–24dp |

These are starting tokens. Validate contrast and theme semantics before locking them. Do not use raw values directly in feature composables.

## 5. Screen and component specifications

### Screen 1 — Home / empty chat

- Centered Brox Kai identity/orb with a subtle ambient animation.
- Clear welcome headline and concise helper text.
- Horizontal quick-action chips instead of oversized fixed cards.
- Chips are contextual and state-aware: Resume RPG, Ask a question, Run sandbox check, Daily reminders.
- The composer remains visible and reachable.
- Animation supports static/reduced-motion rendering.

### Screen 2 — Conversation

- Compact user bubbles and aligned assistant response column.
- Avoid large nested message containers and card-within-card styling.
- Preserve markdown, code, image, table, and attachment rendering.
- Add a compact response action bar: Copy, Retry/Regenerate, Share, Speak, and feedback where implemented.
- Keep streaming/cancel status separate from final answer content.
- Scrolling must not jump while the user is reading earlier messages.

### Screen 3 — Generative micro-canvas

Use kai-ui to render context-specific interactive work products.

**RPG/game widget**
- Compact stats and animated HP/progress indicators.
- Responsive artwork and readable narrative.
- Action buttons use typed callbacks.
- State remains owned by the conversation or game feature, not transient composable state.

**Recipe/workflow widget**
- Scrollable checklist with explicit completion state.
- Clear step hierarchy and ingredient quantities.
- Accessible alternative to visual progress.

**Data widget**
- Schema-validated tables/charts and filter controls.
- Explicit units and labels.
- Text/table fallback and bounded data sizes.

### Screen 4 — Agent activity

- Collapsed activity summary above/near the assistant response.
- Expanded chronological list of actual MCP/native/sandbox operations.
- Show tool name, concise sanitized summary, status, duration if measured, and bounded result preview.
- Support waiting-for-approval, running, success, failure, and cancellation.
- Never show private chain-of-thought or raw hidden reasoning.
- Never expose secrets, credentials, or unrestricted command output.

### Screen 5 — Universal Action Bar

- Plus menu for supported camera/gallery/file actions.
- Multiline text field with draft preservation.
- Slash command picker for registered skills/MCP tools.
- Voice input only where a working speech-to-text path exists.
- Clear Send and Stop states.
- Attachment tray with preview and remove controls.
- Respect IME/keyboard insets, focus order, and desktop shortcuts.

### Screen 6 — Terminal and code workspace

- Compact devices: docked bottom sheet or full-screen workspace.
- Expanded desktop: resizable split pane beside chat.
- Terminal surface: `#0B0D11`, readable monospace, selection/copy, syntax highlighting where available.
- Tool-to-terminal handoff must be explicit and limited to approved/safe commands.
- Existing sandbox process, filesystem, and permission boundaries remain authoritative.
- Do not imply identical shell support on Android, Desktop, and Web.

### Screen 7 — Settings and services

- Compact horizontal category strip with scroll-to-selected behavior.
- Expanded rail/sidebar navigation.
- Slim service cards with provider/model, status, and diagnostics.
- Latency and token counts only when the runtime provides real measurements.
- Consistent toggles, labels, helper text, and error states.
- Avoid duplicate or overflowing navigation pills.

## 6. Architecture mapping

The app is Compose Multiplatform. Use these as logical boundaries and adapt to the existing source tree:

| UI responsibility | Suggested Compose boundary |
|---|---|
| Dynamic header | `AgentHud`, `AgentStatusIndicator` |
| Agent execution trace | `AgentActivityDisclosure` |
| Interactive response blocks | Existing `KaiUiRenderer` and typed node model |
| Composer | Existing `QuestionInput` evolved into `UniversalComposer` |
| Attachments/slash actions | Composer subcomponents/sheets |
| Terminal/editor | Existing workspace/editor and sandbox UI |
| Glass surfaces | Shared theme/component primitives |
| Runtime state | Existing ViewModel, state, actions, and runtime event contracts |

Avoid creating parallel React-style `src/components` or a second AgentRuntime provider. Extend the existing Kotlin architecture.

## 7. Phase roadmap

### Phase 1 — Foundation and theming

**Deliverables**
- Semantic color, surface, border, radius, spacing, and typography tokens.
- OLED, dark, and light theme validation.
- Shared glass surface, status pill, icon button, and quick chip components.
- Reduced-motion and blur fallback behavior.
- Screenshot fixtures and contrast checks.

**Exit criteria**
- No hard-coded palette drift in migrated components.
- Theme tests and baseline screenshots pass.
- Large text and accessibility contrast are acceptable.

### Phase 2 — Core shell and action bar

**Deliverables**
- Compact Agent HUD replacing the vertical AI Agent badge.
- Adaptive header actions and overflow behavior.
- Empty-state quick chips and ambient orb.
- Capability-aware universal composer.
- Draft/attachment persistence through sheet and layout transitions.

**Exit criteria**
- No header or tab overflow on compact screens.
- Keyboard does not cover composer or latest message.
- Every visible action is connected to a real capability.

### Phase 3 — Generative UI and activity trace

**Deliverables**
- Versioned/validated kai-ui schema evolution with backward compatibility.
- Component registry improvements where justified by current renderer.
- RPG, recipe, and data micro-canvas patterns.
- Runtime-backed activity trace with safe summaries.
- Consistent response action bar.

**Exit criteria**
- Old saved kai-ui messages still render.
- Invalid/untrusted model output cannot execute arbitrary code or privileged actions.
- Activity events reflect real runtime operations and redact secrets.

### Phase 4 — Terminal and settings polish

**Deliverables**
- Dockable terminal/workspace presentation.
- Explicit safe tool-to-terminal handoff.
- Compact settings category strip and expanded rail.
- Service status cards using real runtime diagnostics.
- Motion and interaction polish.

**Exit criteria**
- Terminal process ownership and sandbox permissions are unchanged.
- Settings navigation works at compact and expanded widths.
- Unsupported platform capabilities are clearly hidden or disabled with explanation.

### Phase 5 — Regression and release readiness

**Deliverables**
- Unit, serialization, screenshot, and UI regression tests.
- Android/Desktop/Web builds for supported targets.
- Accessibility and performance checks.
- Documentation updates and reviewable pull request.

**Exit criteria**
- Existing provider, chat, MCP, sandbox, interactive UI, and editor workflows remain functional.
- No critical crash, layout overflow, or secret exposure.
- All required CI checks pass.

## 8. Testing matrix

| Area | Required scenarios |
|---|---|
| Header | Idle, streaming, tool-running, waiting, error; compact and expanded |
| Composer | Empty, draft, multiline, attachments, slash menu, send, stop, keyboard |
| Timeline | Long response, code, table, image, streaming, manual scroll, retry |
| Activity | Concurrent tools, approval, success, failure, cancellation, redaction |
| kai-ui | Old schema, new schema, invalid JSON, oversized tree, historical read-only |
| Workspace | Open/close dock, resize split, terminal unavailable, process cancellation |
| Settings | All categories, selected-tab visibility, large font, narrow width |
| Themes | Light, dark, OLED, contrast, reduced motion |
| Platforms | Android, Desktop, Web targets currently supported by the repository |

## 9. Risks and mitigations

- **Large chat screen refactor:** extract one boundary at a time and preserve behavior with screenshot tests.
- **Compose platform differences:** keep platform APIs behind existing source-set abstractions.
- **Blur/rendering cost:** use blur sparingly and retain opaque/translucent fallbacks.
- **Agent activity privacy:** sanitize at the runtime boundary, not only in UI.
- **Generative UI attack surface:** strict schema allowlist, size limits, typed actions, and no dynamic code execution.
- **Runtime regressions:** no provider, persistence, MCP, or sandbox business logic migration during visual-only changes.
- **False capability claims:** hide controls when the platform/runtime does not provide the feature.

## 10. Rollback strategy

- Keep each phase independently reviewable.
- Avoid changing persisted data formats unless backward-compatible defaults and migration tests exist.
- Gate new UI affordances behind existing settings or a feature flag where appropriate.
- Retain the previous component path until screenshot and interaction parity is demonstrated.
- Revert presentation changes without reverting provider/runtime fixes.

## 11. Definition of done

A phase is complete only when its implementation, tests, screenshots, and documentation are reviewed. A visual mockup or successful compile alone is not sufficient. No phase is considered released until its required CI checks pass and the owner explicitly approves the release path.
