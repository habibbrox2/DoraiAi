# Chat & Conversations

**Last verified:** 2026-11-10

DORAI AI's chat system manages the message history, conversation persistence, file attachments, and speech output. Conversations are service-independent — switching providers does not affect which conversation is loaded or restored. Multiple conversations are persisted and browsable via a history sheet.

## Concepts

### Conversation

A persisted chat session containing an id (UUID), message list, timestamps (`createdAt`, `updatedAt`), a title, and a type (`chat`, `heartbeat`, or `interactive`). Conversations are stored in a local database (browser builds use the settings store) and restored across app launches.

### History

The in-memory message list that drives the UI. Each entry has a role: USER, ASSISTANT, TOOL, or TOOL_EXECUTING. History is the source of truth during a session; it is written to a Conversation on save.

### Conversation Title

Auto-derived from the first user message when a conversation is saved for the first time. Truncated to ~50 characters at a word boundary. Once set, titles are not updated.

## Conversation Lifecycle

- The "current conversation" pointer is persisted across launches **by default**: opening the app restores whichever conversation was last active, including an empty new chat the user explicitly started. Settings → General → **"Restore last chat on launch"** (on by default) can disable this — with the toggle off, the app always starts a fresh empty chat on launch; share and assist cold-starts continue to bypass the restore regardless
- If the persisted pointer references a conversation that no longer exists (or is null because the user started a new chat), the app opens to an empty new chat
- "New Chat" clears history, unsets the current conversation pointer, and persists the empty state — so an unused new chat survives an app restart
- A new conversation ID (UUID) is generated on first successful save (after the first assistant response) and immediately becomes the persisted current pointer
- Conversations are saved after each assistant response
- Only the most recent 20 exchanges are persisted per chat conversation; heartbeat conversations have a separate, larger cap of 50 messages so longer automation runs are not truncated as aggressively
- Multiple conversations are persisted — starting a new chat preserves previous conversations
- Conversations are service-independent — switching services does not affect which conversation is loaded
- Interactive vs normal chat mode is persisted alongside the current pointer, so an empty interactive chat also survives a restart
- On the first launch after upgrading from a build that did not persist the current-conversation pointer, a one-time migration pins the most recently updated conversation as the current pointer so the user resumes where they left off
- On Android, sharing plain text into DORAI AI from another app (the system share sheet) starts a new conversation and puts the shared text in the input field, unsent. The previous conversation stays in history. The user can edit the text or add an instruction, then send as usual. Opening DORAI AI from the launcher or the assist gesture is unchanged; assist still starts an empty new chat

## Chat History

- A history icon appears in the top bar when saved conversations other than the current one exist
- Tapping it opens a bottom sheet listing all chat conversations sorted by last updated (newest first)
- Each item shows the title and formatted date
- Non-interactive conversations are outlined with a primary-colored border; interactive-mode conversations get an animated gradient border. The active conversation's title is rendered in the primary color (inactive titles use onBackground)
- Tapping an item loads that conversation and dismisses the sheet
- Each item has a delete button that defers deletion with a snackbar "Undo" option (~4 seconds) before the conversation is permanently removed. The snackbar appears inside the history sheet so it remains visible while the sheet is open, and the sheet stays open so multiple conversations can be deleted in sequence
- Deleting the active conversation clears the chat
- Heartbeat conversations are included in the history list with a "Heartbeat" label badge, and can also be accessed via the heartbeat banner

## Message Sending

- User message is added to history, then an API call is made via the fallback chain
- Tool calls are executed inline (TOOL_EXECUTING shown during execution, TOOL result stored after)
- On success, the conversation is saved
- On failure, an error is displayed with a retry button
- **Free rate-limit upsell:** when the user has **no configured services** (only Free FAST/EXPERT) and the request fails with a rate-limit, quota-exhausted, or Free proxy capacity error (e.g. “All free providers failed”), the chat shows a special panel instead of the generic error. It explains that Free is rate-limited and shows compact provider chips (icon + name) for free-usage providers (Groq, Cerebras, Gemini, OpenRouter, Ollama Cloud) in a flow layout; tapping a chip opens that provider’s API-key page in the browser. A retry control remains available. The panel is not shown if any non-Free service is already configured.

## Cancel

- While a request is in progress, a stop button replaces the send button in the input field
- Clicking stop cancels the ongoing API request and any in-flight tool executions
- After cancellation, the loading state clears and the send button reappears when typing

## Retry & Regenerate

- **Retry** resends the current prompt
- **Regenerate** removes all messages after the last user message, then resends

## Timeline Rendering

- The timeline follows streaming token growth (not just new messages) so the latest output stays visible during generation, and it aligns to the newest message's *end* so a reply taller than the viewport shows its newest text rather than its first line
- Following the tail is durable "pinned" state, not a layout snapshot taken at one instant: scrolling away from the bottom stops the auto-follow and shows the jump-to-latest button, scrolling back (or tapping that button) resumes it, and sending a prompt always re-pins. A completed answer can therefore never land below the fold while the timeline was following
- Replies stream over server-sent events (`"stream": true`) on the OpenAI-compatible provider family: the answer grows in place inside a single message, and tool calls are assembled from streamed fragments. Gemini, Anthropic and the Responses-API models (GPT-5 family) are still buffered and appear in one chunk; reasoning traces stream silently and attach to the message when the turn finishes. A provider that ignores `stream` and answers with plain JSON renders identically to a buffered reply
- When a response is pending but no assistant content has arrived yet (no text, reasoning, or tool activity), a first-token "thinking" card shows animated dots, the active model name, and a Stop button (instead of a bare spinner); reduced-motion keeps the dots static
- While the assistant reply is streaming in, a blinking trailing caret bar is shown under the reply text; reduced-motion shows a static bar
- Assistant messages that continue a run of assistant messages (no intervening user message) omit the repeated brand-mark + model header; a fallback note may still render on its own
- Assistant "who" row shows the active provider's identity icon when the active service is known (the generic app logo is the fallback); the Local/Cloud badge next to it reflects where inference actually runs — a keyless free-fallback answer always shows Cloud, even when the configured primary service is on-device, and an OpenAI-Compatible endpoint shows Local only for loopback/private/LAN base URLs (default `localhost:11434`), otherwise Cloud
- A subtle "Worked for Xs" indicator renders at the top of a complete agent response when the history carries per-entry timestamps (≥ 2s span; hidden otherwise)
- Agent background work (reasoning, tool calls) renders as chronological, collapsed-by-default accordions — a "Thought process" block followed by a "Ran …" block — the final Markdown answer always renders below them, fully outside the processing blocks; tool calls awaiting approval or failed tools stay always visible outside the accordion because the agent turn is blocked until the user answers
- Tool-approval and failed tool cards sit outside the accordion; the accordion header carries a ≥48dp button-role touch target with expanded/collapsed state description
- Only the last assistant message in the timeline shows the regenerate button; earlier ones keep copy/speak actions
- User messages carry a trailing copy button (next to the bubble) with the same 2-second copied feedback as the assistant copy action
- Timestamps (HH:mm, local 24h) render under user bubbles and assistant replies; they are persisted per-message in the conversation JSON blob (`Conversation.Message.sentAt`) so restored conversations show times too (messages saved before this field existed render without a timestamp)
- Markdown images in assistant replies: a standalone `![alt](url)` block renders `Fit`-scaled (max 360.dp, aspect-ratio preserving, rounded corners), shows a loading placeholder while fetching and an "Image unavailable" fallback on error, and opens a full-screen viewer (pinch/double-tap zoom) on tap; images inline with text render as a horizontal thumbnail strip under the paragraph instead
- `Ctrl/Cmd+K` opens a command palette (new chat, switch model, open settings, and jump to a saved conversation filtered by title); arrow keys select, Enter runs, Escape dismisses. Desktop/Web only. The composer already sends on Enter (and Ctrl/Cmd+Enter also sends)
- Looping/decorative animations respect `prefersReducedMotion()`: logo, loading spinner, dynamic-UI button pulse, gradient border, agent orb/HUD, typing dots, loading bar, streaming caret — all render statically when the user prefers reduced motion
- A font-scale stress snapshot (`chat_compact_conversation_fontscale_14`, 1.4×) locks in layout at large font scales; the assistant model name truncates with ellipsis before pushing the provider badge off-screen

## File Attachments

Multiple files can be attached to a single prompt. Each file is added one at a time via the file picker or drag-and-drop, and appears as a chip below the input. Clicking a chip removes that specific file from the queue. All queued files are cleared after the prompt is sent. Three categories of files are supported:

### Images
- Attach via file picker or drag-and-drop
- Compressed to JPEG and Base64-encoded
- Maximum raw input size: 50 MB; maximum size after compression: 15 MB — rejected with a size error if exceeded
- Sent as `image_url` (OpenAI-compatible), `image` block (Anthropic), or `inline_data` (Gemini)
- Not offered on the built-in Free service (its proxy fans out to text-only fallback models that reject multimodal content) or on 1min.AI
- Also hidden when the active model is known to be text-only (e.g. DeepSeek chat models, GLM text models), even on services that otherwise accept images
- Shown as a preview thumbnail (max 200dp wide) inside the user message bubble
- Clicking the thumbnail opens a full-screen viewer with pinch-to-zoom, double-tap to toggle zoom, pan when zoomed, and a close button in the top-right (also dismissable via the Android back button or by tapping the backdrop; desktop has no keyboard shortcut for dismissal)

### Text files
- Supports `.txt`, `.md`, `.json`, `.csv`, `.xml`, `.yaml`, `.html`, `.css`, `.js`, `.ts`, `.kt`, `.py`, `.rs`, `.go`, `.c`, `.cpp`, `.swift`, `.sh`, `.sql`, `.toml`, `.ini`, `.log`, `.gradle`, and more
- Maximum size: 200 KB per file
- Content is decoded at send time and concatenated into the user message with a filename header per file
- Works with all providers (content is inlined as text)
- Shown as a filename chip in the user message bubble

### PDFs
- Base64-encoded without compression
- Maximum size: 20 MB — rejected with a size error if exceeded
- PDF attachments are advertised only by services with native document support: Anthropic, Gemini, OpenAI, and OpenRouter. The file picker offers PDF on those services only
- Sent as a `document` block (Anthropic) or `inline_data` (Gemini). On the OpenAI-compatible wire path (OpenAI, OpenRouter, and other OpenAI-compatible services), PDF binaries are currently dropped from the request body — only image parts are encoded as `image_url` — so a PDF attach on OpenAI/OpenRouter is accepted by the UI but not transmitted to the model
- Shown as a filename chip in the user message bubble

### General behavior
- The attachment button is shown whenever the active service supports file attachments (text files work with all remote models); it is hidden when the active service runs on-device, since on-device services do not support attachments
- Unsupported file types (e.g., `.zip`) show an error message
- Files exceeding the per-category size limit show a size error; size is checked by stat before the file is read, so multi-gigabyte attachments are rejected without allocating memory for the full contents
- Long filenames in chips are truncated with an ellipsis while preserving the extension
- File attachments persist across conversation save/restore via an `attachments` list on each message; older conversations saved with a single-file schema are migrated on load. An attachment large enough to push its message past the storage cap is dropped from the saved copy (see [Conversation Storage](#conversation-storage)) — the message text stays

## Speech Output (TTS)

- Toggle in the top bar enables auto-play of new assistant messages
- Per-message play button on assistant messages
- Markdown is stripped before speaking
- Speech uses the engine the user selected in the platform's system speech settings, so third-party engines are honoured; on Android, Google's engine is only used as a fallback when the system default cannot start
- The engine's own default voice is kept as-is. DORAI AI only picks a different voice when the default one does not speak the system language, in which case the first voice matching that language is selected

## Conversation Storage

- On Android, iOS, and desktop, conversations live in a local SQLite database in app-private storage: one row per conversation plus one row per message, so saving a turn writes only the affected conversation instead of re-serializing the whole history
- The browser build has no persistent database and keeps the full conversation list as a JSON blob in the settings store (see [encryption.md](encryption.md))
- Conversations are upserted — updating a conversation replaces the existing entry by ID, new conversations are appended; the list loads ordered by creation time
- Each conversation also retains a rolling tail of its sandbox shell transcript (last ~10,000 characters) so that follow-up commands in a resumed conversation see the prior shell context; transcript updates write only that field
- Migration chain, run once on first load: the legacy encrypted `conversations.enc` file (XOR with a 32-byte random key) migrates into the settings-store blob; a settings-store blob found on a database-capable platform is imported into the database and removed. Settings import reuses the same path — imported conversations are staged in the settings store and absorbed into the database on the next load, replacing its content
- The database structure is versioned and lives on user devices: any future change to its tables or columns requires an accompanying migration step so existing installs upgrade in place (message contents are stored as JSON and tolerate unknown fields, so message-level additions do not need one)
- A stored message is capped at roughly one megabyte, because Android reads database rows through a fixed-size window and a larger row would abort the whole load and crash the app on every launch. A message past the cap is stored without its file attachments; if it is still too large, its text is kept only up to a fixed head length. The in-memory message the model sees during the ongoing turn is untouched — only the persisted copy shrinks
- Messages already stored above that cap by an earlier version are skipped when the conversation loads, so a database that used to crash the app opens with the rest of its history intact

## UI Elements

- **Top bar**: a model pill in the middle showing the active service and the model it is running; tapping it opens the model list and the service list, so the model can be changed without leaving the chat. Alongside it: an interactive-UI toggle (the widgets icon — tinted while the mode is on) with a back action next to it that removes the last exchange, a Sandbox toggle (Android only; it flashes briefly while a shell command runs), TTS toggle, and a direct Settings button. The compact HUD no longer has a three-dot overflow menu.
- **Scroll to bottom**: a small floating action button (down arrow) appears when the user has scrolled up past the latest messages; tapping it animates back to the bottom
- **Messages**: user (right-aligned, with optional image preview), assistant (Markdown-rendered + action buttons), tool executing (spinner), loading indicator, error with retry (or free-provider suggestions panel when Free is rate-limited with no services configured). When the fallback chain answered with an alternate service rather than the user's selected one, a small "Answered by …" label is shown under the assistant message naming the service that produced the response
- **Interactive UI**: a mode of this same chat, not a separate screen. With it on, the AI answers with a dorai-ui screen that renders inline in the conversation and its buttons and forms work like any other message. See [runtime-agent/features/dynamic-ui.md](https://github.com/habibbrox2/DoraiAi/blob/main/runtime-agent/features/dynamic-ui.md)
- **Input**: text field, send/stop button, attachment button, file chip
- **Empty state**: animated logo centered above the welcome text "Your AI. Your World." on the premium dark backdrop, followed by wrapping quick-start chips
- **Drag-and-drop**: supported for file attachments
- **History sheet**: bottom sheet listing saved conversations with title, date, active highlight, and delete

## Navigation Sidebar

On compact screens the chat is wrapped in a modal navigation drawer; on expanded screens the same sidebar is shown beside the conversation. The sidebar is ordered top to bottom:

1. **Header** — DORAI mark, product name, tagline, and the conversation search field
2. **Primary actions** — New Chat and New Project
3. **Chat history** — a "Recent chats" section grouped by date; search filters titles live, and empty states distinguish "No chats yet" from "No matches"
4. **Scrollable area** — the history list takes the remaining space
5. **Bottom navigation** — Dashboard, Interactive UI, Workspace, Settings
6. **Footer** — logo and version

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../data/Conversation.kt` | Conversation and message data classes, type constants |
| `composeApp/src/commonMain/.../data/ConversationStorage.kt` | In-memory conversation flow, transcript trimming, legacy migration |
| `composeApp/src/commonMain/.../data/ConversationPersistence.kt` | SQL and settings-blob persistence backends, staged-import handling |
| `composeApp/src/commonMain/sqldelight/com/dorai/ai/db/conversation.sq` | Conversation and message table schema and queries |
| `composeApp/src/commonMain/.../data/FileClassification.kt` | File category enum, MIME/extension classifier, size constants, file exceptions |
| `composeApp/src/commonMain/.../data/RemoteDataRepository.kt` | History management, conversation save/restore/delete, title derivation, message sending |
| `composeApp/src/commonMain/.../ui/chat/ChatViewModel.kt` | Chat UI state, send/retry/regenerate/cancel/loadConversation/deleteConversation actions; startup restore honored from the `restore_last_chat` General toggle |
| `composeApp/src/commonMain/.../ui/settings/GeneralSettings.kt` | General settings (incl. "Restore last chat on launch" toggle) |
| `composeApp/src/commonMain/.../ui/chat/DoraiChatScreen.kt` | UI v2 chat screen (active): adaptive list-detail scaffold, message timeline rendering (user bubbles, assistant who-row/markdown/tool cards, streaming typing indicator, jump-to-latest). who-row shows the active provider's icon from `ChatMessageItem.serviceIcon` |
| `composeApp/src/commonMain/.../ui/chat/ToolEventGrouper.kt` | Groups conversation history into chat messages; propagates the active service's `executionLocation` (Local/Cloud) and provider icon onto assistant messages; fallback-answered messages are always Cloud; computes the agent-turn wall-clock span used by the "Worked for Xs" indicator |
| `composeApp/src/commonMain/.../ui/chat/composables/ChatCommandPalette.kt` | Ctrl/Cmd+K command palette: jump actions + conversation search |
| `composeApp/src/commonMain/.../ui/chat/composables/AppSidebar.kt` | Navigation sidebar: header/search, New Chat/New Project, grouped history, bottom nav, footer |
| `composeApp/src/commonMain/.../ui/chat/composables/EmptyState.kt` | Centered logo, "Your AI. Your World." welcome copy, wrapping quick-start chips |
| `composeApp/src/commonMain/.../ui/chat/composables/AgentHud.kt` | Compact HUD with direct Settings/Speech actions (no overflow menu) |
| `composeApp/src/commonMain/.../ui/chat/composables/ChatHistorySheet.kt` | Bottom sheet listing saved conversations |
| `composeApp/src/commonMain/.../ui/chat/composables/HeartbeatBanner.kt` | Dismissable banner for heartbeat notifications |
| `composeApp/src/commonMain/.../ui/chat/composables/TopBar.kt` | Top bar with the model pill, the interactive-UI toggle and back action, sandbox, TTS, and settings icons |
| `composeApp/src/commonMain/.../ui/chat/composables/FreeProviderSuggestionsPanel.kt` | Free rate-limit upsell panel with provider chips |
| `composeApp/src/commonMain/.../data/FreeProviderSuggestions.kt` | Providers offered in the Free rate-limit upsell panel |
| `composeApp/src/commonMain/.../ui/chat/composables/QuestionInput.kt` | Text input with send/stop button |
| `androidApp/src/main/AndroidManifest.xml` | Share-sheet registration for plain text (`ACTION_SEND`) |
| `androidApp/src/main/kotlin/.../MainActivity.kt` | Reads shared text from `ACTION_SEND` and hands it to chat |
