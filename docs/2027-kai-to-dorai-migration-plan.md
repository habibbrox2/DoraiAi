# kai → dorai Data-Migration Plan

**Status:** In progress (code-side backward-compat shipped; external contracts retained for migration).
**Scope:** Remove the remaining `kai` words from persisted data formats, external contracts and runtime artefacts without losing existing user data.

## 1. Principle

- **Read both, write the new name.** Every consumer first tries the `dorai*` form; if absent it falls back to the legacy `kai*` form. New writes always use `dorai*`.
- **Migrate once, lazily.** On first run after upgrade, copy/normalise legacy data into the new location/key and keep the legacy copy until the next major release.
- **Never delete legacy data silently.** Old keys/files stay readable for one full release cycle, then are removed in a follow-up cleanup.

## 2. Contracts and migration status

| # | Legacy contract | Location | Migration strategy | Status |
|---|---|---|---|---|
| 1 | `~/.kai` data directory | `Platform.jvm.kt` `getAppFilesDirectory()` | Copy `~/.kai` → `~/.dorai` on first run if `~/.dorai` absent; use `~/.dorai` thereafter. | ✅ Shipped |
| 2 | SSH config markers `# kai:<marker>:start/end` | `SshConfigManager.upsertBlock` | Regex now matches both `kai:` and `dorai:` blocks and rewrites them as `dorai:`. | ✅ Shipped |
| 3 | `kai-ui` markdown fence | `BlockScanner` + `ChatSystemPromptBuilder` | Parser accepts both `kai-ui` and `dorai-ui`; new LLM output uses `dorai-ui`. | ✅ Shipped |
| 4 | `kai-browser` rootfs binary + `KAI_OPEN_URL_FILE` env + `/tmp/kai-open-url` | `WorkspaceEnvironmentManager` | Install `dorai-browser` + keep `kai-browser` alias; set both env vars; fallback path tries `dorai-open-url` then `kai-open-url`. | ✅ Shipped |
| 5 | `kai-winsize`, `kai-pid`, `kai-open-url` temp files | `WorkspaceProotExecutor`, `WorkspaceEnvironmentManager` | Renamed to `dorai-*` (runtime-only, recreated each session). | ✅ Shipped |
| 6 | `kai.debug` system property | `BuildKonfig.kt` | Read `dorai.debug`, fall back to `kai.debug`. | ✅ Shipped |
| 7 | `kai-settings` export filename | `ExportImportSection.kt` | Renamed to `dorai-settings`. | ✅ Shipped |
| 8 | `brox-kai-owner` sponsor id | `ServicesSettings.kt` | Renamed to `brox-dorai-owner` (in-memory only, no persistence). | ✅ Shipped |
| 9 | `.kai-tmp` temp file suffix | `FileReviewEngine.kt` | Renamed to `.dorai-tmp`. | ✅ Shipped |
| 10 | `kai-shell-reader` thread name | `ShellCommandTool.kt` | Renamed to `dorai-shell-reader`. | ✅ Shipped |
| 11 | `kai-test-*` / `kai-symlink-probe` / `kai-posix-probe` temp prefixes | tests | Renamed to `dorai-*`. | ✅ Shipped |
| 12 | `KAI_INTEGRATION` / `KAI_REPORT_DIR` env vars | `DoraiUiValidationTest` | Read `DORAI_*` first, fall back to `KAI_*`. | ✅ Shipped |
| 13 | `kaimutableConversations` legacy storage key (wasmJs) | `ConversationStorage.wasmJs.kt` | **Kept.** Legacy key only read for one-time migration; renaming breaks migration of old data. Remove in next major release. | ⏸ Deferred |
| 14 | `kai9000` model name | provider model id | **Kept.** External model identifier — cannot rename. | ⏸ Permanent |
| 15 | `~/.kai/projects` doc reference | `DesktopProjectFileReviewFileSystem.kt` (comment) | Update comment only. | ✅ Shipped (comment) |

## 3. Remaining work

- Remove legacy `kai:` SSH block recognition after one release cycle.
- Remove `kaimutableConversations` legacy-key read after one release cycle.
- Drop the `kai-browser` alias and `KAI_OPEN_URL_FILE` fallback after one release cycle.
- Drop `kai.debug` fallback after one release cycle.

## 4. Verification

- `:composeApp:compileAndroidMain`, `:composeApp:compileKotlinWasmJs`, `:composeApp:compileKotlinIosSimulatorArm64`, `:composeApp:desktopTest` — all BUILD SUCCESSFUL after these changes.
- Desktop SSH config tests (`SshConfigManagerTest`) exercise the upsert path and pass.
