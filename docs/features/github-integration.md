# GitHub Integration

**Last verified:** 2026-11-14

DORAI AI can connect to GitHub through a GitHub App using the **Device Flow** and browse the repositories an installation can see — read-only. The integration is local-first: authentication, token custody, repository browsing, cloning and git operations all happen on the device.

> **Status: read-only P0 integration.** Clone/sync/status are functional on Android only (guest/proot git). Write operations (`git add` / `commit` / `push`, pull-request creation) are implemented behind a default-off maintainer opt-in (`writeOperationsEnabled`), are not reachable from any UI yet. Note: the original "every write requires explicit user approval" design was retired 2026-11-13 — approval is now MCP-only, so the gate is the maintainer opt-in plus the hard tool constraints; the `confirmToolExecution` flow no longer fires for these tools.

## GitHub App pre-flight (required, manual)

No client secret exists for this integration — the GitHub App is registered with **Device Flow only**:

- App name: `DORAI AI GitHub`, owner `habibbrox2`, no webhook, no callback URL, webhooks disabled.
- Installation scope: any GitHub account or organization; repository permissions **`Contents: Read-only`** (plus `Pull requests: Read-only`), `Metadata` implicit. The owner can restrict access to selected repositories by installing with "Only select repositories" for least privilege.
- `client_id` is supplied at build time and is never committed. The Gradle build resolves it from `-Pdorai.github.clientId=...` or the `DORAI_GITHUB_CLIENT_ID` environment variable (CI fills the variable from the `DORAI_GITHUB_CLIENT_ID` repository secret) and generates the commonMain constant `GitHubBuildConfig.clientId`; `GitHubAppConfig.clientId` reads that constant. **When the build supplies no value the generated constant is `null`**, so the source tree carries no client ID and the integration fails closed (see "Fail-closed guarantees" below). Maintainers should supply the value through the build (a local `~/.gradle/gradle.properties` entry or the CI secret), never by editing `GitHubAppConfig.kt` or the generated file.

Phase 0 registration is a manual owner step and is **not** part of the codebase. Phase 4 (write operations) additionally requires `Pull requests: Write` on the App registration.

## Settings

**Settings → GitHub** (dedicated category, icon: code) — visible on all platforms, but the integration itself is supported on **Android and desktop only**. iOS and Web render a "GitHub integration is available on Android and desktop only" notice.

The screen opens with a **hero card**: the GitHub mark, a headline that states the current state
(not a second "GitHub" title — the category chip already names it), a status badge
(Not connected / Waiting for authorization / Connected / Connection failed), the description, and the
one action that changes the state (**Connect to GitHub** or **Disconnect**). Below it sit the error
card, the waiting-for-authorization card, the installations and the repository list.

The screen has two states:

### Not connected

- "Connect to GitHub" button starts the Device Flow: `POST https://github.com/login/device/code` → while waiting, the screen shows the user code with a copy button, the verification URI with an "Open GitHub" button that launches the browser, and a Cancel button (`cancelDeviceFlow()`). Errors render as a localized card with a Retry button (`retry()`).
- The app polls `POST /login/oauth/access_token` with `grant_type=device_code` and the OAuth `device_code` (never the user-facing `user_code`), honoring `authorization_pending` (keep polling), `slow_down` (interval + 5s), `expired_token` (stop and surface error), `access_denied`, `device_flow_disabled`, and `bad_refresh_token`.
- On success the tokens are stored in the platform vault and `vault.isConnected()` is verified; a persist mismatch surfaces `Error("token could not be saved")`, never a fake Connected state.
- The consent happens in a browser, so the app is in the background when it completes: a notification ("GitHub connected — return to DORAI AI to browse your repositories") tells the user it worked. Tapping it opens the app on the GitHub settings through the `dorai://github/done` deep link (`DeepLinks.GITHUB_RETURN_URI`).

### Connected

- Shows the connection status; the connect button is replaced by a **Disconnect** button that clears the vault after a confirmation dialog.
- The account's installations (`GET /user/installations`) are listed and rendered (account name + type), followed by their repositories (`GET /user/installations/{installation_id}/repositories`) — read-only list from the **first installation** in v1; a Refresh button re-fetches (falls back to installations when none is loaded).
- **Refresh failures never destroy a valid session.** A missing/expired token (401) does end it; rate limits and transient network failures keep `Connected` and surface a dismissible banner instead of flipping to `Error`.
- The authenticated user's `login` (`GET /user`) is fetched best-effort on connect and shown as "Signed in as …".
- **Disconnect** clears the vault after a confirmation dialog.
- Clone/sync failures render localized messages (git enum names → localized strings), never raw `error.toString()` leakage.
- **Clone** is visible only after a repository is selected. Confirming the dialog clones into `projects/<projectName>` with `--depth=1` and stamps `lastSync` (local date-time formatting); `projectName` is validated by `GitUrlValidator.isValidProjectName` (no traversal, no slashes, no leading dot) before any guest call. On failure `cloneState` becomes `Failure` and the repository selection is rolled back.
- **Refresh status** and **Sync** buttons run `git status` and `git fetch --prune` + `git pull --ff-only --quiet` for the selected project; failures are recorded as a human-readable `lastError`.

## Fail-closed guarantees (2026-11-14)

- No client ID is checked in (`GitHubAppConfig.clientId` reads the generated `GitHubBuildConfig.clientId`, which is `null` unless the build supplies one). Until a build-time client ID is supplied, `connect()` returns `MissingClientId` before any network I/O and a `Connected` state cannot be entered.
- Device-token polling submits the OAuth `device_code` (never the user-facing `user_code`), so GitHub cannot answer `incorrect_device_code` forever; the `user_code` is display-only.
- Vault writes are verified after `store()` — a throwing or silently no-op vault surfaces `Error("token could not be saved")`, never a fake `Connected`.
- `GitUrlValidator.isValidGitHubUrl` is https-only with `[A-Za-z0-9._-]` path segments (this is what keeps shell metacharacters off the Android `/bin/sh` clone path, which also quotes the URL).
- The desktop credential broker emits credentials only when git requests host `github.com`; any other host (repo `insteadOf` redirects, proxies) gets silence.
- A token that exists in the vault is the source of truth for "refresh can run" — recoverable refresh failures (rate limit, transient network) keep `Connected` with a dismissible banner; only 401 ends the session.

## Platform support

| Platform | Token vault | Git CLI | Clone/sync | Notes |
|---|---|---|---|---|
| Android | EncryptedSharedPreferences (`dorai_github_vault_v1`, dedicated file) | Guest/proot Debian one-shot executors via `WorkspaceEnvironmentManager` | **Working** | Clone/status/log/fetch/pull executed inside the guest for `/root/projects/<project>` |
| Desktop (Windows/macOS/Linux) | Dedicated AES-256-GCM file under `~/.dorai` (`github-vault.aes` + owner-only `github-vault.key`) | `DesktopGitCommandRunner` + `GitExecutableLocator` + credential broker | **Not wired yet** | The GitHub section uses `WorkspaceController`, which is a no-op on desktop, so clone/sync fail on desktop today. Wiring the desktop runner into the section is pending. |
| iOS | Keychain (`com.dorai.ai.github.vault`, `KeychainSettings`) | none | Not supported | Section shows the "not supported" notice |
| Web (WasmJs) | no-op vault | none | Not supported | Section shows the "not supported" notice |

## Security model

- **Tokens never leave the device and never reach tools, logs, crash reports, tool arguments or the agent timeline.** All GitHub API calls send `Authorization: Bearer <token>` only; the global Ktor request logger (`HttpLogSanitizer`) redacts the `Authorization` header before any log output, and the redaction tests pin GitHub token literals (`ghp_`, `ghu_`, `ghs_`, `github_pat_`).
- **Vaults are dedicated files**: Android `dorai_github_vault_v1` (never shares keys with provider prefs), desktop `github-vault.aes`/`github-vault.key` with owner-only permissions.
- **Git credential broker**: network git operations use an ephemeral helper (owner-only temp dir, deleted after each command) which answers the username/password challenge from a sidecar token file — the token never appears in argv, env or shell echo. The helper parses git's stdin and emits credentials only when the requested host is exactly `github.com`; for any other host it exits silently, so repo `insteadOf`/proxy redirects cannot siphon the token. Desktop git runs through `GitCommandRunner`/`GitCredentialBroker` for clone/fetch/pull/push. The Android guest has no guest-visible helper today (host `cacheDir` is not mounted into proot), so anonymous reads work for public repositories but **authenticated git operations on Android are not wired** (e.g. push, private-repo clone) — Phase 0 + guest credential plumbing must land first.
- **Fail-closed policy**: `GitCommandPolicy` allows only read-only subcommands and rejects dangerous flags (`--force`, `--hard`, `--delete`, `--prune`, `--mirror`, `--no-ff`, plus destructive/linkable aliases) everywhere, with exact `-C`-prefix handling and abbreviated-long-option rejection. `WorkspaceEnvironmentManager` additionally validates project names and relative paths on every git call.
- **Write operations are gated twice**: `GitHubIntegrationState.writeOperationsEnabled` defaults to `false` and is flipped only by an explicit maintainer opt-in; the tool constraints remain (`git push` accepts only `git push origin HEAD` with no force/mirror/set-upstream, `git commit` requires exactly `-m <msg>` with a 1..200-char single-line message, and `git add` requires explicit relative paths (never `.` or `:/`)). The historical "OTHER/ASK-ALWAYS, scoped to the repository fullName" approval classification was retired 2026-11-13 when approvals became MCP-only — if that changes again, expect GitHub write tools to prompt like any MCP tool. No UI exposes the write actions yet.
- `HttpLogSanitizer` does not change behavior for non-GitHub traffic.

## Known limitations

- The device-flow screen waits indefinitely while the token is pending — there is no countdown/UX for expiry beyond the terminal `expired_token` error state.
- Desktop clone/sync is not functional (no-op desktop `WorkspaceController`; the desktop CLI layer exists but is unwired).
- iOS has no `GitCommandRunner`/`GitCredentialBroker` actuals yet (latent compile break for iOS; iOS not in CI).
- `Pull requests: Read-only` keeps PR creation disabled until `writeOperationsEnabled` and the App is re-registered with `Pull requests: Write`.
- The first installation is used for repository listing in v1; there is no installation selection UI yet (multi-installation accounts see only the first).
- Foreground refresh only; no background polling in v1 — the device-flow wait is cancelled when the tab leaves composition.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/build.gradle.kts` | Generates `GitHubBuildConfig.kt` with the build-time client id (Gradle property `dorai.github.clientId` or `DORAI_GITHUB_CLIENT_ID` env; `null` when absent) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/DeepLinks.kt` | The `dorai://` scheme constants (OAuth and GitHub hand-off links) — the source of truth for the manifest filters |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubAppConfig.kt` | Client id (from the generated constant), API version, app-name constants; no secrets |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubDeviceFlow.kt` | RFC 8628 Device Flow state machine (request code, poll, refresh) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiClient.kt` | REST wrapper: installations, repositories, user (read-only) + `createPullRequest` (write-gated) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiResult.kt` | Sealed outcome types (Success / Unauthorized / NotFound / Forbidden / RateLimited / Failed) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubTokenVault.kt` | `expect` vault interface + `createGitHubTokenVault()` |
| `composeApp/src/androidMain/kotlin/com/dorai/ai/github/GitHubTokenVault.android.kt` | EncryptedSharedPreferences vault (`dorai_github_vault_v1`) |
| `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitHubTokenVault.desktop.kt` | AES-256-GCM file vault (`~/.dorai/github-vault.aes`) |
| `composeApp/src/iosMain/kotlin/com/dorai/ai/github/GitHubTokenVault.ios.kt` | Keychain vault |
| `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/github/GitHubTokenVault.wasmJs.kt` | No-op vault |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationViewModel.kt` | Connect/cancel/disconnect Device Flow (userCode + verificationUri in VM state), installation + automatic repository loading, select-and-clone, sync, write-gated commit/push/PR |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationState.kt` | UI/domain state incl. `CloneState`, `writeOperationsEnabled`, `userCode`/`verificationUri` (waiting-state display only) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/GitHubSection.kt` | Settings → GitHub screen (Android/desktop gate, clone/sync UI, confirmation dialog) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCommandPolicy.kt` | Read-only allowlist + flag deny-list + exact write shapes (gated by `writeEnabled`) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitUrlValidator.kt` | `github.com` URL validation (https-only, shell-safe segment charset), project-name rules, safe relative paths |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCredentialBroker.kt` | Ephemeral owner-only credential helper; token never in argv/logs |
| `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitCommandRunner.desktop.kt` | Desktop git runner (ProcessBuilder, policy-checked, broker for network commands) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/workspace/WorkspaceController.kt` | Interface: clone/status/log/fetch/pull-ff-only + write methods; `NoOpWorkspaceController` default |
| `composeApp/src/androidMain/kotlin/com/dorai/ai/workspace/runtime/WorkspaceEnvironmentManager.kt` | Guest one-shot proot git executors, path validation |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubIntegrationViewModelTest.kt` | Connect/clone/sync/write-gate tests with fakes |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitCommandPolicyTest.kt` | Read-only + gated write policy pins |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/tools/GitHubToolPermissionsTest.kt` | Tool classification / scope / approval-flow tests |

## Tests

Run: `./gradlew :composeApp:desktopTest` (all `com.dorai.ai.github.*` suites). Screenshot goldens cover the settings shells; see `:screenshotTests:verifyPaparazziDebug`. Android guest git paths are exercised through the `WorkspaceController` interface with fakes — no SDK-bound integration test runs them against a live guest in CI yet.
