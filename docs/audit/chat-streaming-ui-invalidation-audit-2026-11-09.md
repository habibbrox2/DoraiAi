# Chat Streaming & UI Invalidation Audit — 2026-11-09

**Reported symptom:** "The AI response is being generated, but the chat UI does not update with it. If you go to another screen and come back to Chat, the new response is visible. Response data and UI rendering appear to be out of sync — a state-observation / recomposition problem."

**Scope:** UI v2 chat only (`DoraiAppShell` → `DoraiChatScreenConnected` → `DoraiChatScreen`), plus the state pipeline that feeds it: `ChatViewModel.state` → `RemoteDataRepository.chatHistory` → `Requests` transport. Legacy `ChatScreen` / `ChatTimeline` / `BotMessage` are excluded except where they prove a contract the v2 screen silently dropped.

**Method:** read-only static audit of the whole write → observe → render path. No build or test run was possible in this environment (see §F).

**Verdict:** the recomposition/state-observation layer is **not** the primary defect. The chat has **no incremental response channel at all** — every provider call is a single blocking request/response, and the assistant message is written to history exactly once, after the whole turn completes. The UI therefore has nothing to render during generation, and whether the finished message becomes *visible* is decided by an auto-follow scroll heuristic rather than by state invalidation. A second, independent class of defects makes the rendered timeline and the persisted/restored timeline diverge.

Severity legend: **High** = directly produces the reported symptom; **Medium** = amplifies it / will block the fix; **Low** = polish or mismatch; **Info** = observation, no action implied.

---

## A. The pipeline, stage by stage

| Stage | What the code does | Streaming-capable? |
|---|---|---|
| Transport (`network/Requests.kt`) | `openAICompatibleChat` (:257), `geminiChat` (:200), `anthropicChat` (:507), `openAIResponses` (:304) each `POST` once and deserialize a single JSON body via `response.body()`. `OpenAICompatibleChatRequestDto` (dtos/openaicompatible/OpenAICompatibleChatRequestDto.kt:8) has **no `stream` field**. | **No** |
| Provider fan-out (`RemoteDataRepository.askInternal`, :1141) | Sequential fallback chain over `getOrderedFallbackEntries()` (:1225–1268); includes the keyless free tier and on-device engine. Each attempt is one blocking call. | **No** |
| History writes (`chatHistory`, `MutableStateFlow`) | `USER` row appended before the call (:1203–1218); optional compaction (:1218); **`ASSISTANT` row appended once, after the loop returns** (:1271–1281); `saveCurrentConversation()` (:1283). Tool rounds do emit per-iteration rows (:1525–1560). | **No** for text; yes for tool steps |
| ViewModel (`ChatViewModel.state`, :194–226) | `combine(_state, chatHistory, savedConversations, currentConversationId, hasUnreadHeartbeat)` → `distinctUntilChanged()` → `stateIn(WhileSubscribed(5s))`. `askInternal` (:247) sets `isLoading` once and clears it after the repository returns (:289). | Correct, but only ever sees whole messages |
| Screen (`DoraiChatScreenConnected.kt:97`) | `val uiState by viewModel.state.collectAsStateWithLifecycle()`; `messages` = `remember(uiState.history, …) { groupHistoryToChatMessages(...) }` (:112); `isStreaming = uiState.isLoading` (:250). | Correct |
| Timeline (`DoraiChatScreen.kt`) | `LazyColumn` + `itemsIndexed(messages, key = { _, msg -> msg.id })` (:1142); streaming caret gated on `isStreaming` (:1149); `"streaming-tail"` card gated on `isStreaming` (:1172); follow-the-tail `LaunchedEffect(messages.size, lastMessageTextLength)` (:952). | Renders only what history contains |

**Consequence:** for a plain (tool-less) chat turn, `chatHistory` changes exactly twice per turn — the user row, then the assistant row several seconds later. Everything the UI calls "streaming" is driven by a single boolean that flips at the *end* of the turn.

---

## B. Findings

### High

1. **No streaming transport, so no partial response can ever be observed.** Every provider path builds one request and reads one response body (`Requests.kt:257-300`, `:200-255`, `:507-560`, `:304-340`); the request DTOs carry no `stream`/SSE flag, and nothing in `RemoteDataRepository` consumes `text/event-stream` (only `mcp/McpClient.kt:126-192` does, for MCP — unrelated). `askInternal` appends the assistant `History` **once** at `RemoteDataRepository.kt:1271-1281`, after `askWithService` has fully returned. `KeylessFreeTierFallback` and the on-device `engine.chat(...)` (`:579-660`) are also whole-response. This is the architectural cause of "the response is being generated but the UI is not updating with it": there is no state to invalidate during generation.

   **Status (2026-11-09): fixed for the OpenAI-compatible provider family** — SSE transport plus in-place delta rendering, see §G. Gemini, Anthropic, the Responses API and on-device inference still return whole responses.

2. **The documented "streaming" contract does not exist in code.** `docs/features/chat.md:67-69` promises: "the timeline follows streaming token growth (not just new messages)", "a blinking trailing caret bar is shown under the reply text", and CHANGELOG:206 claims "eliminating recomposition thrashing during token streaming". The caret (`DoraiChatScreen.kt:2326`), the tail card (`:1172`), and the follow logic (`:948-956`, whose comment explicitly says "keyed on the last message's text length so token-by-token growth also keeps the list scrolled") are all dead weight: `lastMessageTextLength` only changes when a whole message is added, because `message.text` never grows incrementally. Users and the audit trail are being told a feature exists that cannot fire — this is why the defect reads as a regression rather than a missing feature.

   **Status (2026-11-09): the contract is now implemented where the transport exists** — the caret, the follow-the-tail effect and "Worked for Xs" all fire while deltas stream; `docs/features/chat.md` now states the provider boundary (§G).

3. **Whether the finished answer becomes visible is decided by a scroll heuristic, not by invalidation.** `DoraiChatScreen.kt:940-956`:

   ```kotlin
   val isAtBottom by remember { derivedStateOf { … lastVisible >= total - 2 } }
   LaunchedEffect(messages.size, lastMessageTextLength) {
       if (messages.isNotEmpty() && isAtBottom) listState.scrollToItem(messages.lastIndex)
   }
   ```

   The completed assistant row is appended **below the viewport** whenever `isAtBottom` was false at the moment of completion (user scrolled up a little, keyboard resize, reading a long previous answer, tail card having shifted the list). When the screen is re-entered, `ChatDetailPane` is recreated: `rememberLazyListState()` is fresh, the effect re-runs on first composition, `layoutInfo` is empty ⇒ `total == 0` ⇒ `isAtBottom == true` ⇒ `scrollToItem(messages.lastIndex)` — the timeline jumps to the newest row. That asymmetry is exactly the reported "go to another screen, come back, and now the new response is there". The message was always in `uiState.history`; it was simply never brought into view.

   *Confidence:* the code path is verified; the exact empty-`layoutInfo` timing on first composition should be confirmed on device with the diagnostic in §D. Even without that timing, the design is unsound: a state change that must be seen depends on a layout snapshot, and `scrollToItem` (not `animateScrollToItem`) fights user scrolling.

   **Status (2026-11-09): fixed** — durable pinned-tail state replaces the snapshot read and the newest item's end is targeted; see §G.1.

4. **`isLoading` is the only "generation in progress" signal and carries no stage information.** `ChatViewModel.askInternal:268-300` sets `isLoading = true`, awaits the whole chain (local-network permission → compaction summarization call → sequential provider fallbacks, each with retry/timeouts → tool loop rounds → persistence), then clears it. The v2 screen renders nothing about which stage it is in: `fallbackStatus` is populated by the repository on every fallback (`_fallbackStatus.value = FallbackStatus(...)`, `RemoteDataRepository.kt:1255`, `:1296`) and is collected into `ChatUiState.fallbackStatus` (`ChatViewModel.kt:148-150`), but **only the legacy `ChatTimeline.kt:153` renders it** — `DoraiChatScreenConnected` never forwards it. During a multi-provider failure chain (which can last a minute) the user sees one static thinking card and correctly concludes "the UI is stuck even though the AI is working".

### Medium

5. **Whole-state re-derivation runs on the main thread on every emission.** `ChatViewModel.state` (`:194-223`) recomputes, per emission of any of the five sources: sorting every saved conversation, localized `getString(...)` per conversation, `dataRepository.getInstalledSkills()`, `dataRepository.supportedFileExtensions()`, plus an immutable-list copy. It is subscribed on `viewModelScope` (Main.immediate) and there is no `flowOn`. `WorkspaceViewModel.kt:85-87` already uses `flowOn(Dispatchers.Default)` for the same pattern. Today this runs twice per turn; the moment real streaming lands (30–60 emissions/s) this becomes the bottleneck and re-introduces the jank the CHANGELOG claims to have fixed.

6. **The timeline is fully regrouped on every emission, and nothing is skippable.** `groupHistoryToChatMessages` (`ToolEventGrouper.kt:88`) re-walks the entire history, re-parses every tool output with regexes (`parseToolExecutionOutput`, `:23-46`) and rebuilds every `ChatMessageItem`; `deriveChatTimelineData` (`ChatTimelineData.kt:37`) does a second full pass. Both run inside `remember(uiState.history, …)` (`DoraiChatScreenConnected.kt:107-125`), i.e. on the composition thread, and `ChatMessageItem` (`DoraiChatScreen.kt:295-313`) is neither `@Immutable` nor an `ImmutableList` holder, so no row can be skipped. With token streaming this is O(history × rows) work per chunk. `MarkdownRenderer.kt:81` keys blocks as `key(index, block)` — correct but re-creates every block group on any content change.

7. **Two extra LLM round-trips lengthen the "spinner with no content" window.** `compactHistoryIfNeeded()` (`RemoteDataRepository.kt:1904`) is invoked *inside* the turn, before the real request (`:1218`), and performs a full summarization `askSilently(...)` when the history crosses the threshold; `generateFollowUpSuggestions()` (`ChatViewModel.kt:320-349`) performs another full silent request after the answer. The first one is invisible progress; the second is correctly after `isLoading = false` (`:289` vs `:294`) but still competes with the visible turn.

8. **Persisted state and on-screen state diverge on failure, cancel, and reload.** `saveCurrentConversation()` runs only after a successful turn (`RemoteDataRepository.kt:1283`); a failed/cancelled/backgrounded turn leaves the user's message visible but unpersisted, so it disappears on relaunch. On save, `trimToRecentExchanges(history, 20)` (`:1970`, documented in `docs/features/chat.md`) means a restored conversation can be shorter than what was on screen — reported by users as "the response I just got is gone / the chat changed after I re-entered".

### Low

9. **`isRestoring` is dead in UI v2.** `ChatViewModel.kt:128` documents that `ChatScreen` gated the interactive branch on `!isRestoring`; v2 never reads it (`grep isRestoring` → only `ChatUiState.kt:109`, `ChatViewModel.kt:141`, `DashboardModels.kt:86`). The composer is therefore interactive while the persisted conversation is still loading on a cold start, and the restored history then replaces an empty timeline under the user. The ID allocation at `askInternal:1148-1150` makes a true clobber unlikely, but the empty-chat flash is real.

10. **Out-of-band full-list replacement in compaction.** `compactHistoryIfNeeded` ends with `chatHistory.value = listOf(summaryEntry) + recentMessages` (`:1957`), where `recentMessages` is a `subList` **view** of the previous list (`:1923-1924`). The timeline has no transition for "history was rewritten"; rows silently disappear and the retained backing list keeps the old entries alive.

11. **`isAtBottom` is created in a bare `remember`** (`:940`) around a `derivedStateOf`, and is read inside `LaunchedEffect` (not observed in composition). It is a layout snapshot read at one instant, not a durable "user is pinned" signal — see finding 3.

    **Status (2026-11-09): removed** together with finding 3; the jump-to-latest button and the follow effect now read one `pinnedToTail` flag (§G.1).

### Info — verified correct (rule-outs)

12. **The observation chain itself is sound.** `App.kt:198` collects `chatViewModel.state` at the root of the composition, so `stateIn(WhileSubscribed(5_000))` always has a subscriber during in-app navigation; `DoraiChatScreenConnected.kt:97` collects the same flow again. `SharingStarted.WhileSubscribed(5s)` therefore cannot be the cause. `remember` keys in `DoraiChatScreenConnected.kt:107-125` include `uiState.history`, so a new history produces a new `messages` list. `LazyColumn` is keyed by the stable `History.id` (`:1142`). `History` (`ChatUiState.kt:123`) is an immutable data class, and every repository write uses `update { it.toMutableList().apply { add(...) } }`, so `MutableStateFlow` equality never swallows an addition. Writes from `backgroundDispatcher` are safe — `MutableStateFlow` is thread-safe and Compose snapshot invalidation is thread-agnostic.

13. **`chatHistory` has exactly one implementation and one writer.** The interface declares it once (`DataRepository.kt:20`); the only implementation is `RemoteDataRepository.kt:274` and every mutation lives in that file. No second flow or repository instance feeds the timeline.

---

## C. Root cause statement

The reported behaviour is the sum of two independent defects on the same user journey:

1. **No incremental generation channel (primary, architectural).** The turn is a single blocking request; the assistant message lands in `chatHistory` once, at the end. Nothing in the pipeline can invalidate the UI during generation, so the timeline is static by construction, and the "streaming" UI is a fake driven by `isLoading` (findings 1, 2, 4).
2. **Visibility of the completed message is a scroll decision, not an invalidation decision (primary, UI).** In-place completion only follows the tail when `isAtBottom` happened to be true; re-entering the screen recreates the list state and re-runs the follow effect, which lands on the last row. The data was always in state — which is why leaving and returning "fixes" it (finding 3).

Findings 5–10 are what will make the fix fail if they are not addressed: they either delay the first visible chunk (main-thread derivation, O(n) regrouping, extra LLM calls) or make the rendered and restored timelines disagree.

**Not the cause:** recomposition staleness, `collectAsStateWithLifecycle`, `WhileSubscribed`, `distinctUntilChanged`, immutable-list equality, keyed `LazyColumn` items, or multiple repository/ViewModel instances (finding 12).

---

## D. How to confirm on device (3 diagnostics, no code change)

1. **Prove the turn is single-shot:** log inside `askInternal` around `:1220` and `:1271` (`"history=${chatHistory.value.size} t=${now}"`). For a plain chat you will see exactly two timestamps seconds apart, with no events in between; if the request is long, the first assistant timestamp equals the completion time.
2. **Prove the message reached state before it was seen:** log at `DoraiChatScreenConnected.kt:112` (`"messages=${messages.size} loading=$isStreaming"`). At completion you should see the new size *while the previous answer is still what the user sees*.
3. **Prove the scroll is what changed:** log `listState.layoutInfo.totalItemsCount`, `visibleItemsInfo.lastOrNull()?.index`, `isAtBottom` inside `DoraiChatScreen.kt:952` on both paths. Expect `isAtBottom == false` for the in-place completion and `total == 0 → isAtBottom == true` (i.e. `scrollToItem(lastIndex)`) on first composition after re-entry.

---

## E. Remediation plan (ordered by impact)

1. **Add a real streaming path, then render deltas in place.** SSE for the OpenAI-compatible / Responses / Anthropic / Gemini adapters (`Requests.kt`), emitting `Flow<TextDelta>`; in `RemoteDataRepository`, create the assistant `History` row *before* the request and append deltas with `chatHistory.update { h -> h.map { if (it.id == turnId) it.copy(content = it.content + delta) else it } }`, throttled to one emission per frame (~16–50 ms). Keep the final `AssistantTurn` write as the reconciliation/authoritative copy. Gate on a capability flag per service so non-streaming providers keep today's behaviour. This is the only change that makes "real-time response rendering" true.
2. **Make follow-the-tail explicit state, not a layout snapshot.** Replace `remember { derivedStateOf { … } }` + `scrollToItem` with: a `userScrolledUp` flag derived from `snapshotFlow { listState.isScrollInProgress }` / `firstVisibleItemIndex`, `snapshotFlow` over `messages.lastOrNull()?.id` + text length, and `animateScrollToItem` only when the user is pinned. Do **not** scroll on first composition unless the messages actually changed since the last composition of this screen.
3. **Fix the state derivation off the main thread** (`flowOn(Dispatchers.Default)` on the combine, precomputed conversation summaries, `getInstalledSkills()` cached in a StateFlow) and make `ChatMessageItem` `@Immutable` with `ImmutableList` fields so rows can skip. Move `groupHistoryToChatMessages`/`deriveChatTimelineData` behind an incremental/derived structure (or at minimum key it on the last assistant id + size).
4. **Surface progress in v2:** forward `fallbackStatus` into `DoraiChatScreen` and render it with the existing `StatusPill` / activity accordion, and add a stage enum (`CONNECTING`, `THINKING`, `TOOL`, `FALLBACK`) to `ChatUiState` instead of one boolean.
5. **Persist progressively and honestly:** save after the user row, after each tool result, and (debounced) while streaming; do not silently trim to 20 exchanges between what is displayed and what is stored.
6. **Gate the v2 composer on `isRestoring`**, matching the legacy screen's documented behaviour.
7. **Until §E1 ships, correct the contract:** remove or gate the caret / "token growth" copy in `docs/features/chat.md:67-69` and the CHANGELOG claim, and stop passing `isStreaming = uiState.isLoading` as if it meant token streaming.

Suggested verification for the fix (not run here): a `ChatViewModelTest` case that appends two assistant deltas to `FakeDataRepository.chatHistory` while `askGate` is held, asserting `state` emits both intermediate histories; plus `desktopTest` + `:screenshotTests:verifyPaparazziDebug` per `AGENTS.md`.

---

## F. Out of scope / not verified

- **No build, unit test, or screenshot test was executed.** This container has no JDK, Gradle cache, or Android SDK, so `./gradlew` cannot run; every claim above is from code inspection only. Findings 1, 2, 4, 5, 6, 8, 9, 10, 12 are direct reads of the cited lines; finding 3's *timing* (empty `layoutInfo` on first composition) needs the §D diagnostic to be promoted from "most probable" to "confirmed".
- Streaming behaviour of the three providers' actual endpoints was not exercised; the conclusion that DORAI sends no streaming request is from the DTOs and call sites, which carry no such parameter.
- iOS/Android-specific lifecycle effects on `collectAsStateWithLifecycle` were not tested on device; the root-level collector at `App.kt:198` makes a stale-subscription scenario unlikely (finding 12).
- The legacy `ChatScreen` / `ChatTimeline` / `BotMessage` / `TopBar` / `AgentHud` path was read only to establish which contracts v2 dropped; it is not in production navigation (`ChatScreen.kt:140-147`).

---

## G. Remediation status (implemented 2026-11-09)

Findings 1 and 2 are addressed for the OpenAI-compatible provider family, and findings 3 and 11 are addressed by the pinned-tail rework in §G.1.

**Transport** — `Requests.openAICompatibleChatStream(...)` (`Requests.kt`): one `POST` with `"stream": true`, `bodyAsChannel()` + `readUTF8Line()` over `text/event-stream`, answering deltas handed to a callback as they arrive. It handles `delta.content`, silently accumulates `delta.reasoning_content`/`delta.reasoning`, assembles `delta.tool_calls` fragments by `index`, stops on `data: [DONE]`, skips `:` comments and undecodable keep-alives, and turns an `error` event into `OpenAICompatibleGenericException`. A provider that ignores `stream` and answers with JSON is handled in the same method (the message is delivered as one delta), so callers need no fallback branch. Streaming is deliberately **not** wrapped in `retryApiCall`: replaying after tokens were rendered would duplicate text; the fallback chain takes over instead.

**In-place rendering** — `StreamedAssistantRow` (`RemoteDataRepository.kt`): `askInternal` creates one per visible turn and threads it through `askWithKeylessFreeTierFallback` → `askWithService` → `plainChat`/`handleOpenAICompatibleChatWithTools` → `runToolLoop`. The first delta appends the assistant `History` row (with `sentAt` = first-token time); later deltas `copy(content = content + delta)` on the same row id, so the keyed `LazyColumn` item recomposes in place instead of a new message appearing at the end. Writes are coalesced to one per 50 ms (`STREAM_FLUSH_INTERVAL_MS`). `Messages.size`/`lastMessageTextLength` now change continuously, which is what `DoraiChatScreen`'s follow effect, streaming caret and "Worked for Xs" indicator were already keyed on — no UI change was needed.

**Finalization** — the authoritative `AssistantTurn` write now *updates* the streamed row (`copy(content, reasoningContent)`) when streaming produced one, and appends only otherwise. The tool loop does the same: a streamed round that returns tool calls is converted into the tool-call row (same id, original `sentAt`, `toolCalls` attached) instead of leaving a twin. On a provider failure the partial row is discarded before the next provider answers (`stream.discard()`), including inside the keyless free-tier retry chain.

**Boundary, deliberately not covered:** Gemini, Anthropic and the Responses API (`requiresResponsesApi` models) still use their buffered shapes — they now show one final write, exactly as before. On-device LiteRT inference is also unchanged.

**G.1 Follow-the-tail (findings 3 and 11)** — `DoraiChatScreen` no longer samples `isAtBottom` from a `remember { derivedStateOf { … } }` and re-reads it inside a keyed `LaunchedEffect`. A durable `pinnedToTail` flag is maintained by `snapshotFlow { listState.layoutInfo … }` over the whole lifetime of the screen: the last item leaving the viewport unpins, it becoming visible again pins, the jump-to-latest button pins and appears from the same flag, and a new (or restored) user prompt re-pins via `LaunchedEffect(lastUserId)`. The follow effect keeps its content keys (`messages.size`, newest text length) and now also scrolls to the newest item's *end* (`size - viewport`, clamped) instead of its top, so a reply taller than the viewport shows the text that just arrived. The streaming tail card's visibility is derived once at the top of the screen and shared with the effect, so both agree on whether a trailing item sits below the messages. Because the flag is state rather than a snapshot read at one moment, a completed answer can no longer be appended below the fold while the timeline was following — the case finding 3 describes.

**Still open (unchanged by this work):** findings 5–6 (main-thread state derivation and full regroup per emission — now exercised at ~20 emissions/s during a stream, so this moves up in priority), 7 (invisible compaction call), 8 (persistence only after success), 9–10.

**Verification:** none of this was compiled or run in this container (no JDK, Gradle cache or Android SDK), including the `DoraiChatScreen` rework in §G.1 — that UI change was not viewed on any target, so `:screenshotTests:verifyPaparazziDebug` is still owed per `AGENTS.md`. Static review only: `StreamedAssistantRowTest` (new, `commonTest`) covers coalescing, stable row identity, single-use `takeEntryId()` and `discard()`. Pending on a machine with a JDK: `./gradlew :composeApp:desktopTest`, plus a manual check that deltas appear progressively, that a tool turn shows the streamed text and the tool accordion on one row, and that a forced provider failure leaves no duplicate half-answer.
