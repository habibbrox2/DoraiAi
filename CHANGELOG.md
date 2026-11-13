## DORAI AI - Offline Voice Mode (Android, Bengali) (Unreleased)

### Added
- **Voice Mode on Android (Phase 1)**: the chat composer's mic button now works. Tap it, the app asks for the microphone, the device's own speech recognizer listens, and the transcript is sent as an ordinary chat message — a voice turn gets exactly the same tools, network access and persistence as a typed one. The assistant's answer is read back sentence by sentence. Voice Mode is **off by default** and is switched on in **Settings → Voice**; it is Android-only for now, so the desktop, iOS and web builds keep the voice entry points hidden.
- **Bengali-first speech pipeline** (`com.dorai.ai.voice`): markdown, code, raw URLs and emoji are removed before anything is spoken, and prose numbers are read as Bengali words — "২০২৬" is spoken "দুই হাজার ছাব্বিশ", while identifiers, versions, decimals and currency amounts keep their written form ("v2.1", "৳৫০", "1.5GB"). Bengali and Latin words that are written glued together are separated so the synthesizer does not read one unpronounceable token. Bengali conjuncts, matras and joiners are never reordered, stripped or normalised.
- **`SpeechToText` contract** in `commonMain` keeps the chat pipeline independent of the engine: the on-device sherpa-onnx binding planned for Phase 2 drops in behind the same interface without touching the chat UI, the settings screen or the speech pipeline.
- **`RECORD_AUDIO` runtime permission** is requested only when a voice turn starts, never at launch. Denying it leaves text chat untouched and points at the system settings instead.
- **Honest offline behaviour**: the recognizer is asked to prefer its on-device model. A device with no offline Bengali language pack reports a network error and the turn fails with a clear message, instead of quietly streaming the audio away.
- **Docs**: `docs/knowledge/voice/` is a new OKF bundle (engine inventory, speech-model pin and bump policy, update log), and `docs/features/offline-voice-ai.md` now records the shipped Phase 1 alongside the remaining plan.
- **Voice model download infrastructure (Phase 2)**: a pinned catalog entry for the Coqui bn VITS model (`models/voice/<id>/`), `AndroidVoiceModelDownloadWorker` (foreground service, unique work with `KEEP`, Wi‑Fi-only by default with an optional mobile-data opt-in), `.part` resume with byte-Range, re-hashing of kept bytes to rebuild the streaming digest, per-file length + SHA-256 verification, atomic rename, `.sha256` markers next to verified files, and a pre-download free-space gate (model size plus 25 % headroom). The model's license is not declared upstream; on 2026-11-13 the project owner recorded a license sign-off for the Coqui bn VITS model in all flavors (accepting the upstream repo's silence; the model lineage is Coqui TTS, typically MPL-2.0). The download button is **enabled** as of that sign-off.
- **Voice model management in Settings → Voice**: per-model download, pause, resume, delete and re-download, plus the mobile-data opt-in toggle; a model is never usable before every file passes verification.

### Fixed
- **Voice settings hid the Models area silently when empty**: with no catalog entries the whole section vanished without a word; it now shows an honest empty state ("উপলব্ধ মডেল এখানে দেখা যাবে").
- **Bengali voice strings render as mojibake on device**: the `settings_voice_*` strings (and three English ellipsis/em-dash values) added in the Phase 2 infrastructure were double-encoded at insertion; they now display correct Bengali in Settings → Voice.
- **TTS speed/pitch/volume labels showed "100%%"**: `tts_value_format` used `%%`, which Compose resource formatting does not unescape; the labels now show a single `%`.

### Changed
- The composer's mic button becomes a stop button while a voice turn is running, with a short status line under the input (listening / transcribing / thinking / speaking). The resting composer is unchanged.

---

## DORAI AI - Device Actions & MCP-Only Tool Approval (Unreleased)

### Added
- **Device actions (Android)** — six tools ported from the Google AI Edge Gallery "Mobile Actions" pattern: `flashlight_on` / `flashlight_off` (direct CameraManager torch), `create_contact`, `send_email`, `show_location_on_map`, `open_wifi_settings` (intents into the system UI). Localized (en + bn), each with its own toggle in Settings → Connectors → Plugins, work in chat like every other native tool, and are exposed to on-device (LiteRT) models via the documented allowlist. Content-creating actions launch the system UI — contact editor, email composer — rather than writing Contacts/sending mail directly, so the user reviews every change.

### Changed
- **MCP-only tool approval** — by product decision, only MCP connector tools ask the user before running (the confirmation card is unchanged for MCP). **Every non-MCP tool — shell commands, file writes/deletes, GitHub write tools (`github_commit`, `github_push`, `github_create_pull_request`), device actions, SMS/USSD, email, everything — now executes without a per-call prompt**, in chat and in background/heartbeat runs alike. The on-device (LiteRT) tool exposure now follows the documented allowlist in `docs/features/tools.md` (previously all available tools were handed to on-device models).

### Docs
- `docs/features/tools.md` updated (approval policy, device-action tools, on-device allowlist implementation); `Last verified` bumped.

---

## DORAI AI - MCP OAuth (Desktop Commander & OAuth-Native MCP Servers) (Unreleased)

### Added
- **MCP OAuth support for OAuth-native servers**: DORAI AI now completes the RFC 6749 authorization-code flow (RFC 9728 Protected Resource Metadata + RFC 8414 Authorization Server Metadata + RFC 7591 Dynamic Client Registration + S256 PKCE) end-to-end for MCP servers that require it — today this is **Desktop Commander** (`https://mcp.desktopcommander.app/mcp`), added as a popular preset. Tapping **Authorize** on the server card opens the browser; the server signs the user in, the loopback callback captures `code`, and the token pair is stored in the platform's encrypted settings. Token is refreshed lazily at connection time; **Revoke OAuth** clears the session without deleting the server.
- **Desktop Commander preset** in the popular connectors list — "Authorize once in Connectors" used the same OAuth flow; no manual token entry needed.
- `McpServerConfig.oauth` field (serialized in encrypted settings); `McpOAuthSession`, `McpOAuthFlow`, `McpOAuth` (discovery / DCR / exchange / refresh), `OauthCallbackListener` (platform actuals bind `127.0.0.1:<ephemeral>`), and `McpCryptoPlatform` (jvmShared SHA-256 + SecureRandom) are new.
- Tests pin the OAuth plumbing: protected-resource URL mapping, AS metadata URL, CSPRNG length/uniqueness, PKCE S256 vector, authorization-URL parameter composition, and `McpServerConfig.oauth` roundtrip.

---

## DORAI AI - Workspace Runtime Agent and Critical Audit Remediation (Unreleased)

### Added
- **Runtime agent chat modes wired end-to-end**: Ask / Plan / Build / Review pill selection now reaches the runtime agent (`ChatSystemPromptBuilder.agentModeSection`), not just the UI. Each non-Build mode renders an explicit behavioural contract at the top of the system prompt (Ask = read-only Q&A, Plan = investigate then produce a plan before any change, Review = report with `file:line` evidence and never edit) and forces the agentic tool loop off; Build keeps the full tool set. The persisted "Default agent mode" setting now actually seeds new conversations instead of sitting unused, and `setAgentMode` keeps `ChatViewModel` and `RemoteDataRepository` in sync on every selection.

### Fixed
- **Workspace runtime agent crash (`IndexOutOfBoundsException`) in the session bar**: `tabFocusRequesters` is now keyed on tab count and all read sites clamp the focused index (sessions starting mid-composition used to grow the list after it was captured).
- **Guest git `clone` always failed for fresh projects**: the overwrite guard checked a host path `/root/projects/<name>` that never exists and `mkdirs()` silently no-oped. The host check now uses `paths.projectsDir` (the real bind-mount source) so cloning into a brand-new project works and the overwrite guard actually guards.
- **App runtime agent reported every agent missing even when installed**: the host-side probe checked `~/.local/bin/<binary>` for every agent. Per-agent launcher paths now live in `WorkspaceAgents` and the probe follows them with a dangling-symlink-safe check.
- **Workspace agent mode badges and tab labels**: the "Install Sandbox" CTA and badge strings no longer claim a Debian install == "running sandbox"; tab pills show localized labels instead of raw enum names.
- **One-shot proot commands leaked guest processes when the timeout fired**: `destroyForcibly()` alone left orphaned children (locks, agents). The guest now records a PID before `exec`, and the timeout path kills the recorded process tree via `/proc` (Android has no `Process.pid()`).
- **Desktop/iOS/wasm Workspace reported a phantom clean repository**: `NoOpWorkspaceController.gitStatus` now returns `Failed/isRepository=false` instead of "clean".
- **Tar extraction hardened against hostile archives**: regular-file containment now rejects prefix-collision escapes (`rootfs2/evil`); hardlink targets must stay inside the rootfs; symlink entries reject absolute host-roots and `..` escapes (Debian `/data`, `/storage`, `/system`, `/vendor`, `/proc`, `/sys`).
- **Partial Debian/Alpine installs left a broken rootfs when the download, extract or configure step failed or was cancelled**: the whole bootstrap is now wrapped and wipes the partial rootfs on any Throwable.
- **Workspace git diff flipped the wrong file's contents when requests overlapped**: `loadGitDiff` now uses a monotonic request token; only the newest request can repaint the diff.
- **`refreshGitStatus` left `loading` stuck on cancellation/failure**: `loading` always clears in a `finally` block.
- **Active-project top bar project name pushed tabs/badges off-screen**: the title is now bounded to one line with ellipsis.
- **Workspace file explorer rows shifted right with nested project paths**: the indent was derived from the absolute path depth; it is now a fixed neutral spacer for the flat project listing.
- **Removed unused Workspace pieces**: dead `Code` icon import, unused top-level `widthClass`, unused `GitChangeItem` (and its test), and the `workspaceTabAgentPanelWidth` adaptive token (and test) that only fed the unwired Agent tab.
- **iOS builds latently broken by missing actuals**: `createGitCommandRunner` and `createGitCredentialBroker` now have iOS stubs.
- **Agent installers are HTTPS-only with an SHA-256 audit log** (`/root/.local/state/dorai-agent-install.log`) so a swapped vendor URL or compromised mirror is tamper-evident.

### Changed
- **Workspace screen copy honesty**: the two "completed" setup checkmarks are no longer fake-done while no install is running; `isSandboxRunning` was renamed to `isSandboxReady` and wired to `build.isReady` (Debian present), and the setup screen is consistent with that.
- **Terminal touch-scroll now emits TUI wheel events** (was touch-drag-only), and **OSC 8 hyperlink rows meet the 48dp touch target** with a button role.
- **Agent installer detection verifies the binary by SHA-256 audit log** before confirming success (tamper-evidence for the supply chain).
- **Workspace "Agent" tab removed** (was a permanent empty placeholder); the empty right-hand "Agent rail" on Compact screens is likewise gone.

---

## DORAI AI - Chat UX, Accessibility and Performance Polish (Unreleased)

### Fixed
- **Tool cards show localized error details**: the failure panel, its copy-to-clipboard target, and the exit-code title/label now use localized strings instead of hardcoded English; the tool failed-panel copy button meets the 48dp touch target.
- **Hardcoded English in v2 surfaces is now localized**: the "Interactive reply" dynamic-UI badge, tool-card expand/collapse/lines labels, markdown code blocks ("Run"/"Copied"/"Copy code"), diff blocks ("Copied"/"Copy diff"), the search field placeholder, status badge descriptions ("Status: …"), and the image alt fallback.
- **Accessibility**: the agent timeline expander announces as a button; interactive dorai-ui checkbox/switch/radio rows now merge row semantics with proper role and Checked/On/Selected state descriptions (previously hardcoded English state hints); the model pill carries a button role; markdown image alt falls back to the localized "Image".

### Changed
- **Markdown tables render at most 100 rows**; longer model-emitted tables end with a "… and N more rows" footer instead of expanding into unbounded subtrees (responsiveness safeguard for very large tables).
- **Removed dead chat-v2 state**: the never-called `toggleSpeechOutput` action and `isSpeechOutputEnabled` field, and the `goBackInteractiveMode` action (v2 has no interactive back affordance; the extended interactive-mode tests exercise `setInteractiveMode` directly, which stays).
- **File-review previews are no longer retained in chat state**: the proposals list is still streamed, but the potentially large preview map is no longer built eagerly or kept in UI state.

### Tooling
- **Screenshot coverage for chat states**: streaming-tail, error-badge, free-provider suggestions panel, and frozen interactive answer cases added to the UI v2 Paparazzi suite.

---

## DORAI AI - Chat Prompt and Agent Security (Unreleased)

### Fixed
- **Untrusted content in heartbeat prompts is fenced**: email/SMS/notification previews, prior heartbeat outputs, and memory promotion-candidate contents are wrapped in explicit `<untrusted:…>` delimiters with a standing rule that everything inside them is data to summarise — never instructions to follow, no matter how it frames itself. Previously these flows carried tool-enabled runs (the heartbeat can call shell, SMS, email, and scheduling tools), so an email/SMS/notification could inject commands the heartbeat would execute. Angle brackets inside embedded content are neutralised so embedded text can never terminate a fence. User-authored custom prompts and Heartbeat Additions are not fenced.
- **Interactive-mode `[SYSTEM]` retry text no longer appears in the conversation**: the message the app sends the model when an interactive answer can't be rendered is hidden from the UI (no user bubble) and from persistence (not stored in the conversation); it exists only to seed the retry turn in memory. Previously it leaked as a visible, persisted `"[SYSTEM] Your previous response failed to render as interactive UI…"` user message that providers also re-received forever.
- **Memory tools no longer prompt per call and now behave consistently**: `memory_store`/`memory_learn`/`memory_reinforce` were classified `OTHER` (ask-every-call) while `memory_forget` accidentally classified `READ_ONLY` (its name contains `get`). All four memory tools now share one risk class governed by the memory master switch, consistent with the sandbox tools and the documented product behavior.

### Changed
- **Remote chat memory injection is budgeted (8,000 chars)** instead of unlimited — a large memory store is no longer dumped wholesale into every system prompt (token cost and context rot). Local chat stays at 2,000 chars.

---

## DORAI AI - Chat Backend Integrity (Unreleased)

### Fixed
- **On-device/local failures no longer abort the provider fallback chain**: a LiteRT/on-device error now falls through to the next configured service (LOCAL_FIRST / hybrid routing). Previously any on-device failure rethrew immediately, bypassing the fallback chain — cloud providers never got a chance to answer. Re-running on another provider remains vetoed when the failed attempt already executed tools.
- **Heartbeat / scheduled runs no longer hang forever on tool confirmations**: tools that require user approval are refused immediately (with a tool error fed back to the model) in background runs instead of awaiting a confirmation UI that can never appear, which permanently wedged the whole scheduler loop (heartbeat, tasks, email/SMS polls). The visible chat is unaffected.
- **Persistently failing heartbeats no longer hot-loop**: the heartbeat watermark (`lastHeartbeatEpochMs`) advances on failed attempts too, so a failure waits a full interval before the next attempt instead of retrying every 60 s scheduler tick and burning the user's quota.
- **Tool calls survive conversation save/restore**: assistant `tool_calls` (id, name, arguments, `thoughtSignature`) and tool-result pairing (`toolCallId`, `toolName`) are persisted and restored, so tool-round conversations restored after an app restart keep provider pairing (including DeepSeek/Gemini thought signatures) instead of degrading to bare text.
- **Oversized tool-call argument payloads are shrunk instead of failing the save**: persistence now progressively drops tool-call arguments (then the tool calls) for messages exceeding the 1 MB row budget instead of failing the write.
- **A tool row is never persisted without its initiating tool_call**: compaction and conversation-trimming cut history only on assistant/user boundaries (never leaving an orphaned tool result at the start of the kept window).
- **The user's message is persisted even when the reply fails**: a terminal provider failure now saves the conversation (best-effort) so an app restart cannot lose the message the user already sent.

---

## DORAI AI - Chat UI Wiring and Attachment Protection (Unreleased)

### Fixed
- **Interactive dorai-ui blocks are now interactive**: the AI's inline interactive answers (buttons, forms, quiz choices) call back into the chat when the app is in interactive mode. Previously the callbacks were never wired — every interactive block rendered as read-only, so interactive replies could not be submitted at all. The block on the newest answer is active while a turn is not streaming; once the user submits, the answered block freezes showing the submitted values, and historical interactive blocks render read-only.
- **Free-tier upsell panel is reachable**: when a request fails because no services are configured, the free-provider suggestions panel (provider signup chips + retry) is now rendered in place of the error badge. Previously the panel was never instantiated anywhere and the error badge was suppressed when the upsell flag was set, so the error surface vanished entirely.
- **Share / Assist / Dashboard Design composer prefills are honored**: a prompt passed to a new chat (Share intent, Assist cold start, Design card) is typed into the composer instead of being dropped silently; the prefill is consumed exactly once.
- **Attachment abuse is rejected at add time**: at most 5 attachments per message; each file is checked against its category size limit (image 50 MB, PDF 20 MB, text 200 KB) before being read, and the queue is capped at 40 MB total — previously files of any size or count could be queued until the send path eventually failed.
- **Base64 image attachments decode off the main thread** during composition instead of blocking it.
- **"Waiting approval" badge now reflects reality**: the app-level approval badge derives from the transient tool-confirmation entries actually pending in the conversation instead of an always-empty state list.
- **ViewModel clear-time flush is supervised**: the pending conversation deletion flushed from `onCleared` runs on a supervised background scope and logs failures instead of being lost.

### Changed
- **Removed the invisible follow-up-suggestions LLM call** that ran after every non-interactive answer and was never rendered anywhere (a paid, unused request per answer).

---

## DORAI AI - Settings Architecture Improvements (Unreleased)

### Added
- **Memory Instructions setting** (Settings → Agent → Memory Instructions): edit persistent guidance text that shapes how the AI stores and retrieves memories across conversations. Previously read-only with no UI.
- **Atomic tool permission API**: `AppSettings.setToolPermission(toolName, scope, enabled)` writes a single permission in one call (`enabled = true` → always allow, `enabled = false` → ask every time) instead of callers hand-assembling a `ToolPermission`.

### Changed
- **Unified theme system**: removed deprecated `AppSettings.themeModeFlow` / `getThemeMode()` / `setThemeMode()` / `ThemeMode` enum / `KEY_THEME_MODE` constant. `ThemeSettingsRepository` (premium presets, Match Device, Flat/3D style, custom themes) is now the single authoritative source; legacy `theme_mode` key is kept in sync for Android system bars.
- **Migration code refactored to typed API**: added typed setters for all migration flags (`setMigrationComplete()`, `setServicesMigrationComplete()`, `setInstanceMigrationComplete()`, `setBaseUrlV1MigrationComplete()`, `setCustomModelMigrationComplete()`). `migrateFromLegacyIfNeeded()` and other migrations now use `setEmailAccountsJson()`, `setSplinterlandsAccountJson()`, `setEmailPassword()`, `setSplinterlandsPostingKey()`, `setBaseUrl()` instead of raw `settings.put*()` calls.

### Localization
- Added Bengali strings for Memory Instructions UI (`settings_memory_instructions`, `settings_memory_instructions_description`, `settings_memory_instructions_placeholder`, `settings_memory_instructions_save`).

### Security
- `getEncryptionKey()` confirmed read-only (set once during install/migration); no setter exists and none will be added.
- Legacy → encrypted settings migration keeps clearing the plaintext originals for account JSON and account-scoped secrets (email passwords, Splinterlands posting keys) even when the encrypted store already holds a newer value, so secrets don't survive unencrypted on disk.

---

## DORAI AI - Chat Data Protection (Unreleased)

### Fixed
- **Unparseable pending import no longer wipes the conversation database**: a corrupt (non-blank) staged-import blob at the import inbox key is quarantined to a backup key and the database is left untouched; the importer's explicit blank-string "clear" signal is preserved.
- **Corrupt settings-blob no longer silently yields an empty history**: loadAll quarantines an undecodable settings blob to a backup key instead of returning an empty list that could overwrite it on the next save.
- **Legacy conversation file is kept on decode failure**: an unparseable legacy `conversations.enc` file is copied to a backup key and left on disk for manual recovery instead of being deleted.
- **Persistence failures no longer leave diverged in-memory state**: `saveConversation`/`deleteConversation`/`renameConversation`/`togglePinConversation`/`updateShellTranscript` roll back the in-memory change when the persistence write fails, and surface the error to the caller instead of letting the UI show unsaved state.
- Undecodable message rows no longer vanish silently — each is logged with the decoder error.

### Security
- Corrupt conversation payloads (pending imports, settings blobs, legacy files) are backed up under a quarantine key for manual inspection rather than being silently discarded.

---

## DORAI AI - AI Provider System Fixes (Unreleased)

### Added
- **Free-fallback toggle in Services UI**: Settings → Services now shows a \"Use as fallback\" switch (previously the flag had no UI). Routing honors it: with it off, a failed primary provider fails visibly instead of silently shipping the turn to the keyless Free service.
- **Chat model picker now lists models**, not just providers: the \"Select AI Model\" sheet shows the active provider's models and lets the user switch model without leaving chat.

### Changed
- **Base URL field is shown only for OpenAI-Compatible and Cloudflare** (gateway + account id); every hosted provider's stale custom base URL is ignored by `resolveUrl`, so a previously entered value can no longer be spliced into a canonical endpoint URL.
- **403 responses are mapped per provider**: an auth-shaped 403 (OpenCode, or a gateway reporting an invalid key) surfaces as an invalid-key error instead of a false \"content moderation\" block.
- **Perplexity key validation** now accepts only the documented success statuses (200/400/404/422); other 4xx/5xx no longer report \"Connected\".
- **OpenAI-Compatible instances with a blank base URL** are excluded from the fallback chain.
- **Settings state hygiene**: changing an API key or base URL clears the stale model list/selection; selecting a model from the dropdown clears the custom-model override; a denied local-network permission no longer leaves the model fetch spinner spinning forever.
- **Chat provider badge** now uses the entry's execution location instead of matching the non-existent \"ollama\" service id.
- **Cloudflare AI Gateway provider** added alongside Cloudflare Workers AI: requests are gateway-routed via the `cf-aig-gateway-id` header. Each gateway instance can set **AI Gateway ID** (Settings → Services); blank uses the account's auto-created `default` gateway.
- **Dead chat code removed** (TopBar/AgentHud/ChatComposer/QuestionInput/ServiceSelector) and the screenshot tests updated.

### Security
- **Desktop settings key is DPAPI-protected on Windows** (`CryptProtectData`, user scope, via JNA): the raw AES-GCM key no longer sits in the clear next to `~/.dorai/settings.aes`. Legacy raw-key installs are transparently re-protected on first launch. macOS/Linux keep the owner-only key file (OS keystore integration is pending).
- **Cloudflare Account ID validation**: `resolveUrl` now splices only well-formed 32-hex account ids into Workers AI/AI Gateway URLs; anything else yields a deterministic rejection instead of an arbitrary-string-in-path URL.

### Fixed
- **Model auto-pick no longer hardcodes `kimi-k2.5`** — the provider's own model ordering is respected (curated defaults still take precedence).
- **Truncated SSE streams are no longer reported as successful completions**: a stream that ends without `[DONE]` or a finish reason surfaces as an error, letting the fallback chain retry/fail visibly instead of silently rendering partial answers.
- **`Retry-After` honored on 429**: rate-limit errors carry the server-provided hint (delta-seconds, capped at 300s) and `retryApiCall` waits `max(backoff, retryAfter)`. Streaming completions remain non-retried by design.
- Hardcoded English strings in provider surfaces localized (\"Cloudflare Account ID\", \"Cloudflare AI Gateway ID\", \"The provider is connected, but it returned no available models.\", permission-denied fetch error).
- Model picker search empty state and custom-model checkbox accessibility (role = Checkbox).

---

## DORAI AI - GitHub Integration (Unreleased)

Connect an embedded GitHub App to the app with read-only access by default. Not yet released.

### Added
- **New GitHub settings category** (Settings → GitHub, icon: code) for the dedicated `DORAI AI GitHub` App: sign in with GitHub App Device Flow (user authorizes at https://github.com/login/device), see the installations and repositories the app can access (read-only, from the first installation in v1), select one, and clone it into a local `projects/<name>` folder (`--depth=1`, project folder name validated) via native `git` — Android's proot Debian guest does the cloning/status/fetch/pull-sync (fast-forward only). iOS/Web show a "not supported" notice; desktop shows the screen but clone/sync are not wired yet (pending desktop WorkspaceController).
- **Repository browsing UI**: connected state shows the account's installations and repositories (private/internal badges are on the data model); clone button appears only after a repository is selected; clone confirmation dialog shows the full repository name and target folder; status/sync display last-sync time and errors.
- **Write operations behind an explicit opt-in** (not reachable from any UI, off by default): `github_commit`, `github_push`, `github_create_pull_request` tool-names are classified OTHER / ASK-ALWAYS and scoped to the repository fullName; `git add` (explicit relative paths only), `git commit` (exactly `-m <msg>`, 1–200 chars, single line) and `git push` (exactly `git push origin HEAD`, no force) are admitted only when `writeOperationsEnabled` is flipped on by a maintainer via `GitHubIntegrationViewModel.setWriteOperationsEnabled(true)`; `git commit`/`push` validate paths and message; pull requests are created only with `pull_requests: write` installation permission (otherwise the UI points at App settings and does nothing). Approval reuses the existing `confirmToolExecution` flow — no new approval UI.
- **Bengali strings** for the new GitHub settings surface.

### Security
- **Tokens stay on the device**: Android `EncryptedSharedPreferences` (`dorai_github_vault_v1`), desktop AES-256-GCM file under `~/.dorai`, iOS Keychain, WasmJs no-op. Tokens are never placed in tool arguments, logs, crash reports or the agent timeline; Ktor debug logging redacts GitHub token literals (`ghp_`, `ghu_`, `ghs_`, `github_pat_`).
- **Fail-closed git policy** (`GitCommandPolicy`): only read-only subcommands in the default surface, dangerous flags rejected everywhere, abbreviated-long-option and short-cluster rejection, `-C <dir>` prefix handling; write subcommands require the explicit opt-in and exact write shapes.
- **Ephemeral credential helper** (desktop): token lives only in an owner-only sidecar for one command, then both files are deleted; the token never appears in argv, env or shell echo. Android guest git operations authenticate anonymously only — authenticated Android git (private repos, push) is not wired until guest-visible credential plumbing and Phase 0 (GitHub App registration) land.
- **No background polling**; refresh happens only on user action.

### Known limitations
- Desktop clone/sync is not functional yet (desktop `WorkspaceController` is a no-op; the desktop CLI layer exists but is unwired).
- Android authenticated git (private repos, push) is not wired yet.
- PR creation requires `Pull requests: Write` on the App registration — off in the default P0 registration.

---

## DORAI AI - Live Chat Streaming (Unreleased)

### Added
- **The assistant's answer now appears as it is written** on the OpenAI-compatible provider family (OpenAI, OpenRouter, Groq, Mistral, Cloudflare and other compatible endpoints, including local Ollama/LM Studio): the text grows in place inside a single reply instead of arriving all at once when generation finishes, so long answers no longer look frozen. The typing caret, follow-the-tail scrolling and "Worked for Xs" timer now track real token growth. Gemini, Anthropic, the Responses-API models (GPT-5 family) and on-device inference still deliver the whole answer in one piece.
- Streamed tool calls are assembled from their fragments, so a tool-using turn renders its text and its tool activity on the same message.

### Changed
- **A failed provider no longer leaves a half-written answer behind:** when the fallback chain moves to the next provider (or the next free-tier target), the partial streamed text is removed before the new answer starts.

### Fixed
- **The newest answer no longer ends up off-screen.** Following the tail is now durable state instead of a scroll-position guess: scrolling up stops the auto-scroll (the jump-to-latest button appears), scrolling back or tapping it resumes, and sending a prompt always follows again. A long reply also stays anchored to its newest text instead of its first line.

### Tests
- New `StreamedAssistantRowTest` covers delta coalescing, stable row identity, single-use finalization and discard.

---

## DORAI AI - Restore Last Chat on Launch Preference (Unreleased)

### Added
- **General → "Restore last chat on launch"** toggle (on by default). With the toggle on, the app keeps its previous behavior of reopening the last active conversation at launch; with it off, the app always starts a fresh empty chat. The underlying `ChatViewModel` restore is skipped while the toggle is off (share and assist cold-starts already bypassed restore and are unaffected).

### Changed
- **Static General settings screen in the fallback path now renders the real `GeneralContent`** (with the new toggle) instead of a hardcoded bilingual mock screen (language pills, haptics/notifications toggles); removed the mock.

### Tests
- New `ChatViewModelTest` assertions for the restore on/off gate, `SettingsViewModelTest` toggle round-trip, `AppSettingsTest` default/round-trip for `restore_last_chat`. Four screenshot goldens re-recorded to the real General surface.

---

## DORAI AI - Chat Agent Activity Flow (Unreleased)

### Changed
- **Chronological agent-activity layout:** the assistant reply renders top→bottom as (1) a subtle "Worked for Xs" indicator, (2) chronological, collapsed-by-default processing accordions (reasoning / "Ran N commands" / "Used N tools"), (3) the final human-readable Markdown answer completely below them. Tool calls awaiting approval or failed tools are always rendered outside the accordion, since the agent turn blocks until the user answers.
- **Contrast & touch targets:** quick-action pills and dynamic-UI options gained ≥48dp touch targets with painted borders and ≥12sp helper text; action row spacing raised; reasoning/activity accordion headers are 48dp button-role controls with expanded/collapsed state descriptions.
- **Typography:** helper labels (mode badge, fallback note, thought-step counter) raised to a readable size.

### Tests
- `ToolEventGrouperTest` covers the "Worked for" span propagation; 3 conversation goldens re-recorded to the collapsed-processing-block layout.

---

## DORAI AI - Provider System Remediation (Unreleased)

Security hardening, provider integrity fixes and storefront cleanup for the LLM provider surface. Not yet released.

### Security
- **SSRF defense-in-depth for `fetch_url`:** refused credentialed URLs (`user:pass@host`), cloud metadata hostnames, and any hostname that resolves to loopback/private/link-local/ULA/CGNAT/reserved/multicast (including IPv4-mapped IPv6 and NAT64-wrapped equivalents); resolved-IP validation re-runs on every redirect hop. Platforms without DNS (iOS, Web) skip the resolved-IP check (documented limitation); literal-IP checks apply everywhere.
- **Debug logs sanitized:** Ktor debug logging (`LogLevel.BODY`) now redacts `Authorization`, `Proxy-Authorization`, `x-goog-api-key`, `x-api-key`, `api-key`, `cookie`, `set-cookie` header values and inline bearer/key tokens before printing.
- **Legacy settings migration is now verify-then-clear:** plaintext legacy keys (API keys, encryption key, email account data and per-account passwords, splinterlands account/posting keys — including per-account keys discovered from account JSON) are copied to the encrypted store, verified against it, and only then removed — per-key, never wholesale — and the migration runs at most once. Previously the plaintext copies lingered indefinitely.
- **Release workflows hardened:** Play Store deploy validates `ref` as an existing `vX.Y.Z` tag reachable from `main` before building; signing secrets flow via `env:` only; decoded keystores/service-account keys are deleted (`if: always()`) after the release jobs finish; the service-account key is validated as JSON after decoding; `release.yml` removes the decoded keystore after preflight validation and after the APK jobs.
- **Android release builds without signing keys now log a loud warning** instead of silently producing a debug-signed "release" artifact.

### Added
- **Honest Local/Cloud classification:** new `ExecutionLocation` on services; OpenAI-Compatible instances are classified Local only for loopback/private/LAN base URLs (default `localhost:11434`), otherwise Cloud; LiteRT is always Local; the home privacy footer, chat model badge, and assistant who-row inherit the classification; free-fallback answers always display Cloud. Malformed base URLs classify as Cloud (never as Local).
- **Assistant who-row shows the active provider's identity icon** (app logo remains the fallback), matching the settings card/picker surfaces.
- **Model catalog honesty:** `OpenRouter` routing ids (`openrouter/auto`, `kilo-auto/free` and the free OpenCode Zen ids) resolve; duplicate-key detection surfaced as `ModelCatalog.duplicateKeys`; lookup prefers the full provider-qualified id.
- **True Local/Cloud badge in chat** now survives conversation restore and tool-only messages.

### Changed
- **Positive status badges are theme-primary instead of `SecondaryText`** (Connected/Active/Ready are distinguishable from Disabled by color; labels and content descriptions remain the primary channel).
- **Mock "AI Providers" screen in the static fallback removed** — it showed invented provider rows with a fake masked key; it now renders the real (empty-state) Services screen for that branch.
- **Unreachable Free-fallback toggle removed** — the only caller passed `showFallbackToggle = false`; the toggle UI, its gated branch and the now-unused localized-string code reference are gone (the repository/setting/action remain live and tested for future product use).
- Provider-related strings extracted to the string catalog (English + Bengali); decorative chevrons/badges no longer double-announce; configured service cards announce expanded/collapsed state; status badges/chips announce their meaning; services overview/groups/empty-state localized.
- **OKF bundles synced:** `docs/knowledge/free-tier/openrouter.md` now lists the runtime 17 OpenRouter free ids (added `openrouter/auto`, `auto`); fictitious `.grok/skills` paths replaced with environment-side command wording; log.md updated.

### Tests
- New: `SsrfAddressGuardTest`, `HttpLogSanitizerTest`, `ProviderLocalityTest` (incl. Ktor localhost-fallback regression), `FreeTierModelsTest`, `SettingsLegacyMigrationTest`, `DnsResolvedHostGuardTest` (desktop), `ToolEventGrouperTest` locality propagation, `ModelCatalogTest` duplicate-key/qualified-id, `FetchUrlHostTest` metadata hosts. 8 intentional screenshot goldens re-recorded (provider icon, badge color, compact "AI Providers" screen); full suite passes.

---

## DORAI AI - Chat UI v2 Polish & A11y Fixes

### Fixed
- **Assistant answer readability:** the assistant message text is now pinned to an explicit content-color token, so it renders bright-on-dark on every theme preset instead of resolving to a dark fallback in some presets/pairings.
- **Mojibake:** removed the garbled "DORAIâ€¦" composer placeholder and matching workspace hint.
- **Scroll behaviour:** no longer force-scrolls to the bottom while the user is reading history; auto-scroll happens only at the bottom or on first load.
- **Keyboard offset:** removed the double `imePadding` stack at the shell and composer layers on Compact.
- **Model fetch/error visibility:** an error banner with retry/dismiss now appears in chat (previously errors were only visible in the legacy UI).
- **Reasoning block label:** collapsed "Thought process" and expanded "Thought Process" unified into one casing; reasoning markers localized.

### Changed
- **Touch targets:** chat icon buttons (new chat, drawer toggle, settings, close, clear, rename, pin/unpin, delete, copy, speaker, regenerate, tool retry/dismiss) now meet the 48dp minimum.
- **Message column:** conversation column capped at 760dp and centered; composer column likewise capped — no more over-wide lines on Large screens.
- **Mode selector:** active mode badge now shown on all width classes (previously hidden on compact); Bengali strings added for the chat surface.
- **History search:** searches both title and current-conversation preview; the preview is shown for the open conversation.
- **Rename flow:** conversation rename action + dialog is reachable from the history list (pin/delete remain).
- **Agent mode:** the selected mode is ViewModel-scoped (instead of screen-local state) and now actually drives the agent loop — Build enables the tool-calling (interactive) loop; Plan/Review stay conversational. Run Plan switches to Build mode and asks the model to proceed with the plan.

### Fixed
- **Legacy quarantine:** `ChatScreen`/`ChatShell` are `@Deprecated` (not deleted); `KEY_UI_V2` flag kept only for compatibility.

### Tests
- Re-recorded the 12 chat/settings/workspace goldens whose renders shifted by the accessibility (48dp) and v2 text-color fixes; full `verifyPaparazziDebug` suite passes.

---

## DORAI AI - Custom Gateway & Model Fetch

### Added
- **Custom Display Name:** The OpenAI-Compatible (custom gateway) config card now accepts an optional per-instance **Display name** (e.g. "My LiteLLM proxy"). The name is persisted per instance, shown in the settings card header, in chat (model picker, composer and top bar), and wherever a configured instance is listed, so multiple gateways stay distinguishable. Export/import carries the name with the other per-instance settings.

### Changed
- **Model Fetch Errors Surface:** For providers exposing a `/models` endpoint (including custom gateways), a failed model-list request now surfaces as a connection/fetch error instead of silently producing an empty selection. The endpoint is no longer reported as "connected with zero models" on a network/auth failure.
- **No Endpoint, No Phantom Fetch:** An OpenAI-Compatible instance with no base URL configured no longer fires a request at the localhost default in Settings; it shows "Enter the custom endpoint's base URL to fetch its models" until a URL (or API key) is entered, after which validation and the model list refresh automatically.
- **Live Rename in Chat:** The chat shell re-reads configured services whenever the destination changes, so a renamed gateway (or updated key/model) appears in chat without an app restart.

### Tests
- Screenshot coverage for the custom gateway card: `settings_providers_custom_gateway_expanded_ogshine` renders the expanded AI Providers screen with the new Display name field for the built-in gateway fixture.

---

---

## DORAI AI - UI v2 Migration

### Added
- **UI v2 Adaptive Shell:** Responsive architecture supporting Compact (<600dp), Medium (600–839dp), Expanded (840–1199dp), and Large (≥1200dp) with bottom navigation bar, 78dp navigation rail, and 224dp permanent drawer.
- **Top Safe Area Insets:** Applied `statusBarsPadding()` across Compact, Medium/Expanded, and Large layouts ensuring edge-to-edge content renders below the system status bar and camera notch.
- **Full Chat UI/UX Migration:**
  - Rich Markdown rendering with syntax-highlighted code blocks, tables, and KaTeX math formulas via `MarkdownContent`.
  - Collapsible reasoning / thinking blocks (`DoraiReasoningBlock`) with step counters and expandable accordions for reasoning models.
  - Multimodal user attachments with inline image preview cards and document chips.
  - Interactive Text-to-Speech (TTS) audio playback action on assistant messages.
  - Multimodal composer attachments strip with file remove badges and system file picker integration.
  - Proactive Heartbeat and pending SMS draft banners at the top of the chat timeline.
  - Conversation management with search, pin/unpin badges, and deletion with undo snackbar.
  - Full text selection enabled via `SelectionContainer` across all messages.
- **Production Connected Wrappers:** Added `DoraiChatScreenConnected` and `DoraiWorkspaceScreenConnected` bridging UI v2 views with production `ChatViewModel` and `WorkspaceViewModel`.
- **UI v2 Reachability Test Suite:** Added `DoraiUiV2ReachabilityTest` covering shell state, backstack routing, all 8 settings categories, rich chat item properties, and workspace tab transitions.

### Fixed
- **Runtime Screen Wiring:** Resolved issue where installed APK bypassed UI v2 screens and displayed legacy or empty mock screens. `App.kt` now directly routes `DoraiAppShell` to live production screens.
- **Settings Multi-Category Wiring:** Connected all 8 settings categories (`General`, `Appearance`, `Agent`, `Providers`, `Tools`, `Mcp`, `Sandbox`, `Account`) to their full production feature panels, including FOSS auto-updater card, GitHub repository links, Debian sandbox controls, and GitHub MCP/skills.
- **Chat Tool Approvals & Dynamic UI:** Added full callback wiring for tool execution approvals (`onAllowOnce`, `onAllowSession`, `onDeny`, `onRetryTool`, `onAskFixTool`), clipboard copying, and dynamic UI form submissions.

### Features Retained
- **100% Feature Continuity:** Preserved all existing capabilities including Memory management, Automation tasks, Voice TTS, FOSS auto-updater card, Third-party Licenses dialog, AI Providers catalog, GitHub Copilot MCP, and Debian Linux sandbox.

---

## DORAI AI - Workspace v2 File Explorer & Real Data

### Added
- **Left-side File Explorer (UI v2 Workspace):** Opening a project now shows the worktree file listing by default — on Compact as a left slide-in pane (open by default, toggleable from the project top bar), on Medium/Expanded/Large as a permanent left panel. The list is the real guest filesystem, with name/path search, filter chips, loading/error/empty states, and git-status badges matching the Git tab. Tapping a file opens it in the Editor pane; tapping a directory navigates.
- **Real Project Cards:** Project list rows show the real guest path, the host-side last-modified date when available, and — for the open project — the real branch and working-tree change count.
- **Workspace New Project dialog:** The previously dead New Project button in the ready screen now opens the create-project dialog.

### Changed
- **Editor pane shows the real buffer:** The v2 Editor tab renders the actual guest file (syntax highlighted) with save/undo support and the real readonly/binary/too-large/unreadable states.
- **Git tab shows real changes:** The v2 Git tab lists the open project's `git status` (staged/unstaged) with stage/unstage/discard actions and a unified-diff bottom sheet per file. Empty states reflect the real status result (clean / not a repository / git missing).
- **No demo content:** The v2 Agent activity panel, Editor fallback and Terminal fallback now show honest empty states instead of hardcoded demo tool cards, demo code, demo `git diff` text, or demo shell output.

### Fixed
- Project cards no longer display invented branch names, update timestamps, or change counts for projects that have never been opened; unknown values are omitted rather than faked.
- The fake "New Project" affordance that did nothing when tapped now works.

---

## DORAI AI - Workspace Editor & Source Control

### Added
- The Workspace **Git** tab now lists the open project's real working-tree changes, read with `git status --porcelain` inside the Debian guest each time the tab is opened. Added, modified, deleted, renamed, untracked, and conflicted paths each get their own badge.
- File rows in the projects drawer show the same git status, so the explorer and the changes list never disagree.

### Changed
- The projects drawer search field now filters the file list as you type, matching names and paths.
- The drawer's "Modified" chip counts files that git actually reports as changed; the "Agent touched" chip and its per-file tag were removed because nothing tracks which files an agent edited.

### Fixed
- Typing in the Workspace editor buffer and pressing Save now writes the file back to the sandbox instead of keeping the edit only in the UI.

---

## DORAI AI - Provider Model Lists

### Changed
- The model picker no longer seeds or merges the hardcoded `Service.defaultModels` catalog for providers that expose a `/models` endpoint. A newly added provider starts with an empty list and is populated from the provider's live response after the key is validated; a failed request shows the fetch error instead of stale bundled ids.
- Providers without a models endpoint (Perplexity and the Free tier) keep their curated list, since there is nothing to fetch.

---

## DORAI AI - Automatic App Updates

### Added
- Added an **Auto download and install** switch to the Android FOSS **App updates** card. When enabled, the app checks for a newer stable release at launch (at most once every four hours), downloads the verified APK in the background, and opens Android's package installer when the download finishes.
- The preference is persisted across launches. Turning the switch on requests install permission if needed and starts an immediate check.
- The downloaded APK still passes the existing SHA-256, package, version-code, and signer-certificate verification before Android's installer is opened.

### Notes
- Android requires the user to confirm every package installation in its own system dialog. The switch automates discovery, download, and opening the installer; it cannot silently bypass the OS confirmation.
- The Play Store flavor keeps its existing behavior (no in-app update installer).

---

## DORAI AI - 2027 Navigation & Library Polish

### Added
- Restructured the chat sidebar: brand header with search, New Chat and New Project primary actions, a "Recent chats" list grouped by date, boxed bottom navigation (Dashboard, Interactive UI, Workspace, Settings), and a logo/version footer.
- Merged Settings → Integrations into Tools and rebuilt it as a three-tab library: Connectors (MCP servers), Skills, and Plugins (native tools). Each tab has a Yours/Discover split, search, and Add actions.
- Added curated "Top connectors" and "Top skills" discovery lists with added-state badges and one-tap add.
- Added a Plugins search field that appears once the native tool list grows past eight entries.
- Centered the empty-state logo and welcome copy, now "Your AI. Your World.", over the premium dark backdrop; quick-start chips wrap instead of scrolling horizontally.

### Changed
- Renamed the Settings overview row from "Integrations" to "Connectors".
- Moved the "Request an integration" entry point to the bottom of the merged Tools page.
- Removed the Splinterlands section from Settings. The runtime, persistence, and battle loop are unchanged, so saved configurations are retained, but there is currently no in-app surface for editing them.

### Removed
- Removed the compact HUD three-dot overflow menu. Settings and Speech are now direct actions, and Back appears only in interactive mode.

### Tests
- Refreshed Paparazzi goldens and all localized store screenshots (phone and tablet) to match the new sidebar, Tools tabs, and empty state.

---

## DORAI AI v2.1.14

### UI/UX & Design System Remediations
- **WCAG AA Contrast Compliance:** Validated and enforced normal text contrast (>= 4.5:1), large text contrast (>= 3.0:1), and UI border contrast (>= 3.0:1) across all 13 built-in theme presets and dynamic custom theme generators.
- **Thought Process Disclosure:** Added modern collapsible reasoning/thinking containers with monospace step indicators for reasoning models (e.g. DeepSeek R1, Gemini Flash Thinking).
- **Streaming Performance:** Optimized Markdown block rendering in chat timeline (`key(index, block)`) eliminating recomposition thrashing during token streaming.
- **Service Readiness:** Added clear "No service configured" fallback and direct "Set up service" navigation in dashboard when services are unconfigured.
- **A11y & Touch Ergonomics:** Enforced minimum 48dp touch targets, semantic content descriptions for icon buttons, and bounded auto-dismissing (6s) error notifications.
- **Responsive Layout:** Added desktop minimum window size (600x480), centered wide-screen composer (capped at 820dp), virtual keyboard IME padding for service selector dialogs, and table cell min-width stability.
- **Branding Consistency:** Unified logo v2 mark usage across application components and synchronized Wasm shell metadata.

### Verification & Testing
- Synchronized Paparazzi screenshot goldens across all active CMP targets.
- Expanded comprehensive contrast and theme engine regression test suites.

---

## DORAI AI v2.1.6 - First DORAI AI Release

### Release identity
- First independently branded DORAI AI release; legacy Kai release tags and binaries remain unchanged.
- Android application ID and namespace: `com.dorai.ai`.
- iOS bundle ID: `com.dorai.ai`.
- Android version code: `217`.

### Included
- DORAI AI product identity, application icons, localized app names, and platform bundle identifiers.
- DORAI-specific updater discovery and release artifact naming; legacy Kai APKs are excluded from DORAI update installation.
- Release workflow validation for semantic version consistency, merged-tag requirements, signing safeguards, and gated distribution channels.
- Website and documentation identify the first DORAI release as pending until the release workflow is explicitly run.

### Distribution notes
- Expected release tag: `v2.1.6`.
- Expected Android artifact prefix: `DORAI-AI-v2.1.6-Android`.
- This changelog entry prepares release notes only; it does not indicate that a GitHub release has been published.

---

## DORAI AI ? IDE-grade Agent Review Experience

### Added
- Added a bounded, line-numbered side-by-side file diff when a tool supplies both before and after snapshots.
- Added explicit fallback states for patch-only, new-content-only, and missing-snapshot file activity; the UI never invents an original version.
- Added per-file session review annotations (Reviewed / Needs changes), clearly separated from applying or reverting filesystem changes.
- Added an observable Agent Run Summary with step, completion, failure, file, terminal, and elapsed-time metrics.

### Safety and limitations
- Review annotations are presentation-only; no filesystem write-back or rollback is implied.
- Diff computation is bounded to 250 lines per side.
- No private provider reasoning, inferred token usage, or inferred cost is exposed.

---

## DORAI AI ? Premium Agent Conversation Experience

### Added
- Added a turn-scoped Agent Activity disclosure for observable tool, MCP, terminal, and file-operation activity.
- Correlated concurrent tool execution and completion by stable tool-call IDs, with legacy name matching only when IDs are absent.
- Added expandable, bounded activity details with secret-pattern redaction and explicit lifecycle status.
- Kept tool approval cards as separate permission gates; collapsing activity never changes runtime execution.
- Removed provider reasoning payloads from visible conversation messages; the timeline presents observable execution rather than private model deliberation.

### Tests
- Added regression coverage for concurrent same-name tool calls, out-of-order completion, failed tool results, and shell command mapping.

---

## DORAI AI ? 2027 Premium AI Agent UI Migration refinements

### Changed
- Added a compact Agent HUD layout for narrow chat workspaces; secondary actions move into an overflow menu while provider/model selection remains reachable.
- Standardized primary chat controls, provider/model picker rows, and assistant message actions on 48dp minimum touch targets.
- Replaced hardcoded Premium Dark welcome-state colors and settings status colors with shared semantic design tokens.
- Reduced-motion mode now renders static agent orbs without starting infinite pulse animations.

### Tests
- Added a compact HUD breakpoint regression test at 720dp.
- Retained existing premium-dark home, conversation, settings, and sandbox screenshot fixtures.

---

## DORAI AI ? One Chat Interface (Interactive UI Merged)

### Added
- The chat header now carries an **interactive UI toggle** (a widgets icon, tinted while the mode is on), and the sidebar's "Interactive UI" entry doubles as the same toggle with its state shown
- The back action (remove the last exchange) sits next to the toggle and appears only while interactive mode is on

### Changed
- Interactive UI is no longer a separate full-screen interface ? it is a mode of the one chat. The dorai-ui screens render inline in the conversation, so the timeline, the composer and the rest of the chat stay available
- Switching the mode no longer starts a new chat or clears the conversation; it changes how the next answer is produced and rendered
- While interactive mode is on, the service picker only lists agentic-capable services and models (as the old interactive screen did)
- The interactive system prompt now tells the model that its screen renders inside the chat and that text outside the dorai-ui fence is shown as ordinary chat text
- A conversation's type follows the mode, so a chat reopens in the mode it was left in

### Removed
- The separate Interactive UI screen with its own top bar, welcome screen, collapsed input pill and full-screen dorai-ui renderer

---

## DORAI AI ? Chat Works Out of the Box (Kilo Gateway + OpenCode)

### Added
- New **Kilo Gateway** provider (`https://api.kilo.ai/api/gateway`) ? one OpenAI-compatible endpoint for every provider, with a free tier that needs no API key (`kilo-auto/free` rotates through the free models)
- A bare install now starts with two keyless providers configured ? **Kilo Gateway** and **OpenCode** ? with their free models pre-selected, so the first message works without adding any service or provider
- The built-in Free tier (Free FAST / Free EXPERT) stays the last-resort fallback behind them

### Changed
- The default provider seeding runs once per install: an install that already has services keeps exactly what it has, and a default provider the user deletes stays deleted (it can be re-added from Settings ? Services)

---

## DORAI AI ? Midnight Theme + Theme Switch Buttons

### Added
- New **Midnight** theme: a deep-navy dark variant (background `#06152E`, surfaces `#0E2A4D`) that keeps the cyan brand accent without going pitch black ? sitting between the neutral Dark theme and the pure-black OLED one
- The theme picker is now a row of **switch buttons** (System -> Light -> Dark -> OLED -> Midnight) instead of a dropdown, so every theme is visible at a glance and switching takes a single tap

### Changed
- `ThemeMode` gains `Midnight`; the stored preference keeps working, and Android system bars use light icons for it like the other dark themes

---

## DORAI AI ? Model Picker in the Chat

### Added
- The chat header shows the model that is loaded: the pill carries the provider icon, the active service and the model it is running
- Tapping the pill opens the models the active service can run (the loaded one highlighted) above the service list, so the AI model can be switched at any time without leaving the chat
- The provider's model list is fetched when the picker opens rather than on every chat start; if the fetch fails, the curated defaults stay selectable
- Picking a model stores it for that service instance ? the next message uses it and the choice survives a restart

---

## DORAI AI ? Settings Split Into Category Tabs

### Added
- Settings is split per feature category again: the features that were stacked inside the General tab now have their own tab each ? **Agent** (soul, memories, scheduled tasks, heartbeat, email, SMS, notifications), **Tools** (native tools, MCP servers, skills), **Integrations** (Splinterlands, integration requests) and **Sandbox** (Linux sandbox)
- The Sandbox tab appears only where the sandbox exists (Android); other platforms no longer show a tab that would open on an empty page
- The Tools tab's "install the sandbox first" prompt now opens the Sandbox tab directly

### Changed
- `SettingsTab` enum: `General, Model, Data, Account` ? `General, Agent, Services, Tools, Integrations, Sandbox, Data, Account` (the former `Model` tab is now `Services`, matching its "Model & API Key" content and the already-translated tab strings)
- General keeps the app-level preferences only: overview, theme, UI scale, dynamic UI, daemon mode, export/import
- Dashboard deep links follow the new tabs: "Set up a service" opens Services, "Tools" opens Tools

---

## DORAI AI ? Desktop Auto-Updater (GitHub Release Check)

### Added
- Desktop auto-update check: on startup the desktop app queries GitHub Releases (`habibbrox2/DoraiAi`, throttled to one check per 4 hours) and shows an "Update Available" dialog with release notes and a Download & Install button when the latest `v2.0.*` release is newer than the running version
- New common updater module (`composeApp/src/*/kotlin/com/inspiredandroid/kai/updater/`): all release-checking logic is platform-independent; platform differences are isolated behind two expect/actual functions ? `httpGet` (desktop/android use `java.net.HttpURLConnection`, wasmJs/iOS use ktor) and `currentPlatformKey` (JVM derives Windows / macOS x64+arm64 / Linux from `os.name`+`os.arch`; web and iOS report `none`)
- Release-asset picker keys on the platform key: `.msi` for Windows, `.dmg` (aarch64-aware) for macOS, `.deb`/`.AppImage`/`.tar.gz` (aarch64-aware) for Linux

### Fixed
- The earlier commonMain interface + platform `actual object` split did not compile (redeclaration + supertype loop); replaced with a single common `AutoUpdater` object and federated expect/actual function implementations

---

## DORAI AI - Premium Chat UI + Code Editor (Plan 7)

### Added
- Chat-first launch: Android and every other platform now open directly into the Chat UI; the Dashboard lives as a sidebar link (brand click)
- New AppSidebar: New Chat button + live search, time-grouped conversation list (Pinned / Today / Yesterday / Previous 7 Days / Previous 30 Days / Older), rename / pin / delete via per-row menu, bottom destinations (Dashboard, Interactive UI, Kai Build on Android, Settings) plus the DORAI AI brand
- ModalNavigationDrawer sidebar on compact windows + permanent 280dp rail on expanded windows
- Redesigned TopBar: drawer toggle, centered model-selector pill, right-side quick actions (sandbox, speech output, settings)
- Redesigned EmptyState: small animated mark, "How can I help you today?" heading, 2x2 quick-start cards with title + subtext and hover glow
- Response action bar under every assistant message: Copy, Thumbs Up/Down (local-only), Regenerate, Read Aloud
- CodeFenceBlock header: language tag + "Copy code" button + "Open in editor" hook
- New CodeEditorScreen: monospace editable editor opened from any chat code block
- Settings restructured to 4 tabs: General, Model and API Key, Data Control, Account and Plan
- Error handling moved from centered ErrorMessage blocks to a top-right toast badge with retry
- Time-based chat history grouping, rename, pin, delete and live title search

### Changed
- SettingsTab enum reduced from 6 to 4 (General, Model, Data, Account)
- Conversation gains isPinned / pinnedAt (defaulted, backward compatible)
- App start destination changed from Dashboard to Home (Chat)

---

## DORAI AI ? Auto-Release Pipeline (Auto Version Bump + GitHub Release)

### Added
- `.github/workflows/auto-release.yml`: on every push to `main`, automatically bumps the DORAI AI version (patch +1 over the latest `v2.0.*` tag), updates `appVersion`/`versionCode` in `gradle/libs.versions.toml`, commits and tags `v2.0.x`, builds the Foss APK + Windows x64 MSI + Windows x64 zip from that tag, and publishes a GitHub Release named `DORAI AI v2.0.x` ? "DORAI AI" is the fixed brand name, only the version changes; assets are named `DORAI-AI-v2.0.x-<platform>.<ext>`
- `.github/workflows/apk.yml`: on-demand Foss APK build (`workflow_dispatch`) usable from the Actions tab without tag pushes or signing secrets

### Changed
- Base `appVersion` in the version catalog realigned from upstream `3.3.0` to the DORAI `2.0.3` line; each auto-release increments it going forward

---

## DORAI AI ? Desktop Shell: `sleep` Allowed + Cancellation Test Fix

### Added
- Desktop `ShellCommandTool` allowlist now includes `sleep` ? side-effect-free wait primitive, bounded by the existing per-call timeout (Android shell is unaffected: it runs inside the proot sandbox)

### Fixed
- `ShellCommandToolTest > cancelling a running command kills it` failed on CI: the P0-B test rewrite used `sleep 3`, which the fail-closed allowlist rejects, so the command never started and nothing was ever cancelled (the test body skips on Windows, so it only surfaced on the Linux runner)
- Cancellation test now runs `sleep 27.314` (odd duration, collision-proof) and verifies via `ProcessHandle` that the process is actually killed after cancellation, replacing the vestigial marker-file check that nothing ever created

---

## DORAI AI ? Home Dashboard UI Upgrade (Plan 6)

### Added
- Dashboard launch route (`Dashboard`) with 3-tab navigation (Dashboard/Chat/Settings) and typed `Settings(tab)` / `Build(project)` destinations
- `ui/dashboard/` ? `DashboardModels.kt` (read-only aggregation + `buildDashboardUiState`, `DESIGN_STARTER_PROMPT`) and `DashboardScreen.kt` (header, readiness, responsive primary action grid 1/2/4 columns, recent conversations/projects with resume + View all)
- Dashboard cards: Chat (primary), Kai Build (Android-only, honest disabled note), Design (opens chat with guided design prefill), Tools (Settings/Tools)
- `ChatViewModel.startDesignChat` + `loadConversation` made public for dashboard resume; `TopBar` gets mobile Home button; `SettingsScreen`/`KaiBuildScreen` accept initial tab/project

### Changed
- App start destination moved to Dashboard; Chat/Settings navigation anchors to Dashboard
- `Home` route preserved as chat workspace (reachable from Dashboard/Chat tab)

---

## DORAI AI - Services & Sponsors UI Upgrade (Plan 5)

### Added

- Compact Services overview card: configured-service count with attention/checking/connected status badge; empty state when nothing is configured.
- Grouped add-service sheet (Hosted providers / On-device / Custom endpoints) with a setup-requirement summary on every provider row.
- `SupportProfile` model + mapper: DORAI owner profile first (stable id, repository identity, local branded avatar), then current and past sponsors with the previous deduplication.
- `SupportSection` with `OwnerProfileCard`, `SponsorGrid`, and width-tiered collapse (2/4/6 tiles at 600/900dp) plus a `Show all sponsors` / `Collapse sponsors` toggle that announces the hidden count.
- `SupportLinks` configuration block owning every support-area URL.
- Drag handle now meets the 48dp touch-target minimum around its 24dp glyph.

### Changed

- Service card headers: color-only connection dot replaced with a text status badge covering every connection state; expand/reorder behavior unchanged.
- Sponsor area rebuilt: owner card always first (never hidden by the collapsed limit), grid above the sponsorship/partnership CTAs, animated expand/collapse that preserves toggle focus, no redundant toggle for short lists, link cards carry accessible names and stay informational without a destination.

## DORAI AI ? Tools & Sandbox Workspace UI Upgrade (Plan 4)

### Added

- Shared operational components (`ui/components/OperationalComponents.kt`): status vocabulary, status color ramp, `StatusBadge` (text + color), `CapabilityCountCard`, `CollectionEmptyState`.
- Tools Overview header summarizing enabled native tools, connected MCP servers, and installed skills with attention/needs-setup badges (responsive 3/2/1 columns).
- Native tools search/filter when 8+ tools are present, with localized-name matching and a no-match empty state.
- Sandbox setup readiness flow: distribution from state, Needs setup / Installing / Failed badge, storage + session expectations, and a "Retry setup" action after failure.
- Common sandbox workspace header (semantic heading + Ready badge) shared by Terminal, Files, and Packages.
- Explicit confirmation before skill uninstall (previously a single tap removed the skill).

### Changed

- MCP server cards: raw status dot replaced with a text status badge including `Disabled` and `Not checked` states; dedicated empty state for the server list.
- Sandbox subtab selector: only the selected tab is primary + semibold (previously all three tabs were primary, hiding the active tab); `Role.Tab`/selected semantics on tabs and session chips.
- Session chips get a larger touch target; skills and native tools use dedicated empty-state cards instead of plain text.

## DORAI AI ? Kai Build Workspace UI Upgrade (Plan 3)

### Added

- `BuildDesignTokens`: Build-specific spacing, shapes, status kinds/colors, terminal surfaces, environment/step status mapping.
- Reusable `BuildStatusPill` and `BuildProgressBlock` components for session tabs, headers, and setup progress.
- Project hub environment readiness header with next-action guidance and semantic section headings.
- Dedicated Agent Manager card, split from the system info card.
- Step-based setup surface (Choose agents ? Install ? Verify) derived from `KaiBuildUiState`, with an actionable Retry on failure.
- File-scoped Find/Replace panel with live match count and Replace-all confirmation (flows through the existing save/dirty model).
- Centralized `buildWorkspaceTier(width)` policy: hub content centers on expanded windows, full-bleed on compact/medium.

### Changed

- Session bar is project-aware: names the active project and shows a Running/Idle status pill next to the session title.
- Session tabs expose `Role.Tab`, selection state, and announce running/idle to screen readers.
- Project title capped at 180 dp so narrow windows keep the session strip usable.
- Setup title/description folded into the environment header and step guide.

## DORAI AI ? Settings UI Upgrade (Plan 2)

### Added

- `SettingsDesignTokens` and `SettingsSectionHeader` with semantic heading semantics.
- `SettingsNavigation` rail + `SettingsDetailPane` extraction; rail + detail pane on expanded (<=840dp), horizontal selector on compact.
- Overview card atop General tab: `SettingsStatusRow`, `SettingsKeyValueRow`, `SettingsOverview` (sandbox install status).
- `SettingsConfirmDialog` (destructive confirmations) and `SettingsStatusBanner` (Info/Progress/Warning/Error) for skills and MCP errors.

### Changed

- `SettingsScreenContent` restructured with `BoxWithConstraints`; `BottomInfo` made internal.
- MCP server removal now confirms first; skill install/browse failures surface as banners instead of bare text.
- Delete-downloaded-model icon button gained a content description.

## DORAI AI ? Chat UI Upgrade (Phase 1 & 2) + Follow-up Suggestions

### Added
- **Phase 1 ? Foundations**: `ChatDesignTokens.kt` (spacing, shapes, surface/status colors), plus reusable `StatusPill`, `PanelHeading`, `ChatActionButton` components.
- **Phase 2 ? Responsive shell**: `ChatAdaptivePolicy` (compact/medium/expanded), `ChatShell` (adaptive rail/main/side-panel/overlays), `ChatTimeline`, `ChatComposer`, `ConversationRail` extractions; conversation column centered at 760dp.
- **Follow-up suggestions**: after a successful (non-interactive) reply, the app generates 3 short follow-up questions via a silent LLM call and shows them as chips below the last assistant message; tapping a chip sends it. State is cleared on new chat / conversation load / error.

### Changed
- `ChatScreen.kt` ? `ChatModeScreen` now composes through `ChatShell` (header/rail/main/overlays) instead of a monolithic Box/Column.
- `ChatViewModel.kt` ? follow-up generation + clearing wired into ask/clearHistory/startNewChat/loadConversation.
- `ChatUiState.kt` ? added `followUpSuggestions: ImmutableList<String>`.

---

## DORAI AI ? Full Logo & Animation Replacement

### Changed
- Replaced every remaining upstream Kai logo/animation with DORAI artwork:
  - **In-app animated logo** (`LogoAnimation.kt`) ? the old two purple orbs are now the Brox BK monogram easing apart and reuniting, theme-adaptive (cyan + navy on light, cyan + supporting blue on dark).
  - **Android adaptive icon** ? `composeApp` launcher foreground, background and a new `ic_launcher_monochrome.xml` now use the Brox BK monogram (matching `androidApp`).
  - **Android notification icon** (`ic_notification.xml`) ? purple rings replaced with a white BK alpha mask.
  - **Web app icon** (`site/img/icon.svg`, `composeApp/.../wasmJsMain/resources/icon.svg`) ? Brox navy/cyan app icon.
  - **Favicons** (`site/img/favicon.ico`, wasm `favicon.ico`) ? regenerated as multi-size Brox ICOs (16/32/48/64).
  - **Apple touch icon** (`apple-icon-180x180.png`) ? regenerated from the Brox 384px export.
  - **Animated hero logo** (`site/img/logo_animation.svg`) ? Brox BK symbol with a float + sparkle animation.
  - **Animated logo GIF** (`site/img/logo_animation.gif`) ? replaced with the Brox animation (`Brox_Kai_v2_logo_animation.gif`), used by the README; the loose root copy was removed.
- `site/index.html` ? hero mascot alt text updated to "DORAI animated logo".

---

## DORAI AI ? OpenCode Console Integration

### Added
- Added OpenCode Console default models (20 curated coding models)
  - Free models: Big Pickle, GLM-5.3-Flash Free, DeepSeek V4.1 Flash Free, MiniMax M3.1 Flash Free, Beam Free, LL Alpha Free, Exo Free, MiMo-V2.6-Flash Free, Nemotron 3 Ultra Free, Nemotron 3.5 Lightning Free
  - Paid models: Claude Sonnet 5.5, Claude Opus 5.5, GPT 6.1 Sol, GPT 5.5, GPT 5.3 Codex, Grok 4.7, GLM 5.3, Kimi K2.7 Code, DeepSeek V4 Pro, Qwen3.8 Max
- Default model set to `big-pickle` (free)
- Updated API key URL to point to OpenCode Console dashboard (`opencode.ai/console`)
- API key is now **optional** for OpenCode:
  - **With key** ? authenticated access to Console models (free + paid)
  - **Without key** ? requests carry no `Authorization` header; the public model catalog still loads, and a rejected chat request (OpenCode answers 403) is reported as an invalid-key error so the built-in **Free** service fallback takes over automatically.
- OpenCode 403 responses now map to "invalid API key" instead of a misleading "content moderation" error.

### Changed
- `Service.kt` ? OpenCode service enhanced with curated model list, Console links, and optional API key (`requiresApiKey = false`, `supportsOptionalApiKey = true`)
- `Requests.kt` ? OpenCode 403 ? invalid-key error mapping

---

## DORAI AI ? P0-B Shell Hardening

### Security
- Replaced blocklist with allowlist (fail-closed) for shell command execution
- Eliminated shell interpretation ? commands run directly via ProcessBuilder (no `sh -c`/`cmd /c`)
- Added shell metacharacter rejection (`;`, `|`, `&`, `$`, backticks, etc.)
- Added path traversal protection (`..` in arguments)
- Added dangerous flag rejection (`-rf`, `-R`, `--force`, etc.)
- Extended blocked environment variables (added JAVA_TOOL_OPTIONS, PYTHONSTARTUP, NODE_OPTIONS, etc.)
- Added working directory restriction (must be within project root)
- Added environment variable value validation (reject metacharacters)

### Changed
- `ShellCommandTool.kt` ? complete rewrite: allowlist-based, no shell interpretation
- Tool description updated to reflect no pipes/redirections/chaining support

### Limitations
- Pipes (`|`), redirections (`>`), chaining (`&&`) no longer supported
- Allowlist may need expansion for additional use cases

---

## DORAI AI ? P0-A Security Hardening

### Security
- Restricted Android cleartext HTTP to localhost only (was globally permitted)
- Disabled Android backup (`allowBackup=false`) ? prevents ADB/cloud data exfiltration
- Tightened backup/data-extraction rules to exclude conversation DB and encrypted settings
- Added MCP SSRF protection: URL validation (scheme, IP range, credentials, encoding), connection-time re-validation, redirect validation, header sanitization
- Disabled lenient JSON parsing in MCP client
- Added SHA-256 checksum generation and verification for release artifacts
- Documented protection boundaries and limitations in SECURITY.md

### Added
- `McpUrlValidator` ? KMP-compatible URL validator (Android, Desktop, iOS, Web)
- `McpUrlValidatorTest` ? 28 unit test cases
- `docs/P0-A-IMPLEMENTATION-PLAN.md` ? detailed implementation plan

### Changed
- `McpClient` ? fail-fast URL validation, manual redirect handling (max 3 hops)
- `McpServerManager` ? validates URLs before adding/connecting servers
- Platform files ? `resolveHostAddresses` expect/actual (JVM: InetAddress, iOS/Web: fail closed)

### Limitations (documented, not guarantees)
- DNS rebinding TOCTOU window exists
- Proxy bypass possible
- No connection-time IP pinning (Ktor limitation)
- Unsigned checksums (integrity only)

---

## v3.3.0 ? 2026-10-02

### Features
- Add Requesty and 1min.AI as providers

### Fixes
- Keep chat history intact when compaction fails or a tool loop trims context
- Back up an unreadable settings file instead of overwriting it, and make desktop settings writes atomic
- Fix Debian sandbox installs in Android work profiles and cloned apps
- Detect notification listener access on Android 8.0
- Stop or time out shell commands instead of leaving them running in the background
- Cancelling a request no longer shows up as an error
- Rename MCP tools whose names clash with another tool
- Accept OpenAI-compatible base URLs pasted with a trailing /chat/completions
- Show the provider's message for request-too-large errors on non-image requests
- Route GPT-5.6 and newer model families to the Responses API
- Send the OpenCode session header to every opencode.ai OpenAI-compatible instance
- Treat empty scheduled-task triggers and null tool arguments as absent
- Fire dorai-ui countdown callbacks only from interactive messages
- Fix a stray %% in the Kai Build download label
- Verify email TLS hostnames and reject line breaks in IMAP/SMTP arguments
- Check every fetch_url redirect hop and cap the bytes it reads
- Limit open_url file:// links on Android to the sandbox's /root

### Improvements
- Don't retry provider errors that a retry can't fix
- Return tool errors as structured JSON
- Refresh model catalog Elo scores
- Upgrade dependencies

## v3.2.0 ? 2026-09-07

### Features
- Support OpenAI's Responses API for GPT-5.6 models
- Add LFM2.5 1.2B Instruct to the on-device model catalog

### Fixes
- Cap stored message size so a large attachment can't brick the app
- Fix the Debian sandbox install failing on the base package step
- Send the OpenCode session header so OpenCode Zen no longer rejects requests

### Improvements
- Let on-device models declare their own tool support and sampling defaults, so models without a tool template no longer invent answers
- Refresh the on-device catalog: updated gemma-4-12b-it build with vision, audio, and multi-token prediction
- Refresh model catalog Elo scores
- Upgrade dependencies

## v3.1.0 ? 2026-08-30

### Features
- Receive shared text from the Android share sheet
- Add Debian as a sandbox distribution, shared with Kai Build
- Add rename and delete for Kai Build projects
- Add custom model entry for OpenAI-compatible services
- Add an optional API key field when adding the Jina MCP server

### Fixes
- Fix model catalog context windows across major families

### Improvements
- Respect the system text-to-speech engine and voice
- Improve accessibility
- Improve PTY rendering
- Refresh model catalog Elo scores, free-tier models, and popular MCP servers
- Unify the platform, tool-layer, persistence, provider, and permission abstractions
- Unify the staggered settings layouts
- Upgrade iOS LiteRT-LM to 0.15.0
- Migrate deprecated Gradle DSL and clipboard APIs
- Add missing localizations
- Upgrade dependencies

## v3.0.0 ? 2026-08-02

### Features
- Add Claude Code, Grok, and OpenCode support via the Debian Linux terminal
- Add checksum verification for local model downloads

### Improvements
- Improve Linux filesystem file handling
- Refresh open files when switching to a Linux system tab
- Upgrade dependencies

## v2.9.0 ? 2026-07-25

### Features
- Add free-tier service suggestions when a rate limit is reached
- Add custom local model `.litert` import support

### Improvements
- Refresh popular MCP server list
- Update model catalog Elo scores
- Upgrade dependencies

## v2.8.3 ? 2026-07-21

### Features
- Add Perplexity as a service
- Add AI Horde as a service

### Fixes
- Align socket timeout with chat request timeout

### Improvements
- Improve Alpine package search ranking
- Upgrade Alpine sandbox to 3.22
- Prefer requestScrollToItem for list scrolling
- Update model catalog Elo scores

## v2.8.2 ? 2026-07-18

### Features
- Save copies of sent emails to the IMAP Sent folder
- Resolve email tool accounts by id, address, or single account

### Fixes
- Fix heartbeat CompletionHandlerException
- Add java.sql to desktop jlink runtime modules

### Improvements
- Increase chat request timeout to 180 seconds
- Auto-install bash package during Alpine Linux install
- Update model catalog (Kimi K3, Inkling, Elo scores)
- Upgrade dependencies

## v2.8.1 ? 2026-07-11

### Features
- Migrate conversations to SQL storage
- Add Gemma 4 12B local model and missing catalog entries

### Fixes
- Fix Android 17 local network permission request

### Improvements
- Improve service and tool calling
- Add Atlas Cloud UTM tracking
- Update model catalog
- Upgrade dependencies

## v2.8.0 ? 2026-07-01

### Features
- Add per-model image-capability gating so text-only models no longer offer image input
- Add Android assist-gesture launch support

### Fixes
- Reload conversations after importing settings
- Restore the Fetch URL tool in the Android tool list

### Improvements
- Group sponsored sections in the UI
- More prominent interactive loading animation
- Update model catalog (including DeepSeek V4 1M context window)
- Additional localizations
- Upgrade dependencies

## v2.7.1 ? 2026-06-15

### Improvements
- Upgrade dependencies
- Update model catalog

## v2.7.0 ? 2026-05-31

### Features
- Introduce Skills ? packaged, reusable instructions the assistant can load on demand via the Linux sandbox, plus a built-in "create-skill" workflow to author your own
- Add Atlas Cloud as a featured provider

### Fixes
- Fix text selection in chat list
- Fix heartbeat active hours slider ranges
- Fix heartbeat Linux sandbox running in current conversation
- Fix terminal scroll out of bounds
- Fix OpenAI-compatible model loading when only the default URL is set

### Improvements
- Upgrade dependencies
- Update model catalog and scores
- Improve free endpoint DTO sanitization
- Extend API error mapping

## v2.6.3 ? 2026-05-24

### Fixes
- Fixed quick service toggle not scaling properly with many services
- Fixed content serialization issue
- Fixed dynamic system prompt builder incorrectly handling deactivated tools and agent settings
- Strip orphaned tool calls left over from prior interruptions

### Improvements
- Updated model catalog

## v2.6.2 ? 2026-05-21

### Features
- Parse inline tool calls

### Fixes
- Fix multiple service deletes in a row
- Fix DeepSeek thinking-mode tool calls returning reasoning_content error
- Improve fallback error handling

### Improvements
- Improve model sorting
- Update model catalog
- Upgrade SDKs and LiteRT
- Compose performance improvements
- Internal code cleanup

## v2.6.1 ? 2026-05-17

### Features
- Collapsable reasoning content UI with cleaned-up handling across services

### Improvements
- Improve UI performance
- Add missing localizations
- Upgrade dependencies
- Internal code cleanup

## v2.6.0 ? 2026-05-15

### Features
- Allow user-installed certificates
- Desktop AVX2+ CPU compatibility check

### Fixes
- Fix MCP tools serializing array and object arguments as strings
- Fix settings crash on Android 8.0
- Fix potential terminal sheet crashes
- Surface real errors from OpenAI-compatible chat

### Improvements
- Tolerate non-standard MCP tool schemas
- Improve SSH Alpine integration (requires re-installing Alpine)
- Add honesty, tool-use, and acting sections to chat system prompt
- Make chat input persistent between screen changes
- Upgrade Compose Multiplatform and SDKs
- Update model catalog
- Remove iOS x64 target and deprecated APIs

## v2.5.1 ? 2026-05-13

### Fixes
- Fix terminal auto-scroll crash

### Improvements
- Update model catalog
- Add dynamic model endpoint for LongCat service
- Add Ollama Gemma4 to no-tool model list
- Support .gsc files and increase upload size limit to 200KB
- Show history delete undo inside modal
- Upgrade dependencies (coroutines, datetime, and others)

## v2.5.0 ? 2026-05-06

### Features
- Theme switcher and native desktop UI scale settings
- Linux sandbox icon flash on shell execution

### Fixes
- Fix DeepSeek tool execution
- Fix sponsors display

### Improvements
- Improve Ollama error handling
- Compose performance improvements
- Add missing localizations
- Upgrade LiteRT
- Upgrade SDKs

## v2.4.1 ? 2026-05-02

### Features
- Persistent terminal sessions in conversations
- Notification read integration

### Fixes
- Fix Linux sandbox initialization
- Fix Linux sandbox cd command

### Improvements
- Show detailed package upgrade info in Linux sandbox
- Show byte size for files smaller than 1 KB
- Material 3 search inputs
- Update model catalog and scores
- Upgrade SDKs

## v2.4.0 ? 2026-04-28

### Features
- Linux sandbox file browser with file and folder actions
- Linux sandbox package manager
- Open files from the Linux sandbox
- Themed icon support
- Section selection dialog for exports

### Fixes
- Fix markdown regex parsing

### Improvements
- Persist Linux sandbox UI state during the session

## v2.3.4 ? 2026-04-26

### Fixes
- Fix calendar event time zone issue
- Fix conversation restore ANR
- Fix iOS quick toggle hidden behind soft keyboard
- Fix escape character handling

### Improvements
- Add scheduled task details
- Simplify memory, schedule, and heartbeat tool toggles
- Improve email polling and simplify email/SMS tool UI
- Add missing model info
- Migrate deprecated APIs

## v2.3.3 ? 2026-04-24

### Features
- Add SMS read and send support to FOSS build
- Add iOS and desktop notifications support
- Add Z.AI Coding Plan service

### Fixes
- Fix out-of-memory crash
- Fix crash when deleting models
- Fix crash when downloading models while offline

### Improvements
- Improve IMAP tool
- Use existing catalog for model context window info
- Upgrade Kotlin
- Add missing models

## v2.3.2 ? 2026-04-23

### Fixes
- Fix dorai-ui rendering and restore issues

### Improvements
- Make OLED dark mode opt-in
- Improve heartbeat, email polling, and task scheduling reliability
- Prevent incompatible models from being used in interactive chat and automations
- Align general grid UI
- Align localizations
- Upgrade dependencies

## v2.3.1 ? 2026-04-22

### Features
- Add LaTeX code block rendering

### Improvements
- Pure black backgrounds in dark mode for OLED power savings
- Improve fallback status UI
- Add Mistral 3.5 models to catalog
- Update Kimi model catalog (add K2.6, refresh Arena scores)
- Update free API endpoint
- Upgrade dependencies

## v2.3.0 ? 2026-04-20

### Features
- Custom math formula rendering
- Expandable and editable memories
- Zoom and pan for attached images

### Fixes
- Hide thinking section on chat restart

### Improvements
- Add Alpine Linux package download mirrors
- Pulse animation on interactive chat stop button
- Add missing model info
- Performance improvements
- Replace ABI filter with runtime check
- Remove unnecessary Android version checks

## v2.2.1 ? 2026-04-20

### Features
- Add local Qwen 3 0.6B model

### Improvements
- Upgrade LiteRT
- Defensive desktop ProGuard rules

## v2.2.0 ? 2026-04-16

### Features
- Add custom markdown parser
- Preserve previous Kai UI output and allow editing

### Improvements
- Pulse animation on stop button
- Add missing localizations
- Add missing model info

## v2.1.5 ? 2026-04-15

### Features
- Add copy-to-clipboard button for code blocks

### Fixes
- Filter out Gemini API responses marked as internal thoughts
- Add size safeguards for PDF and image compression failures

### Improvements
- Highlight currently selected model in model picker
- Improve Linux sandbox interactive interface
- Adjust Gemma 4 device performance expectations
- Upgrade BouncyCastle and remove version checker dependency
- UI polish and alignment

## v2.1.4 ? 2026-04-12

### Features
- Add fast and expert mode toggles for free services
- Add Windows menu entry
- Enrich model catalog with arena scores, release dates, and context windows
- Support multi-file upload for attachments
- Enable simple tools for local Gemma 4
- Select TTS voice based on current locale

### Improvements
- Improve conversation restore on app restart and settings navigation
- Upgrade SDKs

## v2.1.2 ? 2026-04-09

### Features
- Add local model context configuration

### Fixes
- Fix undo snackbars not executing after leaving screen

### Improvements
- Improve filetype checks for file attachments

## v2.1.1 ? 2026-04-09

### Fixes
- Fix crash when opening Settings on devices without downloaded local models

## v2.1.0 ? 2026-04-08

### Features
- Add support for local Gemma 4 via LiteRT
- Add PublicAI, Fireworks, DeepInfra, and OpenCode services

### Improvements
- Add missing localizations

## v2.0.3 ? 2026-04-07

### Fixes
- Fix Bouncycastle and Coil proguard rules on desktop

### Improvements
- Simplify and improve chat input UI
- Extract reusable UI components
- Improve heartbeat error messages

## v2.0.2 ? 2026-04-06

### Fixes
- Fix toolbar vertical padding
- Fix removeLast crash

### Improvements
- Add custom JSON parser for more flexible and resilient UI parsing
- Improve desktop release performance and reduce app size
- Simplify UI spacing by handling padding definitions locally
- General UI/UX improvements

## v2.0.1 ? 2026-04-04

### Features
- Add possibility to define service for heartbeat
- Add additional MCP servers

### Improvements
- Improve dorai-ui prompt and split-block parsing
- Replace Lottie animation with custom implementation
- Improve tool use UI
- Add missing localizations
- Clean up implementations

## v2.0.0 ? 2026-04-03

### Features
- Add experimental interactive UI screen
- Add conversation import and export
- Add dedicated GitHub Pages screen
- Add custom MCP server headers

### Improvements
- Improve Kai UI parsing
- Use Gemini header auth instead of URL parameter

## v1.12.3 ? 2026-04-02

### Features
- Add Dynamic UI ? AI can create interactive buttons, forms, and cards inline in chat responses (opt-out in Settings)

### Fixes
- Fix hover pointer not showing over text inside buttons on desktop and web

## v1.12.2 ? 2026-03-31

### Fixes
- Fix daemon start crash
- Fix heartbeat thinking in wrong conversation
- Fix leaked path in sandbox

### Improvements
- Improve shell integrations
- Add uninstall linux sandbox confirmation dialog
- Add NDK 29 requirement and reproducible build flags

## v1.12.1 ? 2026-03-29

### Improvements
- Replace standard tabs with custom piles
- Build proot from source

## v1.12.0 ? 2026-03-29

### Features
- Add Linux sandbox for Android shell commands with ANSI terminal output
- Add AiHubMix service
- Add desktop scrollbars

### Fixes
- Fix macOS window application appearance

### Improvements
- Improve settings UI
- Migrate extended icons to custom icons
- Upgrade Ktor and add slf4j-nop

## v1.11.2 ? 2026-03-25

### Improvements
- Improve heartbeat reliability with previous response context and failure logging
- Add proper context compaction for conversations
- Upgrade SDKs

## v1.11.1 ? 2026-03-24

### Fixes
- Fix crash when restoring encrypted backups

## v1.11.0 ? 2026-03-24

### Features
- Add skills feature
- Support text and PDF file attachments
- Add minimax and zai services
- Show LLM model performance ranking for Splinterlands auto battle
- Add undo snackbars to revert deletions
- Replace re-order arrows with drag and drop

### Improvements
- Move conversations to secure platform-specific encrypted storage
- Replace location-from-IP tool with HTTPS-secured alternative
- Improve UI performance
- Add missing localizations and hover pointers
- Upgrade Compose Multiplatform, Lifecycle, and Spotless SDKs

## v1.10.0 ? 2026-03-18

### Features
- Add Splinterlands LLM auto-battle integration
- Add web loading spinner
- List sponsors on free tier UI
- Add bottom scroll pointer icon and rounded corners stop icon

### Fixes
- Fix heartbeat and tasks handling
- Fix icon button sizes

### Improvements
- Upgrade Kotlin to 2.3.20
- Upgrade Koin

## v1.9.8 ? 2026-03-15

### Features
- Add scroll to bottom button
- Show selected model on service rows in settings

### Fixes
- Fix model filter

### Improvements
- Improve startup time and migrate deprecated APIs
- Improve performance

## v1.9.7 ? 2026-03-14

### Features
- Add HuggingFace, LongCat, Moonshot, Together and Venice service support

### Improvements
- Sort services in modal alphabetically
- Clean up service quick select and history modal UI

## v1.9.6 ? 2026-03-14

### Features
- Add chat histories and separate heartbeat chat

### Improvements
- Remove hard-coded /v1 path in OpenAI-compatible service setup

## v1.9.5 ? 2026-03-12

### Features
- Add quick service toggle
- Add delete buttons to completed tasks

### Fixes
- Fix reoccurring tasks on fail
- Strip markers in the content field
- Rename daemon notification title

### Improvements
- Improve input layout arrangements
- Migrate project structure for AGP 9 requirements
- Upgrade SDKs

## v1.9.4 ? 2026-03-11

### Features
- Add option to reset modified heartbeat
- Add button to cancel ongoing requests
- Add search input to model modal

### Fixes
- Fix settings import parsing
- Fix MCP server tool definitions
- Add missing UI pointer

## v1.9.3 ? 2026-03-11

### Features
- Add confirmation dialog for settings import

### Fixes
- Fix silent import error

### Improvements
- Replace tasks and email icons with trash can icon

## v1.9.2 ? 2026-03-10

### Features
- Add Anthropic service support

### Fixes
- Fix duplicate model key crash in models modal

## v1.9.1 ? 2026-03-10

### Features
- Add settings import and export

### Fixes
- Fix deprecated APIs and unused code
- Gracefully catch daemon service exceptions

## v1.9.0 ? 2026-03-09

### Features
- Add MCP server support

### Fixes
- Fix dark mode text colors
- Fix broken translations

### Improvements
- Cleaner services box UI
- Show tool usage in single row with minimum display duration
- Remove unused strings

## v1.8.2 ? 2026-03-08

### Features
- Add file:// URL opening support
- Add URL-opening tool
- Add attached image previews in chat UI
- Add configurable active hours for heartbeat
- Add localizations

### Improvements
- Smoother animations and loading indicators
- Remove unused code and simplify internals
- Add docs link to app settings

## v1.8.1 ? 2026-03-07

### Features
- Add Ollama Cloud and Cerebras as AI services
- Add UI scale option for desktop
- Preselect Kimi K2.5 model if available
- Add localizations

### Fixes
- Fix service modal scroll behavior
- Fix old service migration deletion
- Fix iOS image scaling

### Improvements
- Separate screen navigation for web/desktop and mobile
- Set Linux default font size to 110%
- Reorder tools list
- Upgrade SDKs

## v1.8.0 ? 2026-03-05

### Features
- Add multi-service support with fallback logic
- Add image upload support for all services
- Add email plugin

### Improvements
- Improve agentic retry and timeout logic
- Normalize content for text-to-speech
- Upgrade Ktor

## v1.7.11 ? 2026-03-04

### Features
- Add heartbeat monitoring
- Show OpenAI-compatible URL endpoint in settings

### Fixes
- Fix clear icon on desktop and web

### Improvements
- Improve daemon reliability
- Make full toggle UI clickable
- Improve settings UI on large screens
- Add OS to user agent string
- Clean up tool logic
- Migrate deprecated datetime APIs
- Remove unused code and improve performance

## v1.7.10 ? 2026-03-03

### Features
- Add AUR and Snap packaging
- Add Arch Linux install instructions

### Improvements
- Update SHA for Flathub

## v1.7.9 ? 2026-03-03

### Features
- Add search tool with support for more providers
- Add task scheduling and background daemon

### Improvements
- Remove cancelled tasks from task list
- Fix string formatting

## v1.7.8 ? 2026-03-02

### Features
- Add memory system for persistent context
- Add memory toggle in settings
- Add shell command execution for desktop and Android
- Add re-generate button on last bot response

### Fixes
- Fix scroll width issue
- Fix tests

### Improvements
- Remove topics feature
- Update localizations
- Upgrade SDKs

## v1.7.7 ? 2026-02-26

### Fixes
- Fix RTL UI layout issues

### Improvements
- Add locale configurations for iOS and Android
- Add new store localizations
- Improve performance

## v1.7.6 ? 2026-02-25

### Features
- Add identity.md settings for customizable AI persona
- Add tablet screenshots

### Fixes
- Fix unit test
- Fix minimum tablet screenshot size

## v1.7.5 ? 2026-02-24

### Features
- Add OpenClaw service support
- Add missing localizations

### Fixes
- Fix unit tests

## v1.7.4 ? 2026-02-24

### Features
- Add Android alarm tool

### Improvements
- Disable topics by default
- Disable tools for certain models
- Permit cleartext traffic for local network access

## v1.7.3 ? 2026-02-21

### Features
- Make topics optional

### Fixes
- Fix wrong version code
- Fix topic UI issues
- Fix web dark mode background

### Improvements
- Upgrade Compottie animation library
- Unify and simplify service logic

## v1.7.2 ? 2026-02-19

### Features
- Add Flathub packaging and Linux tarball release asset

### Fixes
- Fix race condition in service selection

## v1.7.1 ? 2026-02-19

### Features
- Add topic headers
- Add content localizations
- Add automated Play Store screenshot localization

### Improvements
- Remove SMS permissions to pass Play Store review

## v1.7.0 ? 2026-02-16

### Features
- Add explore screen with curated topics
- Add space topic
- Add input clear icons

### Fixes
- Fix color scheme background
- Fix outdated models endpoint
- Fix topics padding

### Improvements
- Simplify JSON parsing
- Adjust markdown header font sizes
- Upgrade SDKs

## v1.6.2 ? 2026-02-11

### Improvements
- Add missing ProGuard rules
- Separate FOSS and Play Store build flavors

## v1.6.1 ? 2026-02-11

### Features
- Add Nvidia as an AI service

### Improvements
- Always show privacy info
- Upgrade SDKs

## v1.6.0 ? 2026-02-03

### Features
- Add tools/skills support for AI-driven actions
- Add SMS tool
- Add local conversation history storage
- Add markdown image loading support

### Fixes
- Fix generic TTS exception handling
- Fix encryption warning
- Fix deprecated edge-to-edge behavior

### Improvements
- Use multiplatform time for conversation history
- Upgrade date dependency

## v1.5.2 ? 2026-01-31

### Features
- Add OpenRouter service support
- Add xAI service support

### Fixes
- Fix race condition
- Fix test

### Improvements
- Simplify Groq authentication
- Update Android JVM target to 21
- Upgrade SDKs

## v1.5.1 ? 2026-01-28

### Improvements
- Improve Enter+Shift key handling for all platforms
- Improve dropdown click handling
- Upgrade SDKs

## v1.5.0 ? 2026-01-27

### Features
- Add Ollama / OpenAI-compatible service support with authentication

### Improvements
- Keep focus on input after sending chat
- Add chat and settings ViewModel tests
- Clean up service separation
- Upgrade SDKs

## v1.4.1 ? 2026-01-24

### Features
- Add iOS text-to-speech support
- Add iOS launch screen

### Fixes
- Fix cursor behavior
- Fix proper edge-to-edge display on iOS

## v1.4.0 ? 2026-01-23

### Features
- Add free tier and iOS release

## v1.3.5 ? 2026-01-18

### Features
- Add in-app review SDK
- Add localizations

## v1.3.4 ? 2026-01-18

### Features
- Add AppImage and RPM build targets

## v1.3.3 ? 2026-01-17

### Features
- Add Windows and Linux desktop builds

## v1.3.2 ? 2026-01-17

### Improvements
- Harden code
- Add version codes to release files
- Upgrade SDKs

## v1.3.1 ? 2025-12-29

### Features
- Add macOS DMG build
- Add code syntax highlighting

### Improvements
- Improve ChatScreen performance by stabilizing lambdas
- Separate state and actions for better Compose performance
- Hoist LazyListState for improved scroll performance

## v1.3.0 ? 2025-12-21

### Features
- Add dynamic color scheme support
- Add localizations
- Move all user-facing strings to strings.xml

## v1.2.14 ? 2025-12-20

### Improvements
- Upgrade SDKs

## v1.2.13 ? 2025-12-13

### Improvements
- Add and sort Groq models by date
- Upgrade SDKs

## v1.2.12 ? 2025-11-30

### Fixes
- Fix error mapping and UI styling

### Improvements
- Improve empty API key fallback behavior

## v1.2.11 ? 2025-11-29

### Improvements
- Update SDKs and models

## v1.2.10 ? 2025-11-15

### Improvements
- Remove markdown formatting
- Upgrade SDKs

## v1.2.9 ? 2025-10-26

### Improvements
- Upgrade SDKs

## v1.2.8 ? 2025-10-05

### Improvements
- Upgrade SDKs

## v1.2.7 ? 2025-09-27

### Improvements
- Upgrade SDKs

## v1.2.5 ? 2025-09-13

### Improvements
- Downgrade AGP for F-Droid compatibility

## v1.2.4 ? 2025-09-06

### Improvements
- Upgrade SDKs

## v1.2.2 ? 2025-08-03

### Improvements
- Migrate to new FileKit library
- Upgrade SDKs

## v1.2.1 ? 2025-07-05

### Improvements
- Add new Gemini models
- Refactor ChatScreen and apply Compose best practices
- Upgrade SDKs

## v1.1.4 ? 2025-05-29

### Features
- Add Gemini 2.5 Flash and Pro preview models
- Implement specific error handling for Gemini API

### Fixes
- Fix error handling catch and UI

### Improvements
- Improve code quality and apply best practices
- Upgrade SDKs

## v1.1.3 ? 2025-05-11

### Improvements
- Add web navigation binding
- Upgrade SDKs

## v1.1.2 ? 2025-05-04

### Improvements
- Upgrade SDKs

## v1.1.1 ? 2025-04-18

### Features
- Add drag-and-drop support for attachments
- Add Gemini file upload support

### Fixes
- Fix TextToSpeechSynthesisInterruptedError crash on Android
- Fix dark/light mode status bar colors

### Improvements
- Add default Groq model
- Move appVersion to version catalog
- Upgrade SDKs

## v1.0.2 ? 2025-01-03

### Features
- Add copy-to-clipboard, speak, and flag actions

### Improvements
- Remove hard-coded API key and add proxy for Groq endpoint

## v1.0.1 ? 2024-12-30

### Features
- Add dark mode support

## v1.0.0 ? 2024-12-30

### Features
- Initial release
- Chat interface with AI services
- Text-to-speech support
- F-Droid packaging
