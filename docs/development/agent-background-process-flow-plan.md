# Agent Background Process / Cognitive Load Flow — Audit & Implementation Plan

**Status:** Read-only audit + implementation plan (no code changes yet)
**Last updated:** 2026-11-10
**Scope:** DORAI AI chat agent activity / execution trace / "thought process" UI
**Reference flow:** OpenCode-style cloud agent trace described by the user (duration header, ran commands, sandbox creation with ready state, command batch, git/version checks, directory update, final summary with commit hash/branch/CI).

> Note: the attached screenshot could not be rendered by this model (no image input). The audit and plan below are derived from the user's textual description of that flow plus the current repository state.

---

## 1. Read-only audit — current implementation

### 1.1 Two parallel timeline implementations

The repository ships **two** independent chat timeline stacks. Only one is active in production.

| Stack | Entry | Status | Uses |
|---|---|---|---|
| **UI v2 (active)** | `App.kt` → `DoraiChatScreenConnected` → `DoraiChatScreen` | Active | `ToolEventGrouper.groupHistoryToChatMessages`, `ChatMessageItem`, `DoraiAccordionBlock`, `WorkedForIndicator`, `DoraiReasoningBlock`, `DoraiToolCard` |
| **Legacy** | `ChatScreen.kt` (deprecated) | Superseded | `ChatTimeline`, `ActivityMapper`, `AgentActivityDisclosure`, `AgentActivityTimeline` |

Evidence:
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ChatScreen.kt:140` marks the legacy entry as superseded by `DoraiChatScreenConnected`.
- `AgentActivityTimeline` is defined in `AgentActivityTimeline.kt` but has **zero call sites** (dead code).
- `AgentActivityDisclosure` is only reachable through the legacy `ChatTimeline` path.
- `ActivityMapper` is only consumed by `ChatTimeline`.

### 1.2 Active path data flow (UI v2)

```
History (in-memory)
  → ToolEventGrouper.groupHistoryToChatMessages(history, …)
      → ChatMessageItem (per assistant turn)
          .toolExecutions: List<ToolExecutionModel>   (consolidated by toolCallId)
          .reasoningSegments: List<String>
          .agentWorkedDurationMs: Long?  (wall-clock span, ≥ 2 entries with sentAt)
  → DoraiChatScreen renders, per message:
      1. AssistantWhoRow
      2. WorkedForIndicator (if duration ≥ 2s)
      3. DoraiReasoningBlock (thought process, collapsed when complete)
      4. attentionTools (PendingApproval / Failed) — always visible, outside accordion
      5. DoraiAccordionBlock titled "Ran N tools/commands" (background tools)
      6. Final Markdown answer (always below processing blocks)
      7. Streaming caret, plan card, dynamic UI
```

This already mirrors most of the reference flow (duration header, thought process, ran-N block, final answer). Gaps are listed in §1.4.

### 1.3 Legacy path data flow

```
History
  → ActivityMapper.historyToActivities(history)  (reconstructs AgentActivity list by toolCallId)
  → AgentActivityDisclosure (collapsible, "Agent activity · N steps · …")
      → AgentActivityRow per activity (status dot, title, duration, expandable details)
```

Separate model: `AgentActivity`, `ActivityType`, `ActivityStatus`, `ActivityDetails`, `AgentRunSummary`, `summarizeAgentRun`, `formatAgentRunDuration`. `ActivityType` declares `AGENT_STARTED`, `THOUGHT`, `AGENT_PROGRESS`, `AGENT_COMPLETED`, `AGENT_FAILED`, `AGENT_CANCELLED` — **none are ever emitted**. Only `TOOL_CALL`, `SHELL_COMMAND`, `FILE_*`, `SKILL_*`, `PERMISSION_*` are produced.

### 1.4 Gaps vs. reference flow

1. **No environment/sandbox-creation step.** The reference shows "Cloud sandbox create → ready:true". The active UI has no step representing sandbox/environment setup; shell tools just run. `SandboxController` exists but is not surfaced in the trace.
2. **No git/version step grouping.** Git commands (commit, push, `gh --version`) are rendered as generic shell tools; no dedicated title/summary or commit-hash/branch/CI final card. The final answer renders the agent's prose, but there is no structured "final summary" affordance.
3. **No step-level status for setup.** Sandbox creation success ("ready") is not a tracked activity status.
4. **Duplicate/dead code.** `AgentActivityTimeline`, `AgentActivityDisclosure`, `ActivityMapper`, `AgentActivity` model, `AgentRunSummary` duplicate the active path's responsibilities, increasing cognitive load and maintenance risk. They are also the only consumers of several string resources.
5. **Two correlation strategies.** Active path correlates by `toolCallId` inside `ToolEventGrouper`; legacy path correlates by `toolCallId` with name-based fallback inside `ActivityMapper`. Divergent behavior for older history.
6. **Reasoning vs. activity conflation risk.** Policy (`docs/features/reasoning.md`) forbids exposing private chain-of-thought; the UI labels it "Thought process"/reasoning. The reference's "Thought process" is the model's reasoning summary. Must keep the distinction; any new step labels must describe observable work only.
7. **Redaction coverage.** Active path `DoraiToolCard`/`safeActivityPreview` redacts secrets in legacy path; need to verify the active path's tool card applies equivalent redaction (audit item).
8. **Tests.** `ActivityMapperTest`, `AgentReviewModelsTest` cover legacy path; active path (`DoraiChatScreen`, `ToolEventGrouper`) has screenshot coverage but limited unit tests for grouping/accordion. No tests for sandbox/git steps (none exist yet).

### 1.5 What already matches (do not regress)

- Duration header (`WorkedForIndicator`, ≥ 2s, format `Xm Ys`).
- Thought-process block (`DoraiReasoningBlock`), collapsed when complete, expanded while streaming.
- "Ran N commands/tools" accordion (`DoraiAccordionBlock`), collapsed by default, ≥ 48dp touch target, button-role semantics.
- Attention tools (approval/failed) stay outside the accordion.
- Final Markdown answer always below processing blocks.
- Secret redaction helpers exist (`safeActivityPreview`).
- Accessibility: live region, reduced motion, font-scale snapshot mentioned in `chat.md`.

---

## 2. Desired flow mapping (reference → implementation)

| Reference step | Current equivalent | Action |
|---|---|---|
| `Worked for 2m 32s` | `WorkedForIndicator` | Keep; ensure duration uses real wall-clock span (already `agentWorkedDurationMs`). |
| `Ran commands` / `ran tools` (initial) | Reasoning + first tool batch | Keep; could add an explicit "Started" step if desired (optional). |
| `Cloud sandbox create` → `"ready":true` | **None** | Add sandbox-creation activity step with status. |
| `Ran 6 commands` | `DoraiAccordionBlock` "Ran N …" | Keep; optionally split into numbered steps with per-command status. |
| `Cloud git` / `gh --version` | Generic shell tool card | Add git-aware title + version output preview. |
| Directory update / confirm changes | Generic shell tool card | Add working-directory + changed-files summary. |
| Final summary (commit hash, branch, GitHub Actions) | Final Markdown answer | Keep as Markdown; optionally add a structured "Result" card parsed from tool output. |

---

## 3. Implementation plan (phased)

### Phase 0 — Preparation (no behavior change)

- Confirm active screen is `DoraiChatScreen` (done). Document that `ChatTimeline`/`AgentActivityDisclosure` are legacy.
- Add a feature flag or keep legacy path untouched initially; all changes target UI v2.
- Baseline: run `:screenshotTests:verifyPaparazziDebug` and `:composeApp:desktopTest` to capture current golden state.

### Phase 1 — Unify & remove dead code (cleanup, low risk)

1. Delete unused `AgentActivityTimeline.kt` (zero call sites) OR wire it into tests; prefer delete after confirming no references (grep done: none).
2. Decide fate of legacy stack:
   - Option A: Keep `AgentActivityDisclosure`/`ActivityMapper` as fallback for legacy `ChatScreen` (deprecated) and mark `@Deprecated`.
   - Option B: Remove legacy stack entirely (larger; requires updating `ChatScreen.kt` and screenshot tests).
   - **Recommended:** Option A first; removal in a follow-up PR after screenshot baseline update.
3. Mark `ActivityType` aspirational values (`AGENT_STARTED`, etc.) as `@Deprecated` or remove if Option B.
4. Update `docs/features/chat.md` "Key Files" if files removed.

### Phase 2 — Sandbox creation step (new observable step)

1. Introduce a lightweight activity-event representation in the active path (not the legacy `AgentActivity` model). Candidate: extend `ToolExecutionModel` or add a sealed `AgentStep` to `ChatMessageItem`:
   - `AgentStep.SandboxSetup(status: Running|Ready|Failed, environment: String?)`
   - `AgentStep.CommandBatch(count, commands)`
   - `AgentStep.GitOperation(kind, summary, result)`
2. Emit `SandboxSetup` when a shell tool runs against a fresh sandbox session (hook in `SandboxController` / tool executor), updating status to `Ready` on success.
3. Render the step in `DoraiChatScreen` between reasoning and the command accordion, with a status dot + "ready" label, matching the reference.
4. Add redaction + bounded preview (reuse `safeActivityPreview`).
5. Tests: unit for step emission; screenshot for new layout.

### Phase 3 — Git-aware steps & final summary card

1. Detect git tool calls by command content (`git `, `gh `) in `ToolEventGrouper`/`DoraiToolCard` title logic; set title "Git operation" / "GitHub CLI" instead of generic shell.
2. Parse commit hash, branch, and CI status from tool stdout into a structured `GitResult` (bounded, redacted).
3. Render a compact "Result" card (commit hash, branch, GitHub Actions status) above/within the final answer when present; otherwise fall back to Markdown (current behavior).
4. Keep final Markdown answer below (no regression).

### Phase 4 — Granular numbered steps (optional, higher risk)

- Split the single "Ran N commands" accordion into an ordered, collapsed-by-default list of numbered steps, each with status dot + duration, matching reference's top-to-bottom flow.
- Requires changes to `ToolExecutionModel` (add `sequence`, `durationMs`) and `ToolEventGrouper` (per-tool timing).
- Keep accordion as fallback for ≥ N tools to avoid visual noise.

### Phase 5 — Validation

- Unit tests: `ToolEventGrouper` grouping, sandbox step emission, git parsing, redaction.
- Screenshot tests: update goldens for any layout change (`recordPaparazziDebug` with explicit `--tests`).
- Accessibility: TalkBack labels, reduced motion, 1.4× font scale.
- `spotlessCheck` green before commit.
- Bengali policy: any new user-facing strings added to all locale files (values + values-bn + others) with parity.

### Phase 6 — Documentation

- Update `docs/features/chat.md` (timeline rendering, key files).
- Update `docs/features/reasoning.md` implementation map if new step model added.
- Update `CHANGELOG.md` (user-facing UI change).

---

## 4. Risk register

| Risk | Mitigation |
|---|---|
| Breaking legacy screenshot tests | Keep legacy path untouched in Phases 1–3; only delete dead code with zero references. |
| Secret leakage in new git/commit cards | Reuse `safeActivityPreview` redaction; add regression tests with fake tokens. |
| Conflating reasoning with activity (policy) | Label steps as observable work ("Ran command", "Sandbox setup"); never expose model internals. |
| Performance (parsing git output) | Bound previews (existing 500/1200 char caps). |
| Cross-platform (desktop/Android/iOS/wasm) | Changes in `commonMain` only; platform sandbox hooks behind `expect`/`actual` if needed. |
| Bengali parity | Add strings to all locale resources; run parity checks. |

---

## 5. Files likely to change (implementation, later)

- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/DoraiChatScreen.kt` (rendering steps)
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ToolEventGrouper.kt` (step emission, git detection)
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/ToolExecutionModel.kt` (if step fields added)
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/DoraiToolCard.kt` (titles/previews)
- `composeApp/src/commonMain/kotlin/com/dorai/ai/sandbox/...` (sandbox ready hook) — verify exact file during implementation
- `composeApp/src/commonMain/composeResources/values*/strings.xml` (new labels)
- `docs/features/chat.md`, `docs/features/reasoning.md`, `CHANGELOG.md`

Files to remove (Phase 1, after verification):
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/AgentActivityTimeline.kt` (dead)
- Possibly legacy stack (deferred).

---

## 6. Open questions for the user

1. Should sandbox-creation and git steps be **always shown** when applicable, or only when the agent actually performs them? (Recommendation: only when performed.)
2. Is a structured final "Result" card (commit hash/branch/CI) desired, or is the agent's Markdown summary sufficient? (Reference shows Markdown; recommend keeping Markdown as primary, card as enhancement.)
3. Granularity: keep the single "Ran N commands" accordion (current) or expand to numbered per-command steps (Phase 4)? (Recommendation: keep accordion for ≤ 8 tools; add expand-to-steps as an opt-in "Show details".)
4. Should the legacy `AgentActivityDisclosure` path be removed now or kept for the deprecated `ChatScreen`? (Recommendation: keep until `ChatScreen` is deleted.)

---

## 7. Audit commands run

- `git status --short --branch` → `feature/optional-ios-validation-2.1.27...origin/...`
- `grep` (composeApp) for `AgentActivityTimeline(` → 0 matches (dead code confirmed).
- `grep` for `AGENT_STARTED|THOUGHT|AGENT_COMPLETED` → only enum declarations; never emitted.
- `grep` for `AgentActivityDisclosure|AgentActivityTimeline|activityTimeline` → legacy path only.
- Read: `AgentActivity.kt`, `AgentActivityTimeline.kt`, `AgentActivityDisclosure.kt`, `AgentReviewComponents.kt`, `ActivityMapper.kt`, `AgentReviewModels.kt`, `ChatTimeline.kt`, `BotMessage.kt`, `ToolEventGrouper.kt`, `DoraiChatScreen.kt` (relevant sections), `docs/features/chat.md`, `docs/features/reasoning.md`.
- Image input: not supported by this model; reference flow taken from user's textual description.