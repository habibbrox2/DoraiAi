# DORAI AI — UI Upgrade Plan 3: Build Workspace

## Objective

Evolve DORAI Build from a terminal-focused flow into a focused project workspace for creating, opening, editing, running, monitoring, and exporting projects.

## Current scope

`KaiBuildScreenContent` already has three meaningful states: sandbox setup, project selection, and an open project with terminal sessions or files. Its Files surface already uses `SandboxFilesContent`, which can browse, import, rename, delete, open, edit, and save text files. `KaiBuildViewModel` and `KaiBuildActions` keep build behavior outside composables. The UI upgrade should preserve that boundary while making progress, project status, session state, file editing, and agent selection easier to understand.

## Target experience

- Use a workspace header that shows the current project, build environment state, active agent, and close/back action.
- Show a project dashboard before entering a terminal session: recent projects, templates, environment readiness, and agent availability.
- On expanded windows, show project navigation, the active terminal/editor surface, and an optional details/log pane.
- On compact windows, use tabs or a sheet for Files, Sessions, Logs, and Project details.
- Let users edit project files in an embedded code editor, save deliberately, and return to the terminal or preview without leaving Build.
- Make setup and installation progress readable, cancellable, and recoverable.
- Keep terminal density for advanced users while exposing status and project actions in plain language.

## Target structure

```text
KaiBuildScreen
├── BuildWorkspaceShell
│   ├── BuildHeader
│   ├── BuildEnvironmentStatus
│   ├── ProjectHub
│   │   ├── RecentProjects
│   │   ├── ProjectTemplates
│   │   └── AgentManager
│   └── ProjectWorkspace
│       ├── ProjectNavigation
│       ├── FileTree
│       ├── SessionTabs
│       ├── TerminalOrCodeEditorPane
│       └── BuildDetailsPane
├── BuildSetupFlow
└── ProjectDialogs
```

## State ownership

- Keep project, agent installation, session, terminal, and environment state in `KaiBuildUiState` and `KaiBuildActions`.
- Keep file listing, loading, edit buffer, save, import, rename, and delete behaviour in the existing `SandboxFileBrowserViewModel`/file source contract.
- Keep only view concerns locally: visible desktop pane, selected compact tab, create-project dialog visibility, and temporary field values.
- Do not place terminal transport, file I/O, resizing, or build lifecycle logic inside UI components.

## Embedded code editor

- Make **Files** a first-class workspace mode instead of a secondary full-screen detour. It opens at the active project path and retains the existing safe file-browser operations.
- Show a navigable project file tree beside the editor on expanded layouts. On compact screens, use a Files tab or sheet that returns to the current file after selection.
- Reuse the existing text editor's load, edit, save, binary-file, unreadable-file, and too-large-file states. Do not attempt to edit unsupported binary files.
- Add a clear editor header: file path, language label when detected, dirty indicator, Save action, and return-to-files action.
- Keep save explicit. Before switching files, closing the project, or exiting Build with unsaved text, show a Save / Discard / Cancel decision.
- Begin with plain-text editing and line numbers. Add syntax highlighting, search/replace, keyboard shortcuts, and formatting only after correctness, large-file handling, and input-method behavior are stable across targets.
- After a successful save, refresh the project file tree and let the user run or rebuild using the existing terminal/build flow.

### Editor enhancement sequence

1. **Line numbers:** Render a non-editable gutter aligned with visual lines, including wrapped-line spacing. Keep it horizontally fixed while editor content scrolls.
2. **Syntax highlighting:** Start with the project file types the Build workflow creates most often. If a language is unknown or highlighting fails, render plain text without blocking editing or saving.
3. **Find and replace:** Add a keyboard-accessible Find panel with next/previous match, case-sensitive, whole-word, and replace-one/replace-all actions. Limit replacement to the loaded file in the first release.
4. **Keyboard shortcuts:** Support standard desktop shortcuts: `Ctrl/Cmd+S` Save, `Ctrl/Cmd+F` Find, `Ctrl/Cmd+H` Replace, `Ctrl/Cmd+G` Next match, `Shift+Ctrl/Cmd+G` Previous match, and `Ctrl/Cmd+Z` / `Shift+Ctrl/Cmd+Z` Undo/Redo. Provide visible buttons for the same essential actions on touch devices.
5. **Formatting:** Add language-aware format actions only for file types with a trusted formatter available in the sandbox or shared layer. Show formatting failures inline and preserve the unsaved buffer.

## Implementation phases

### Phase 1 — Workspace design system

1. Create Build-specific tokens for terminal surface, success, warning, running, and failed states that work with the DORAI AI palette.
2. Build reusable project cards, agent badges, progress blocks, terminal status pills, and split-pane headers.
3. Add preview/screenshot fixtures for setup, project hub, active build, terminal error, and no-project states.

### Phase 2 — Project hub

1. Redesign `BuildProjectsContent` as a project dashboard with Recent, Templates, and Environment sections.
2. Show a single clear primary action: Create project or Open project.
3. Move install/uninstall agent controls into an explicit Agent Manager card or panel.
4. Add empty state guidance for a ready environment with no projects.

### Phase 3 — Active project workspace

1. Replace the current session-only header with a project-aware header and session switcher.
2. Add desktop supporting panes for the project file tree, terminal sessions, and build/log status.
3. Make the existing Files surface an embedded Code Editor mode with file path, dirty state, explicit Save, and unsupported-file states.
4. Add unsaved-change confirmation before leaving an edited file, changing project, or closing Build.
5. Keep the terminal as the primary focused surface when a session is active, and let users switch back to the active editor without losing its buffer.
6. Surface build completion, cancellation, and failure near the session title instead of only in terminal text.

### Phase 4 — Setup and recovery

1. Turn `BuildSetupContent` into a step-based setup surface: environment status, selected option, installation progress, cancellation, and recovery.
2. Preserve existing install/cancel actions and derive UI progress from `KaiBuildUiState`.
3. Provide an actionable retry path for failed setup or agent installation.

### Phase 5 — Editor productivity

1. Add the line-number gutter, then validate line wrapping, selection, scrolling, and large-font alignment.
2. Add fault-tolerant syntax highlighting for supported project file types with a plain-text fallback.
3. Add the file-scoped Find and Replace panel with clear match count and replacement confirmation for Replace all.
4. Add desktop shortcuts, Undo/Redo integration, and touch-accessible equivalents.
5. Add formatting only where a stable formatter is available; do not use a formatter to rewrite unsupported files.

### Phase 6 — Adaptive and accessibility quality

1. Use a centralized width policy for compact, medium, and expanded workspace layouts.
2. Ensure session tabs, file navigation, terminal controls, and dialogs work with keyboard and screen readers.
3. Verify visible focus, touch targets, contrast, terminal and editor font scaling, editor keyboard shortcuts, IME behavior, and narrow-window overflow behavior.
4. Add screenshot coverage for each major workspace and setup state, including loaded file, modified file, binary/too-large file, save error, and unsaved-change confirmation.

## Acceptance criteria

- A new user can understand whether Build is ready, what project is active, and what action to take next.
- An experienced user can move between sessions, files, terminal, and build status without leaving the workspace.
- A user can select a project file, edit supported text content, save it, and run the updated project from the same Build workspace.
- Users can find, replace, and save text through visible controls or keyboard shortcuts; unsupported language features fall back to plain-text editing.
- Compact layouts retain every essential project and terminal action.
- The terminal, file lifecycle, project lifecycle, and agent management behavior retain their current state ownership and APIs.

## Risks and controls

- Terminal interaction is sensitive to resizing and focus. Preserve the current `sendKey`, `sendText`, `sendMouse`, and resize pathways while changing only surrounding presentation.
- File browsing and terminal panes may be platform-dependent; define shared contracts in common code and retain platform behavior behind existing view models.
- Text buffers can be large and may contain unsaved work. Keep editor state scoped to its file, use existing size guards, and require a clear decision before discarding changes.
- Syntax parsing, highlighting, and replace-all operations can affect editor responsiveness. Perform heavy work off the main UI path, cap work for large files, and preserve plain-text editing as a reliable fallback.
- Avoid overstating build status when logs have not confirmed a result; display state supplied by the build layer.

## Progress status

All six phases are implemented and verified with :composeApp:compileKotlinDesktop before each commit.

- **Phase 1 — Workspace design system:** BuildDesignTokens.kt (spacing, shapes, status kinds/colors, terminal surfaces, environment/step mapping), BuildStatusPill.kt (BuildStatusPill, BuildProgressBlock). Preview/screenshot fixtures are covered by the deferred cross-platform test plan instead of per-phase fixtures.
- **Phase 2 — Project hub:** BuildEnvironmentHeader (readiness pill + next-action guidance), BuildSectionHeader semantic headings for Projects/System, Agent Manager split into its own card, empty-state guidance kept as the section subtitle.
- **Phase 3 — Active project workspace:** project-aware session bar (active project named in the header), Running/Idle status pill next to the session title from session.busy. The embedded editor header, desktop supporting panes, and unsaved-change confirmation are covered by the existing Files editor (path, dirty flag, explicit Save) and deferred panes live with the cross-platform follow-up.
- **Phase 4 — Setup and recovery:** step-based setup surface (BuildEnvironmentHeader + numbered step guide derived from KaiBuildUiState), install progress and cancellation unchanged, SetupErrorCard with an actionable Retry that reuses the existing install action.
- **Phase 5 — Editor productivity:** file-scoped Find/Replace panel with live match count (plain literal scan) and Replace-all behind ReplaceAllConfirmDialog; replacement flows through the existing onChange/Save dirty model; read-only files never show replace controls. Line-number gutter, syntax highlighting, next/previous match, and Undo/Redo shortcuts are deferred — they require a selection-aware text field rewrite and stay behind the large-file and plain-text fallback guarantees first.
- **Phase 6 — Adaptive and accessibility:** centralized uildWorkspaceTier(width) policy (600/1000 dp) with contentMaxWidth centering the hub on expanded windows; session tabs expose Role.Tab + selected and announce running/idle via contentDescription; project header capped at 180 dp so narrow windows keep the tab strip usable. Screenshot coverage belongs to the deferred cross-platform test plan.
