# GitHub Integration

**Last verified:** 2026-11-10

DORAI AI can connect to GitHub through a GitHub App using the **Device Flow** and browse the repositories an installation can see — read-only. The integration is local-first: authentication, token custody, repository browsing, cloning and git operations all happen on the device.

> **Status: read-only P0 integration.** Clone/sync/status are functional on Android only (guest/proot git). Write operations (`git add` / `commit` / `push`, pull-request creation) are implemented behind a default-off maintainer opt-in (`writeOperationsEnabled`), are not reachable from any UI yet, and always require explicit user approval when they become reachable.

## GitHub App pre-flight (required, manual)

No client secret exists for this integration — the GitHub App is registered with **Device Flow only**:

- App name: `DORAI AI GitHub`, owner `habibbrox2`, no webhook, no callback URL, webhooks disabled.
- Installation scope: any GitHub account or organization; repository permissions **`Contents: Read-only`** (plus `Pull requests: Read-only`), `Metadata` implicit. The owner can restrict access to selected repositories by installing with "Only select repositories" for least privilege.
- `client_id` is read at build time from a local `gradle.properties` property such as `dorai.githubAppClientId` (or a build-time constant supplied by the maintainer). It is never committed.

Phase 0 registration is a manual owner step and is **not** part of the codebase. Phase 4 (write operations) additionally requires `Pull requests: Write` on the App registration.

## Settings

**Settings → GitHub** (dedicated category, icon: code) — visible on all platforms, but the integration itself is supported on **Android and desktop only**. iOS and Web render a "GitHub integration is available on Android and desktop only" notice.

The screen has two states:

### Not connected

- "Connect to GitHub" button starts the Device Flow: `POST https://github.com/login/device/code` → the screen shows the user code and verification URL; the user enters the code at https://github.com/login/device and authorizes.
- The app polls `POST /login/oauth/access_token` with `grant_type=device_code` (RFC 8628), honoring `authorization_pending` (keep polling), `slow_down` (interval + 5s), `expired_token` (stop and surface error), `access_denied`, `device_flow_disabled`, and `bad_refresh_token`.
- On success the token is stored in the platform vault; the screen shows the connected state.

### Connected

- Shows the authenticated user (`GET /user`), the account's installations (`GET /user/installations`), and their repositories (`GET /user/installations/{installation_id}/repositories`) — read-only list from the **first installation** in v1. Repository names, full names and owner types are displayed; a repository can be selected for cloning.
- **Clone** is visible only after a repository is selected. Confirming the dialog clones into `projects/<projectName>` with `--depth=1` and stamps `lastSync`; `projectName` is validated by `GitUrlValidator.isValidProjectName` (no traversal, no slashes, no leading dot) before any guest call. On failure `cloneState` becomes `Failure` and the repository selection is rolled back.
- **Refresh status** and **Sync** buttons run `git status` and `git fetch --prune` + `git pull --ff-only --quiet` for the selected project; failures are recorded as a human-readable `lastError`.
- **Disconnect** clears the vault and resets all UI state.

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
- **Git credential broker**: network git operations use an ephemeral helper (owner-only temp dir, deleted after each command) which answers the username/password challenge from a sidecar token file — the token never appears in argv, env or shell echo. Desktop git runs through `GitCommandRunner`/`GitCredentialBroker` for clone/fetch/pull/push. The Android guest has no guest-visible helper today (host `cacheDir` is not mounted into proot), so anonymous reads work for public repositories but **authenticated git operations on Android are not wired** (e.g. push, private-repo clone) — Phase 0 + guest credential plumbing must land first.
- **Fail-closed policy**: `GitCommandPolicy` allows only read-only subcommands and rejects dangerous flags (`--force`, `--hard`, `--delete`, `--prune`, `--mirror`, `--no-ff`, plus destructive/linkable aliases) everywhere, with exact `-C`-prefix handling and abbreviated-long-option rejection. `WorkspaceEnvironmentManager` additionally validates project names and relative paths on every git call.
- **Write operations are gated twice**: `GitHubIntegrationState.writeOperationsEnabled` defaults to `false` and is flipped only by an explicit maintainer opt-in, and every write invocation additionally requires an explicit user confirmation through the existing `confirmToolExecution` approval flow (classification `OTHER`/ASK-ALWAYS, scoped to the repository fullName). Even then, `git push` accepts only `git push origin HEAD` with no force/mirror/set-upstream, `git commit` requires exactly `-m <msg>` with a 1..200-char single-line message, and `git add` requires explicit relative paths (never `.` or `:/`). No UI exposes the write actions yet.
- `HttpLogSanitizer` does not change behavior for non-GitHub traffic.

## Known limitations

- Desktop clone/sync is not functional (no-op desktop `WorkspaceController`; the desktop CLI layer exists but is unwired).
- Android guest git operations authenticate anonymously only; private repositories and pushes need guest-visible credential helper support + GitHub App registration with write permissions.
- `Pull requests: Read-only` keeps PR creation disabled until `writeOperationsEnabled` and the App is re-registered with `Pull requests: Write`.
- The first installation is used for repository listing in v1.
- Foreground refresh only; no background polling in v1.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubDeviceFlow.kt` | RFC 8628 Device Flow state machine (request code, poll, refresh) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiClient.kt` | REST wrapper: installations, repositories, user (read-only) + `createPullRequest` (write-gated) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiResult.kt` | Sealed outcome types (Success / Unauthorized / NotFound / Forbidden / RateLimited / Failed) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubTokenVault.kt` | `expect` vault interface + `createGitHubTokenVault()` |
| `composeApp/src/androidMain/kotlin/com/dorai/ai/github/GitHubTokenVault.android.kt` | EncryptedSharedPreferences vault (`dorai_github_vault_v1`) |
| `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitHubTokenVault.desktop.kt` | AES-256-GCM file vault (`~/.dorai/github-vault.aes`) |
| `composeApp/src/iosMain/kotlin/com/dorai/ai/github/GitHubTokenVault.ios.kt` | Keychain vault |
| `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/github/GitHubTokenVault.wasmJs.kt` | No-op vault |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationViewModel.kt` | Connect/Device Flow, repository browsing, select-and-clone, sync, write-gated commit/push/PR |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationState.kt` | UI/domain state incl. `CloneState` and `writeOperationsEnabled` |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/GitHubSection.kt` | Settings → GitHub screen (Android/desktop gate, clone/sync UI, confirmation dialog) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCommandPolicy.kt` | Read-only allowlist + flag deny-list + exact write shapes (gated by `writeEnabled`) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitUrlValidator.kt` | `github.com` URL validation, project-name rules, safe relative paths |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCredentialBroker.kt` | Ephemeral owner-only credential helper; token never in argv/logs |
| `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitCommandRunner.desktop.kt` | Desktop git runner (ProcessBuilder, policy-checked, broker for network commands) |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/workspace/WorkspaceController.kt` | Interface: clone/status/log/fetch/pull-ff-only + write methods; `NoOpWorkspaceController` default |
| `composeApp/src/androidMain/kotlin/com/dorai/ai/workspace/runtime/WorkspaceEnvironmentManager.kt` | Guest one-shot proot git executors, path validation |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubIntegrationViewModelTest.kt` | Connect/clone/sync/write-gate tests with fakes |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitCommandPolicyTest.kt` | Read-only + gated write policy pins |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/tools/GitHubToolPermissionsTest.kt` | Tool classification / scope / approval-flow tests |

## Tests

Run: `./gradlew :composeApp:desktopTest` (all `com.dorai.ai.github.*` suites). Screenshot goldens cover the settings shells; see `:screenshotTests:verifyPaparazziDebug`. Android guest git paths are exercised through the `WorkspaceController` interface with fakes — no SDK-bound integration test runs them against a live guest in CI yet.
