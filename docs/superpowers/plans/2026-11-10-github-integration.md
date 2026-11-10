# GitHub Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`. Execute one task at a time, keep the checkbox list in this file updated, and do not start Phase 4 (write operations) until Phases 1–3 are merged and verified.

**Goal:** DORAI AI-কে `habibbrox2/DORAI AI GitHub` GitHub App দিয়ে GitHub-এর সাথে local-first integration দেওয়া যাবে — Windows + Android-এ login, selected-repository read-only browsing, native Git CLI দিয়ে clone/status/log/fetch, এবং ভবিষ্যতে explicit approval-gated write operations।

**Architecture:** বিদ্যমান KMP architecture (`commonMain` + platform source sets) এর ওপর `com.dorai.ai.github` package বসানো হবে। OpenAI-compatible `Requests`-এর মতো একটি generic Ktor client-এর ওপর GitHub-specific DTOs, pagination, rate-limit, ETag এবং Device Flow state machine থাকবে। Tokens `ToolExecutor`/agent tools-এ কখনোই যায় না; platform vault (Android `EncryptedSharedPreferences`, desktop hardened encrypted file/DPAPI-compatible boundary) token রাখে। Git operation যা হয় native `git` দিয়ে — Android-এ proot Debian guest-এর existing Workspace runtime, Windows/desktop-এ `ProcessBuilder` + strict `git` subcommand allowlist।

**Tech Stack:** Kotlin Multiplatform 2.4.21, Compose Multiplatform 1.12.2, Ktor 3.6.0, kotlinx-serialization 1.11.0, Koin 4.2.2, SQLDelight 2.4.1 (v1-এ কোনো new table দরকার নেই), existing `EncryptedSharedPreferences` (Android), existing `EncryptedFileSettings` (desktop), existing proot Workspace (`WorkspaceEnvironmentManager.kt`), existing Compose Settings UI.

**Spec:** N/A — this plan is derived from the 2026-11-10 read-only audit delivered in-session. No committed GitHub feature spec exists yet. Treat the confirmed product decisions in "Global Constraints" as binding.

## Global Constraints

- Product: DORAI AI
- GitHub App name: `DORAI AI GitHub`
- GitHub App owner: `habibbrox2`
- Installation scope: any GitHub account or organization
- Authentication: GitHub App **Device Flow**, GitHub App **user authorization**
- Deployment: local-first
- Sandbox hosts: **Windows + Android**
- Git operations: **native Git CLI** where supported
- GitHub API: GitHub App user authorization
- Custom domain: not available and must not be required
- Webhooks: disabled for initial version
- Cloud backend: not required
- Cloud source-code execution: not permitted
- Default repository permission: **read-only**
- Write operations: explicit approval required in a future implementation phase
- No new dependencies without explicit maintainer approval; prefer existing Ktor/serialization/Koin/Settings APIs.
- Never place a GitHub token in tool arguments, tool results, logs, crash reports, `settings.aes`, guest-visible `/root/projects`, or the agent's PTY transcript.
- Keep unrelated working-tree changes untouched. Commits are phase-sized.
- No GitHub App registration is part of implementation; the owner must complete the pre-flight checklist first.

## Review Focus

- Token appears in `git` argv, shell echo, process env dump, `ProcessManager.log`, `ShellCommandTool` output, or the agent timeline → pin with `GitCredentialBrokerTest`, `ProcessManagerAllowlistTest`, `HttpLogSanitizerGithubTest`.
- `ProcessManager.startBackground("cmd /c ...")` bypasses the GitHub/Git allowlist → pin with `ProcessManagerAllowlistTest.backgroundCommandGoesThroughGitAllowlist`.
- User/device flow polling ignores `slow_down` and `expired_token`, creating an infinite authorization loop → pin with `GitHubDeviceFlowTest.slowDownAddsFiveSecondsAndExpiryStopsPolling`.
- Repository clone is attempted for arbitrary hosts or paths (`https://evil.com/...`, symlink escape from project root) → pin with `GitUrlValidatorTest`, `GitProjectPathValidatorTest`.
- Destructive Git flags (`reset --hard`, `clean -fdx`, `push --force`) become reachable through normal tools → pin with `GitCommandPolicyTest.rejectsDangerousSubcommandsAndFlags`.

---

## File Structure

### New commonMain files

- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubModels.kt` — DTOs for User, Installation, Repository, DeviceCode, TokenResponse, PullRequest.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubAppConfig.kt` — `clientId`, API version, app-name constants; no secrets.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubDeviceFlow.kt` — RFC 8628 Device Flow state machine.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubTokenVault.kt` — expect interface: `readAccessToken()`, `readRefreshToken()`, `store(accessToken, refreshToken, expiresAt, login, installationIds)`, `clear()`, `isConnected()`.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiClient.kt` — GitHub REST wrapper: `getAuthenticatedUser`, `listInstallations`, `listInstallationRepositories`, `getRepository`, optional `createPullRequest` behind write flag; ETag caching, Link-header pagination, rate-limit backoff.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiResult.kt` — sealed result types: `Success`, `Unauthorized`, `Forbidden`, `RateLimited`, `NotFound`, `Failure`.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCredentialBroker.kt` — creates the host/guest credential material and returns only opaque broker state; never prints or returns the token to callers.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitUrlValidator.kt` — validates `https://github.com/{owner}/{repo}(.git)?`, project name rules, path containment.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCommandPolicy.kt` — allowlist of git subcommands and flag rejection.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationState.kt` — immutable UI/domain state: connection status, selected installation, selected repo, last sync.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationViewModel.kt` — Koin ViewModel for Settings section.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/GitHubSection.kt` — Compose settings section.

### New platform files

- `composeApp/src/androidMain/kotlin/com/dorai/ai/github/GitHubTokenVault.android.kt` — `EncryptedSharedPreferences` file `dorai_github_vault_v1`, key alias separated from provider API-key prefs.
- `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitHubTokenVault.desktop.kt` — initially `EncryptedFileSettings` variant hardened for this secret (`Cipher AES/GCM/NoPadding`, `restrictToOwner`, atomic replace, backup on decrypt failure), marked as the DPAPI boundary to upgrade before release.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCommandRunner.kt` — expect interface for running git.
- `composeApp/src/androidMain/kotlin/com/dorai/ai/github/GitCommandRunner.android.kt` — delegates to Workspace proot fresh one-shot executor for `/root/projects/<project>`.
- `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitCommandRunner.desktop.kt` — locates `git` (registry/`Program Files/Git` + PATH fallback), runs in app-private `~/.dorai/projects/<project>`, under `ProcessBuilder` with `GitCommandPolicy`.
- `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/github/GitHubTokenVault.wasmJs.kt` — no-op/offline unsupported actual.
- `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/github/GitCommandRunner.wasmJs.kt` — no-op actual, UI reports unsupported.
- `composeApp/src/iosMain/kotlin/com/dorai/ai/github/GitHubTokenVault.ios.kt` and `GitCommandRunner.ios.kt` — Apple Keychain + no-op/native best-effort; GitHub phase is Windows+Android first.

### Existing files to modify

- `composeApp/src/commonMain/kotlin/com/dorai/ai/AppModule.kt` — register `GitHubTokenVault`, `GitHubApiClient`, `GitHubIntegrationViewModel`, `GitCommandPolicy` binding.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/Platform.kt` — add `expect fun createGitHubTokenVault()` if direct `expect` binding is preferred; otherwise keep inside `appModule` with platform factories.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/network/HttpLogSanitizer.kt` — add GitHub token literals (`ghp_`, `ghu_`, `ghs_`, `github_pat_`) and `Authorization: bearer ghs_` masking.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/ToolPermissionManager.kt` — classify `github_commit`, `github_push`, `github_create_pull_request` as OTHER / ASK_ALWAYS by default.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/RemoteDataRepository.kt` — reuse `toolPermissionManager.shouldAskForPermission(...)` for the three write tools; do not add bypass paths.
- `composeApp/src/androidMain/kotlin/com/dorai/ai/workspace/runtime/WorkspaceEnvironmentManager.kt` — add `gitLog`, `gitClone`, `gitFetch`, `gitPullFfOnly` methods for the guest projects root.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/workspace/WorkspaceController.kt` — extend interface with read-only GitHub-facing methods; `NoOpWorkspaceController` returns unsupported.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt` — add `SettingsCategory.GitHub`.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsNavigation.kt`, `SettingsDetailPane.kt`, `SettingsUiState.kt`, `SettingsScreen.kt` — route the new category.
- `composeApp/src/commonMain/composeResources/values/strings.xml` and `values-bn/strings.xml` — English and Bengali strings for the GitHub section.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/adaptive/DoraiDestination.kt` or existing Settings shell — only if the section cannot fit within Settings. Prefer fitting within Settings.

### Tests

- `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubDeviceFlowTest.kt`
- `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubApiClientTest.kt`
- `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubTokenVaultTest.kt` (with fake in-memory vault)
- `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitCommandPolicyTest.kt`
- `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitUrlValidatorTest.kt`
- `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitCredentialBrokerTest.kt`
- `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubIntegrationViewModelTest.kt`
- `composeApp/src/desktopTest/kotlin/com/dorai/ai/github/ProcessManagerAllowlistTest.kt`
- `composeApp/src/commonTest/kotlin/com/dorai/ai/network/HttpLogSanitizerGithubTest.kt`

---

## Phase 0 — Pre-flight: GitHub App Registration (owner only, not code)

This phase is manual and must happen before any device-flow code can be tested against the real app. Do **not** register it inside this implementation plan; the owner does it at GitHub Settings.

- [ ] **Step 1: Create GitHub App**
  - App name: `DORAI AI GitHub`
  - Owner: `habibbrox2`
  - Homepage URL: a valid public URL for the app or a placeholder docs page; no custom domain is required for Device Flow.
  - Callback/webhook: **none**
  - Webhooks: disabled
  - Device Flow: enabled
  - Installation scope: Any GitHub account or organization
  - Repository permissions: `Contents: Read-only`, `Pull requests: Read-only`, `Metadata: Read-only` (implicit). No write permission in v1.
  - User permissions: none
  - Public installation: enabled if you want users outside your org to install; otherwise restrict to selected organizations.
  - Generate **no client secret** for Device Flow; treat `client_id` as a public identifier.
- [ ] **Step 2: Record `client_id` locally**
  - Do not commit it as a task in this plan. Implementation should read it from a local `gradle.properties` property such as `dorai.githubAppClientId` or a build-time constant supplied by the maintainer. If the repository already has a public updater `client_id`, reuse only if it is the same GitHub App; otherwise keep separate.
- [ ] **Step 3: Verify selected-only installation**
  - In a test org, install the app with "Only select repositories" and confirm the app sees only the chosen repos via `GET /user/installations/{installation_id}/repositories`.
- [ ] **Step 4: Document the registration result**
  - Keep the app permissions and installation mode in the feature doc / release notes, not in source comments.

**Gate for Phase 1:** `client_id` available to the build, read-only permissions confirmed on a private test repository.

---

## Phase 1 — Auth Core: Device Flow + Token Vault + Log Safety

### Task 1: `GitHubModels.kt` DTOs and `GitHubAppConfig.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubAppConfig.kt`
- Test: none (plain DTOs)

**Interfaces:**
- Consumes: existing `Version.appVersion` for User-Agent.
- Produces:
  - `data class DeviceCodeResponse(deviceCode: String, userCode: String, verificationUri: String, expiresIn: Long, interval: Long)`
  - `data class GitHubTokenResponse(accessToken: String, expiresIn: Long?, refreshToken: String?, refreshTokenExpiresIn: Long?, tokenType: String, scope: String?)`
  - `data class GitHubUser(login: String, id: Long, name: String?, avatarUrl: String?)`
  - `data class GitHubInstallation(id: Long, account: GitHubInstallationAccount, repositorySelection: String?, permissions: Map<String, String>?)`
  - `data class GitHubInstallationAccount(login: String, id: Long, type: String)`
  - `data class GitHubRepository(id: Long, name: String, fullName: String, owner: GitHubRepositoryOwner, private: Boolean, defaultBranch: String, htmlUrl: String)`
  - `data class GitHubRepositoryOwner(login: String, id: Long, type: String)`
  - `data class GitHubRepoPermissions(contents: String, pullRequests: String)`

- [ ] **Step 1: Write `GitHubModels.kt`**
  - Use `@Serializable`, `@SerialName("device_code")` etc., matching GitHub REST field names.
  - Keep `id` as `Long` and token strings opaque (`String`, no custom type).
- [ ] **Step 2: Write `GitHubAppConfig.kt`**
  - `object GitHubAppConfig { const val USER_AGENT_PRODUCT = "DORAI-AI"; const val API_VERSION = "2026-03-10"; val clientId: String? = null /* injected at build time */ }`
  - Do not hard-code a real `client_id` in this file.
- [ ] **Step 3: Compile-check only**
  - Run: `./gradlew :composeApp:compileKotlinDesktop`
  - Expected: build succeeds. If this reveals unrelated pre-existing errors, stop and report them; do not fix unrelated code in this task.

### Task 2: `GitHubTokenVault` expect/actual

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubTokenVault.kt`
- Create: `composeApp/src/androidMain/kotlin/com/dorai/ai/github/GitHubTokenVault.android.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitHubTokenVault.desktop.kt`
- Create: `composeApp/src/iosMain/kotlin/com/dorai/ai/github/GitHubTokenVault.ios.kt`
- Create: `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/github/GitHubTokenVault.wasmJs.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubTokenVaultTest.kt`

**Interfaces:**
- Consumes: `android.content.SharedPreferences` (Android), `EncryptedFileSettings` (desktop), Keychain (iOS), no-op (Wasm).
- Produces:
  ```kotlin
  interface GitHubTokenVault {
      fun readAccessToken(): String?
      fun readRefreshToken(): String?
      fun readLogin(): String?
      fun store(
          accessToken: String,
          refreshToken: String?,
          expiresAtEpochMs: Long?,
          refreshExpiresAtEpochMs: Long?,
          login: String,
          installationIds: List<Long>,
      )
      fun clear()
      fun isConnected(): Boolean
  }
  ```
- On Android use `EncryptedSharedPreferences.create(..., "dorai_github_vault_v1")` with the existing `MasterKey.AES256_GCM` pattern from `Platform.android.kt:201-212`.
- On desktop use an `EncryptedFileSettings` subclass/`EncryptedFileSettings` instance with `settings.aes` file name `github-vault.aes` and `github-vault.key`; only this class may call `readAccessToken()`.

- [ ] **Step 1: Write the failing test with an in-memory `FakeGitHubTokenVault`**
  ```kotlin
  @Test fun `stored vault round trips tokens and login`() {
      val vault = InMemoryGitHubTokenVault()
      vault.store("access", "refresh", 1_000L, 2_000L, "octocat", listOf(1L))
      assertEquals("access", vault.readAccessToken())
      assertEquals("refresh", vault.readRefreshToken())
      assertEquals("octocat", vault.readLogin())
      assertTrue(vault.isConnected())
  }

  @Test fun `clear removes all token state`() {
      val vault = InMemoryGitHubTokenVault()
      vault.store("access", "refresh", null, null, "octocat", emptyList())
      vault.clear()
      assertNull(vault.readAccessToken())
      assertNull(vault.readRefreshToken())
      assertFalse(vault.isConnected())
  }
  ```
- [ ] **Step 2: Run test to verify it fails**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubTokenVaultTest"`
  - Expected: FAIL (`GitHubTokenVault` unresolved).
- [ ] **Step 3: Implement `GitHubTokenVault` expect + in-memory test fake**
  - Keep the real platform implementations minimal; `isConnected()` is true only if `accessToken != null && login != null`.
- [ ] **Step 4: Implement Android `GitHubTokenVault.android.kt`**
  - Keys: `access_token`, `refresh_token`, `access_expires_at`, `refresh_expires_at`, `login`, `installation_ids` (JSON array).
  - On decrypt failure, delete only `dorai_github_vault_v1`, not the provider `kai_secure_prefs` file.
- [ ] **Step 5: Implement desktop `GitHubTokenVault.desktop.kt`**
  - File: `~/.dorai/github-vault.aes`; key file `~/.dorai/github-vault.key`.
  - Reuse the `EncryptedFileSettings` atomic write pattern.
  - Add `restrictToOwner()` calls on both files.
  - Mark one comment: `// DPAPI migration required before production release — do not ship without OS-bound key.`
- [ ] **Step 6: Implement iOS and Wasm no-op/Keychain actuals**
  - iOS: `KeychainSettings` is not present in the dependency graph; implement with a minimal Security framework-backed `expect/actual` if approved, otherwise mark `unsupported = true` and return false everywhere. Do not add a new dependency without approval.
  - Wasm: return null/false everywhere; GitHub UI must show "Android and Windows only".
- [ ] **Step 7: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubTokenVaultTest"`
  - Expected: PASS.

### Task 3: `GitHubDeviceFlow.kt` Device Flow state machine

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubDeviceFlow.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubDeviceFlowTest.kt`

**Interfaces:**
- Consumes: `GitHubModels.DeviceCodeResponse`, `GitHubTokenResponse`, Ktor `httpClient`, `GitHubAppConfig.clientId`.
- Produces:
  ```kotlin
  sealed interface GitHubDeviceFlowState {
      data object Idle : GitHubDeviceFlowState
      data class AwaitingUser(val userCode: String, val verificationUri: String, val expiresIn: Long, val interval: Long) : GitHubDeviceFlowState
      data class Polling(val userCode: String, val interval: Long) : GitHubDeviceFlowState
      data class Success(val login: String) : GitHubDeviceFlowState
      data class Error(val error: GitHubDeviceFlowError) : GitHubDeviceFlowState
  }

  sealed interface GitHubDeviceFlowError {
      data object AuthorizationPending : GitHubDeviceFlowError
      data object SlowDown : GitHubDeviceFlowError
      data object ExpiredToken : GitHubDeviceFlowError
      data object AccessDenied : GitHubDeviceFlowError
      data object DeviceFlowDisabled : GitHubDeviceFlowError
      data object BadRefreshToken : GitHubDeviceFlowError
      data class Network(val message: String) : GitHubDeviceFlowError
      data object MissingClientId : GitHubDeviceFlowError
  }

  class GitHubDeviceFlow(
      private val clientId: String?,
      private val httpClient: HttpClient = httpClient(),
  ) {
      suspend fun requestDeviceCode(): Result<GitHubDeviceFlowState.AwaitingUser>
      suspend fun pollForToken(deviceCode: String, intervalSeconds: Long, expiresAtEpochMs: Long): GitHubDeviceFlowState
      suspend fun refreshToken(refreshToken: String): Result<GitHubTokenResponse>
  }
  ```
- For GitHub App Device Flow, `scope` parameter is ignored by GitHub; send an empty body to `POST /login/device/code` and no `client_secret`.
- Poll using `interval` + 5s on `slow_down`; stop and return `ExpiredToken` after `expires_in` seconds.
- On success, call `GET /user` to resolve `login` for the vault.

- [ ] **Step 1: Write failing tests with `MockEngine`**
  ```kotlin
  @Test fun `requestDeviceCode returns awaiting user when device flow succeeds`()
  @Test fun `pollForToken maps authorization_pending to polling without failing`()
  @Test fun `pollForToken increases interval by five seconds on slow_down`()
  @Test fun `pollForToken returns expired token after expires_in elapses`()
  @Test fun `pollForToken returns access denied when user cancels`()
  @Test fun `pollForToken throws device_flow_disabled when app setting missing`()
  @Test fun `refreshToken posts grant_type=refresh_token and returns rotated tokens`()
  @Test fun `refreshToken maps bad_refresh_token to reauth required`()
  ```
- [ ] **Step 2: Run test to verify it fails**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubDeviceFlowTest"`
  - Expected: FAIL (`GitHubDeviceFlow` unresolved).
- [ ] **Step 3: Implement requestDeviceCode**
  - `POST https://github.com/login/device/code`, body `client_id=<id>`, `Accept: application/json`.
  - Reject `clientId == null` with `MissingClientId` before making the request.
- [ ] **Step 4: Implement pollForToken**
  - `POST https://github.com/login/oauth/access_token`, `grant_type=urn:ietf:params:oauth:grant-type:device_code`, `client_id`, `device_code`.
  - Handle `authorization_pending`, `slow_down`, `expired_token`, `access_denied`, `device_flow_disabled`.
- [ ] **Step 5: Implement refreshToken**
  - `POST https://github.com/login/oauth/access_token`, `grant_type=refresh_token`, `client_id`, `refresh_token`.
  - No `client_secret` for Device Flow apps.
- [ ] **Step 6: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubDeviceFlowTest"`
  - Expected: PASS.

### Task 4: `GitHubApiClient.kt` — REST wrapper with pagination and rate limits

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiClient.kt`
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubApiResult.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubApiClientTest.kt`

**Interfaces:**
- Consumes: `GitHubTokenVault`, `GitHubModels`, Ktor `httpClient`.
- Produces:
  ```kotlin
  class GitHubApiClient(private val vault: GitHubTokenVault, private val httpClient: HttpClient = httpClient()) {
      suspend fun getAuthenticatedUser(): GitHubApiResult<GitHubUser>
      suspend fun listInstallations(): GitHubApiResult<List<GitHubInstallation>>
      suspend fun listInstallationRepositories(installationId: Long): GitHubApiResult<List<GitHubRepository>> // uses GET /user/installations/{installation_id}/repositories
      suspend fun getRepository(owner: String, repo: String): GitHubApiResult<GitHubRepository>
  }
  ```
- Every request sends `Authorization: Bearer <accessToken>`, `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2026-03-10`, `User-Agent: DORAI-AI/<version>`.
- Follow `Link` header pagination (`rel="next"`) up to 10 pages.
- On `403` with `X-RateLimit-Remaining: 0`, return `RateLimited(resetEpochMs)`.
- On any `401`, clear the vault and return `Unauthorized`.

- [ ] **Step 1: Write failing tests with `MockEngine`**
  ```kotlin
  @Test fun `listInstallations parses installation accounts and repositories_selection`()
  @Test fun `listInstallationRepositories follows Link next header across two pages`()
  @Test fun `401 response clears token vault and returns Unauthorized`()
  @Test fun `403 with X-RateLimit-Remaining zero returns RateLimited with reset time`()
  @Test fun `404 returns NotFound for missing repository`()
  ```
- [ ] **Step 2: Run test to verify it fails**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubApiClientTest"`
  - Expected: FAIL.
- [ ] **Step 3: Implement result type and base request**
  - Parse GitHub error body `{"message":"...","documentation_url":"..."}` for user-facing errors, but never surface the raw token.
- [ ] **Step 4: Implement pagination helper**
  - Extract `Link: <https://api.github.com/...?page=2>; rel="next"` and follow it.
- [ ] **Step 5: Implement rate-limit and 401 handling**
  - On 401, call `vault.clear()`.
  - On 429 or 403 + `Retry-After`/`X-RateLimit-Reset`, return `RateLimited`.
- [ ] **Step 6: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubApiClientTest"`
  - Expected: PASS.

### Task 5: Redact GitHub tokens in logs

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/HttpLogSanitizer.kt:14-38`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/network/HttpLogSanitizerGithubTest.kt`

**Interfaces:**
- Consumes: existing `HttpLogSanitizer.sanitize`.
- Produces: same API, stronger redaction.

- [ ] **Step 1: Write failing test**
  ```kotlin
  @Test fun `sanitizer masks github installation tokens`() {
      val line = "Authorization: Bearer ghs_abcdefghijklmnop1234567890"
      assertEquals("Authorization: Bearer [REDACTED]", HttpLogSanitizer.sanitize(line))
  }
  @Test fun `sanitizer masks github user-to-server tokens`() {
      val line = "Authorization: Bearer ghu_abcdefghijklmnop1234567890"
      assertEquals("Authorization: Bearer [REDACTED]", HttpLogSanitizer.sanitize(line))
  }
  ```
- [ ] **Step 2: Run test to verify it fails**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.network.HttpLogSanitizerGithubTest"`
  - Expected: FAIL.
- [ ] **Step 3: Extend `HttpLogSanitizer`**
  - Add `ghs_`, `ghu_`, `ghp_`, `github_pat_` to the inline token regex.
- [ ] **Step 4: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.network.HttpLogSanitizerGithubTest"`
  - Expected: PASS.

### Task 6: Harden `ProcessManager` against shell bypass

**Files:**
- Modify: `composeApp/src/desktopMain/kotlin/com/dorai/ai/tools/ProcessManager.kt:36-41`
- Modify: `composeApp/src/jvmShared/kotlin/com/dorai/ai/tools/ProcessManagerTool.kt` (only if the public comment/documentation needs updating)
- Test: `composeApp/src/desktopTest/kotlin/com/dorai/ai/github/ProcessManagerAllowlistTest.kt`

**Interfaces:**
- Consumes: existing `ALLOWED_EXECUTABLES`, `hasDangerousFlags`, `containsShellMetacharacters` from `ShellCommandTool` — these must be made `internal` rather than `private` by this task, or duplicated into a shared `GitCommandPolicy` object (preferred: new `GitCommandPolicy` in Task 7, used by both).
- Produces: `ProcessManager.startBackground` rejects commands not matching the Git/shell allowlist.

- [ ] **Step 1: Write failing test**
  ```kotlin
  @Test fun `background git push is rejected`() {
      val pm = ProcessManager()
      val result = pm.startBackground("git push origin main", 5, null, emptyMap())
      assertEquals(false, result["success"])
      assertTrue(result["error"].toString().contains("not allowed", ignoreCase = true))
  }

  @Test fun `background git status is allowed`() {
      val pm = ProcessManager()
      val result = pm.startBackground("git status --short", 5, null, emptyMap())
      assertEquals(true, result["success"])
  }
  ```
- [ ] **Step 2: Run test to verify it fails**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.ProcessManagerAllowlistTest"`
  - Expected: FAIL (`git push` accepted).
- [ ] **Step 3: Implement allowlist check**
  - In `ProcessManager.startBackground`, split `command` by shell whitespace (no pipe/chain support), reject if `parts[0]` is not in the Git/shell allowlist, reject dangerous flags/metacharacters, and return `mapOf("success" to false, "error" to "...")` before `ProcessBuilder`.
- [ ] **Step 4: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.ProcessManagerAllowlistTest"`
  - Expected: PASS.

---

## Phase 2 — Read-only Repository Discovery + Settings UI

### Task 7: `GitCommandPolicy.kt` — shared allowlist and dangerous-flag rejection

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCommandPolicy.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitCommandPolicyTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object GitCommandPolicy {
      val allowedGitSubcommands: Set<String> = setOf(
          "status", "log", "diff", "branch", "remote", "fetch", "clone", "pull", "rev-parse", "ls-remote",
      )
      // Write subcommands (commit, push, add, restore) are intentionally absent in Phases 1–3.
      val rejectedFlags: Set<String> = setOf("--force", "--hard", "--delete", "--prune", "--mirror", "--no-ff")
      fun isAllowedGitArgs(args: List<String>): Boolean
      fun quoteArg(value: String): String
  }
  ```
- `git pull --ff-only` only; `git pull` with any other flag rejected.
- `git clone` only with `--depth=1` and single URL argument; no `--recursive`, `--bare`, `--mirror`.

- [ ] **Step 1: Write failing test**
  ```kotlin
  @Test fun `allows git status porcelain`() = assertTrue(GitCommandPolicy.isAllowedGitArgs(listOf("status", "--porcelain=v1")))
  @Test fun `rejects git reset hard`() = assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("reset", "--hard")))
  @Test fun `rejects git push`() = assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("push")))
  @Test fun `rejects git clone mirror`() = assertFalse(GitCommandPolicy.isAllowedGitArgs(listOf("clone", "--mirror", "https://github.com/a/b")))
  ```
- [ ] **Step 2: Run test to verify it fails**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitCommandPolicyTest"`
  - Expected: FAIL.
- [ ] **Step 3: Implement policy**
  - Compare basename/subcommand only; treat `git -C <dir> <subcommand>` by finding the subcommand token after `-C <path>`.
- [ ] **Step 4: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitCommandPolicyTest"`
  - Expected: PASS.

### Task 8: `GitUrlValidator.kt` — host/repo/project name safety

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitUrlValidator.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitUrlValidatorTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object GitUrlValidator {
      fun isValidGithubRepoUrl(url: String): Boolean
      fun ownerRepoFromUrl(url: String): Pair<String, String>?
      fun isValidProjectName(name: String): Boolean
  }
  ```
- Allowed hosts: `github.com` only. Owner/repo charset: `[A-Za-z0-9_.-]{1,100}`. Project name: `[A-Za-z0-9][A-Za-z0-9_.-]{0,63}`, no `..`, no `/`, no NUL.
- Reject URLs with userinfo, query, fragment, port, or non-https scheme.

- [ ] **Step 1: Write failing test**
  ```kotlin
  @Test fun `accepts https github com owner repo`() = assertTrue(GitUrlValidator.isValidGithubRepoUrl("https://github.com/dorai-ai/dorai.git"))
  @Test fun `rejects non-github hosts`() = assertFalse(GitUrlValidator.isValidGithubRepoUrl("https://evil.com/dorai-ai/dorai.git"))
  @Test fun `rejects url with userinfo`() = assertFalse(GitUrlValidator.isValidGithubRepoUrl("https://user:pass@github.com/dorai-ai/dorai.git"))
  @Test fun `rejects project name with traversal`() = assertFalse(GitUrlValidator.isValidProjectName("../escape"))
  ```
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement validators**
- [ ] **Step 4: Run test**
  - Expected: PASS.

### Task 9: `GitHubIntegrationState.kt` and `GitHubIntegrationViewModel.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationState.kt`
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/AppModule.kt` (register ViewModel)
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitHubIntegrationViewModelTest.kt`

**Interfaces:**
- `GitHubIntegrationState`:
  ```kotlin
  data class GitHubIntegrationState(
      val connectionStatus: GitHubConnectionStatus = GitHubConnectionStatus.NotConnected,
      val user: GitHubUser? = null,
      val installations: List<GitHubInstallation> = emptyList(),
      val repositories: List<GitHubRepository> = emptyList(),
      val selectedFullName: String? = null,
      val isLoading: Boolean = false,
      val error: GitHubDeviceFlowError? = null,
      val writeOperationsEnabled: Boolean = false,
  )
  enum class GitHubConnectionStatus { Unsupported, NotConnected, WaitingForDevice, Connected, Refreshing, Error }
  ```
- `GitHubIntegrationViewModel` actions: `connect()`, `poll()`, `disconnect()`, `refreshInstallations()`, `selectRepository(fullName)`, `refreshRepositories()`, `retry()`.

- [ ] **Step 1: Write failing test with fake vault + fake API client**
  ```kotlin
  @Test fun `connect sets WaitingForDevice when device code succeeds`() = runTest { val vm = buildVm(); vm.connect(); assertEquals(WaitingForDevice, vm.state.value.connectionStatus) }
  @Test fun `disconnect clears vault and repositories`() { ... }
  @Test fun `refreshRepositories keeps selected installation across reloads`() { ... }
  ```
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement state + ViewModel**
  - `connect()` calls `GitHubDeviceFlow.requestDeviceCode()`; on success starts polling coroutine.
  - `disconnect()` calls `vault.clear()` and resets state.
- [ ] **Step 4: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubIntegrationViewModelTest"`
  - Expected: PASS.

### Task 10: Compose Settings section for GitHub

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/GitHubSection.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt:121-134`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsNavigation.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsDetailPane.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsUiState.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values-bn/strings.xml`

**Interfaces:**
- `GitHubSection(uiState: GitHubIntegrationState, actions: GitHubIntegrationViewModel)`.
- New `SettingsCategory.GitHub("github", Res.string.settings_cat_github, Icons.Default.Code)`.
- Android and Desktop only; Web/iOS show "Not supported" text.

- [ ] **Step 1: Add strings**
  - English: `settings_cat_github`, `settings_github_title`, `settings_github_connect`, `settings_github_disconnect`, `settings_github_code`, `settings_github_verification_url`, `settings_github_installations`, `settings_github_repos`, `settings_github_read_only`.
  - Bengali: same keys with Bengali text.
- [ ] **Step 2: Add category routing**
  - In `DoraiSettingsScreen.kt`, add `SettingsCategory.GitHub(...)` after `Mcp`/`Integrations`.
  - Add routing in `SettingsNavigation.kt`, `SettingsDetailPane.kt`, `SettingsScreen.kt`.
- [ ] **Step 3: Implement `GitHubSection`**
  - Not connected: "Connect DORAI AI GitHub" button.
  - Waiting: show user code, verification URI, copy button, "I entered the code" refresh button, cancel.
  - Connected: show avatar/login, installations list, repositories list (name, private badge, select repository), "Disconnect" button.
  - Always show a read-only badge: `Contents read-only`.
- [ ] **Step 4: Screenshot test hook (if applicable)**
  - Add a small Compose UI test for the not-connected and connected states, or extend existing settings screen test.
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.ui.settings.*"`
  - Expected: PASS.

### Task 11: Manual smoke test — connect against real GitHub App

- [ ] **Step 1: Start desktop app**
  - Run: `./gradlew :composeApp:run` (if available) or `./gradlew :androidApp:assembleFossDebug` for Android.
  - Expected: app launches.
- [ ] **Step 2: Connect**
  - Settings → GitHub → Connect; enter code at `https://github.com/login/device`; select "Only select repositories" with one private repo.
- [ ] **Step 3: Verify**
  - User login appears; repositories list shows exactly the selected repo; `GET /user/installations/{installation_id}/repositories` did not return any unselected repo.
- [ ] **Step 4: Disconnect and reconnect**
  - Disconnect clears vault; reconnect succeeds and preserves the same login.

**Gate for Phase 3:** GitHub App connectivity works for personal and organization installations with selected repos only.

---

## Phase 3 — Native Git CLI Layer

### Task 12: Android guest Git read commands and `git clone`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/workspace/WorkspaceController.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/dorai/ai/workspace/runtime/WorkspaceEnvironmentManager.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/workspace/WorkspaceState.kt` (add read-only `WorkspaceGitLogEntry`)
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/workspace/WorkspaceGitLogParserTest.kt`

**Interfaces:**
- Add to `WorkspaceController`:
  ```kotlin
  suspend fun gitClone(project: String, url: String): WorkspaceGitResult
  suspend fun gitLog(project: String, maxCount: Int = 20): List<WorkspaceGitLogEntry>
  suspend fun gitFetch(project: String): WorkspaceGitResult
  suspend fun gitPullFfOnly(project: String): WorkspaceGitResult
  ```
- `WorkspaceGitLogEntry(hash: String, subject: String, authorName: String, dateIso: String)`.
- Clone target: `/root/projects/<project>` only; project name validated by `GitUrlValidator.isValidProjectName`.
- Before clone, check the destination does not already exist and refuse to overwrite.
- Clone command: `git clone --depth=1 --quiet <url> .` inside the project directory.
- Use one-shot `sandboxManager.createProotExecutor().execute`, never the persistent shell, so no state survives.
- No clone command may include `--recursive`, `--mirror`, or user-supplied flags.

- [ ] **Step 1: Write parser test**
  ```kotlin
  @Test fun `parses git log oneline output`() {
      val raw = "abc123 first subject\ndef456 second subject\n"
      val entries = parseGitLogOneline(raw)
      assertEquals(listOf("abc123", "def456"), entries.map { it.hash })
  }
  ```
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement `parseGitLogOneline` in `WorkspaceGitStatusParser.kt`**
- [ ] **Step 4: Implement `gitClone`**
  - Create project dir, run executor, map failure to `WorkspaceGitError`.
- [ ] **Step 5: Implement `gitLog`, `gitFetch`, `gitPullFfOnly`**
- [ ] **Step 6: Extend `NoOpWorkspaceController` with unsupported defaults**
- [ ] **Step 7: Run tests**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.workspace.*"`
  - Expected: PASS.

### Task 13: Desktop Git CLI layer

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitExecutableLocator.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitCommandRunner.desktop.kt`
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCommandRunner.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/dorai/ai/github/GitExecutableLocatorTest.kt`
- Test: `composeApp/src/desktopTest/kotlin/com/dorai/ai/github/GitCommandRunnerTest.kt`

**Interfaces:**
- `GitCommandRunner`:
  ```kotlin
  interface GitCommandRunner {
      suspend fun run(project: String, args: List<String>, timeoutSeconds: Long = 60): GitCommandResult
      suspend fun isGitAvailable(): Boolean
  }
  data class GitCommandResult(val success: Boolean, val stdout: String, val stderr: String, val exitCode: Int, val timedOut: Boolean = false)
  ```
- Desktop locator: check `git.exe` / `git` on PATH; on Windows also `Program Files\Git\cmd\git.exe` and `Program Files (x86)\Git\cmd\git.exe`; verify `--version` major ≥ 2.30.
- Android locator: `command -v git` inside proot guest; Debian guest rootfs already includes git.
- Projects root: `~/.dorai/projects/<project>` on desktop.

- [ ] **Step 1: Write failing locator test with injected filesystem**
  ```kotlin
  @Test fun `finds git on path`() { assertNotNull(locateOnPath("C:/Program Files/Git/cmd/git.exe", isWindows = true)) }
  @Test fun `rejects git without version output`() { ... }
  ```
- [ ] **Step 2: Implement `GitExecutableLocator`**
  - Cache result per process; expose `versionOf(path)`.
- [ ] **Step 3: Implement `GitCommandRunner.desktop.kt`**
  - `ProcessBuilder(listOf(gitPath) + args)`, `directory = ~/.dorai/projects/<project>`, environment only `HOME`, `PATH`, `SystemRoot` on Windows, `LANG`.
  - Never put tokens in args or env; use `GitCredentialBroker`.
  - `timeoutSeconds` enforced with `waitFor`.
- [ ] **Step 4: Write failing runner test with fake git script**
  - Use a temporary `fake-git` executable that echoes args; assert runner passes through validated args and rejects unsafe flags before spawning.
- [ ] **Step 5: Run tests**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.*"`
  - Expected: PASS.

### Task 14: Git credential broker — token never reaches the agent

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitCredentialBroker.kt`
- Create: `composeApp/src/androidMain/kotlin/com/dorai/ai/github/GitCredentialBroker.android.kt` (via `expect/actual`)
- Create: `composeApp/src/desktopMain/kotlin/com/dorai/ai/github/GitCredentialBroker.desktop.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/github/GitCredentialBrokerTest.kt`

**Interfaces:**
- `GitCredentialBroker.credentialArgs(command: List<String>): List<String>` — returns `git -c credential.helper='!dorai-git-helper' ...` style args or env, never the token literal.
- Broker creates a random one-time helper script/file under app-private tmp with mode `0600`/`0600`-equivalent, then deletes it on completion.
- Git asks helper for `username=x-access-token` / `password=<token>` (GitHub App user token).
- Helper output must be written directly to Git's stdin, not captured in logs.

- [ ] **Step 1: Write failing test**
  ```kotlin
  @Test fun `credential args never contain token`() {
      val args = broker.credentialGitConfigArgs(listOf("clone", "https://github.com/a/b"))
      assertFalse(args.joinToString().contains("ghs_"))
      assertTrue(args.any { it.contains("credential") })
  }
  @Test fun `ephemeral helper file is deleted after run`() { ... }
  ```
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement broker with `SecureTempDir`**
  - Android: helper lives in `context.cacheDir/git-askpass-<random>`; chmod 0600; delete in `finally`.
  - Desktop: same under `getAppFilesDirectory()/git-askpass-<random>`; Windows ACL owner-only.
- [ ] **Step 4: Wire runner to use broker only for network Git commands**
  - `clone`, `fetch`, `pull` — local commands (`status`, `log`, `diff`) run without credential helper.
- [ ] **Step 5: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitCredentialBrokerTest"`
  - Expected: PASS.

### Task 15: GitHub section — repository clone and sync UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/github/GitHubIntegrationViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/GitHubSection.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/workspace/WorkspaceController.kt` (new clone methods)

**Interfaces:**
- New state fields: `selectedRepository: GitHubRepository?`, `cloneState: CloneState`, `lastSync: Instant?`, `lastError: String?`.
- Actions: `selectAndClone(repository, projectName)`, `refreshGitStatus()`, `sync()`.
- Clone button only visible after repository selection; confirmation dialog shows full name and target folder.

- [ ] **Step 1: Add failing ViewModel test**
  ```kotlin
  @Test fun `clone creates project and marks it in state`() { ... }
  @Test fun `clone failure rolls back partial state`() { ... }
  ```
- [ ] **Step 2: Implement select-and-clone flow**
  - Validate `projectName` with `GitUrlValidator`.
  - Call `workspaceController.gitClone`.
  - On success, refresh `gitStatus`.
- [ ] **Step 3: Add UI states**
  - Repository list shows "Clone" button; cloned repos show branch and last sync time.
- [ ] **Step 4: Run test**
  - Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.github.GitHubIntegrationViewModelTest"`
  - Expected: PASS.

**Gate for Phase 4:** Clone and read-only Git operations work on Android and Windows with no write permission granted on the GitHub App.

---

## Phase 4 — Write Operations Behind Explicit Approval (future implementation, off by default)

> **Do not implement Phase 4 in the first production release.** It is specified here because the audit required "full integration" scope. Gate every Phase 4 task behind `writeOperationsEnabled: Boolean = false` and a separate maintainer sign-off.

### Task 16: Write-tool classification and approval reuse

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/data/ToolPermissionManager.kt:31-110`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/data/RemoteDataRepository.kt` (write tool execution paths, ~lines 740 and 1680)
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/tools/GitHubToolPermissionsTest.kt`

**Interfaces:**
- `github_commit`, `github_push`, `github_create_pull_request` → `ToolRiskLevel.OTHER`, `ASK_ALWAYS` default.
- Scope for `github_push`: `fullName` of the repository.

- [ ] **Step 1: Write failing test**
  ```kotlin
  @Test fun `github push requires confirmation`() {
      assertTrue(ToolPermissionManager(AppSettings(MapSettings())).shouldAskForPermission("github_push", mapOf("repo" to "a/b")))
  }
  ```
- [ ] **Step 2: Implement classification**
- [ ] **Step 3: Wire `RemoteDataRepository` to existing `confirmToolExecution` flow**
  - No new approval UI; reuse `pendingToolConfirmations`.

### Task 17: `git add`, `git commit`, `git push` with approval gate

**Files:**
- Modify: `GitCommandPolicy.kt` — add write subcommands behind feature flag.
- Modify: `GitCommandRunner.kt` / platform actuals — support `add`, `commit`, `push` only when flag enabled.
- Modify: `WorkspaceController.kt` — add `gitCommit(project, files, message)`, `gitPush(project)`.

**Interfaces:**
- All three refuse to run unless `GitHubIntegrationState.writeOperationsEnabled == true`.
- `git push` always uses `git push origin HEAD` with no force flags.
- `git commit` requires at least one staged file and a non-empty message ≤ 200 chars.

- [ ] **Step 1: Write failing test for explicit flag**
- [ ] **Step 2: Implement commit/push only behind `writeOperationsEnabled`**

### Task 18: Pull request creation via API

**Files:**
- Modify: `GitHubApiClient.kt` — add `createPullRequest` behind write flag.
- Modify: `GitHubIntegrationViewModel.kt` — expose `createPullRequest()` action.

**Interfaces:**
- `POST /repos/{owner}/{repo}/pulls` with `title`, `head`, `base`, `body`.
- Requires `Pull requests: Write` installation permission; if absent, show "Grant write permission in GitHub App settings" and do nothing.

---

## Phase 5 — Polling, Release Readiness, Docs, QA

### Task 19: Foreground-only refresh with ETag

**Files:**
- Modify: `GitHubApiClient.kt` and `GitHubIntegrationViewModel.kt`
- Test: `GitHubApiClientTest.kt` — add ETag 304 handling test.

- [ ] **Step 1: Add ETag cache**
  - In-memory `etagCache: MutableMap<String, String>` keyed by path; send `If-None-Match`; on 304 return cached body with `notModified = true`.
- [ ] **Step 2: Poll only on manual refresh and screen resume**
  - No timer-based polling in background. No WorkManager GitHub poll job in v1.

### Task 20: Documentation and release notes

- [ ] **Step 1: Create/update feature doc**
  - Path: `docs/features/github-integration.md` — behavior spec, GitHub App registration instructions, read-only default, write-approval gate, device flow screenshots/screens, known limits.
- [ ] **Step 2: Update changelog**
  - `CHANGELOG.md` — add user-facing entry under unreleased.
- [ ] **Step 3: Update knowledge bundle if GitHub App adds a public model endpoints list**
  - No new model catalog entries; no `FreeTierModels` / `ModelCatalog` changes needed.

### Task 21: End-to-end QA checklist

- [ ] Personal account installation with selected repos
- [ ] Organization installation with selected repos
- [ ] Private repository clone and `git log` on Android
- [ ] Windows clone and `git status` with portable Git
- [ ] Token refresh after 8h expiry (simulate by shortening `expires_in` in test build only)
- [ ] Disconnect clears vault and repo list
- [ ] Token never appears in logs, tool output, or agent timeline
- [ ] Device flow slow_down/expired handling
- [ ] Unsupported platform (iOS/Wasm) shows clear message

---

## Self-Review

### Spec coverage

- GitHub App Device Flow: Task 3, Task 9, Task 11.
- GitHub App user authorization: Task 3.
- User authorization, not installation JWT: Task 4 list of endpoints `/user`, `/user/installations`, `/user/installations/{installation_id}/repositories`.
- Installation scope any account/org: Phase 0 + Task 4.
- Local-first, Windows+Android: Tasks 2, 4, 12, 13.
- Native Git CLI: Tasks 12–15.
- Read-only default: Phase 0 permissions; Phase 3 Git policy; Phase 2 UI badge.
- Write approval-gated: Phase 4 flag.
- Webhook disabled/no custom domain/no cloud: Phase 0 + no webhook receiver code in any task.
- Credential safety: Tasks 2, 5, 6, 14.

### Step scan

Each step is one action. Tasks include exact file paths, exact test commands, and explicit failure output. No task contains unspecified "add appropriate validation" steps.

### Type consistency

- `GitHubTokenVault` used consistently across `GitHubApiClient`, `GitHubIntegrationViewModel`, platform actuals.
- `WorkspaceController` extended with same method names used by ViewModel.
- `GitHubConnectionStatus` used in state and UI.
- Write flag named `writeOperationsEnabled` everywhere.

### Review Focus (from spec)

- Token in logs/output/args → Tasks 5, 6, 14.
- ProcessManager bypass → Task 6.
- Device flow infinite polling → Task 3.
- Evil clone host/path → Task 8 and Task 12.
- Dangerous Git flags → Task 7.

### Proportion

This plan is long because the feature is a multi-subsystem integration. Each phase produces an independently testable, releasable slice: Phase 1 auth, Phase 2 read-only UI, Phase 3 native Git clone, Phase 4 optional writes, Phase 5 release hardening.
