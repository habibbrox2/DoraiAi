# GitHub → Workspace Move Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Execute one task at a time, keep the checkbox list in this file updated.

**Goal:** Rebrand the DORAI AI source-repository integration from the user-facing identity "GitHub" to "Workspace" — with GitHub remaining the first supported host/provider — starting with the Settings category surface and then finishing user-facing copy.

**Architecture:** Phase 1 renames only user-facing identity: the `SettingsCategory` entry and route id, the settings tab label, the category title/subtitle, and the section heading, backed by renamed Compose Resources keys and values. Implementation internals (`com.dorai.ai.github` package, `GitHubSection` composable body, `settings_github_*` feature strings, token vaults, deep links, tool layer, `GitHubIntegrationViewModel`) intentionally stay unchanged in Phase 1 — the GitHub App is still the host/provider. Phase 2 (separate plan) migrates internal naming to Workspace-first. Phase 3 finishes user-facing copy/docs.

**Tech Stack:** Kotlin Multiplatform / Compose Multiplatform, Compose Resources (`Res.string.*`), kotlin.test, Paparazzi screenshot tests.

**Spec:** `docs/features/github-integration.md` (GitHub integration behavior) + scope note in `docs/superpowers/plans/2026-11-17-settings-uiux-shared-foundation.md` (the "GitHub → Workspace" move gets its own plan).

## Global Constraints

- Java 21 required; no new Gradle dependencies; `gradle/libs.versions.toml` unchanged without approval.
- `commonMain` only: no `java.*`/`javax.*`/`android.*`; strings via `Res.string.*` from `composeApp/src/commonMain/composeResources/values/strings.xml` (+ `values-bn/` for Bengali); no hardcoded copy; add all target locales for any new key (verify `composeResources/values-*/strings.xml`).
- Bengali/Unicode policy applies (`docs/development/bengali-language-and-rendering-policy.md`); `values-bn` must be updated in lockstep when renaming Bengali-facing keys.
- Reuse existing components/tokens; DRY; accessibility floor (48dp, semantics, heading()); security configs preserved; verification before each commit; branch policy (task branch, never main); screenshot gate for global changes; no new dependencies.

## Review Focus

1. Compose Resources key renames must keep generated `Res` symbols in sync (the build regenerates them; stale imports of removed keys fail compilation).
2. Bengali locale (`values-bn`) lockstep update for renamed keys.
3. Settings route category id round-trips (`SettingsRouteTest.everyCategoryRoundTrips`).
4. Screenshot goldens for the renamed category (`:screenshotTests:verifyPaparazziDebug`).
5. No behavior change beyond naming.

---

### Task 1: Rebrand the Settings category identity GitHub → Workspace (Phase 1)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt` (category enum entry + imports + both `when` branches + section heading key)
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsScreen.kt` (tab label resource import)
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/App.kt` (current settings category assignment + comment)
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml` (rename keys + values)
- Modify: `composeApp/src/commonMain/composeResources/values-bn/strings.xml` (rename keys + values)
- Modify: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreenTest.kt` (expected category ids)
- Modify: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/AppNavigationScreenshotTest.kt` (category enum reference)

**Interfaces:**
- Produces: `SettingsCategory.Workspace` (`id = "workspace"`, `titleRes = Res.string.settings_cat_workspace`, `subtitleRes = Res.string.settings_page_workspace_description`, `icon = Icons.Default.Code`); tab label `Res.string.settings_tab_workspace`; section heading `Res.string.settings_workspace_connection`.
- Consumes: existing `Settings` route category-id round-trip and `Settings.categoryOf`.

- [ ] **Step 1: Write a failing test**

Update `DoraiSettingsScreenTest`: the ordered category id list ends with `"workspace"`, and the category entry is `SettingsCategory.Workspace`.

- [ ] **Step 2: Run test to verify it fails**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.ui.settings.DoraiSettingsScreenTest"`
Expected: FAIL (unresolved `SettingsCategory.Workspace` / `"workspace"`).

- [ ] **Step 3: Implement the rebrand**

- Rename the enum entry `GitHub("github", ...)` → `Workspace("workspace", Res.string.settings_cat_workspace, Res.string.settings_page_workspace_description, Icons.Default.Code)`; update both `SettingsCategory.GitHub ->` branches to `SettingsCategory.Workspace ->` and the heading key to `Res.string.settings_workspace_connection`.
- Update the tab-label import in `SettingsScreen.kt` to `settings_tab_workspace`.
- Update `App.kt` to assign `SettingsCategory.Workspace`.
- Rename + retitle strings in `values/strings.xml` and `values-bn/strings.xml`:
  - `settings_tab_github` → `settings_tab_workspace` = "Workspace"
  - `settings_cat_github` → `settings_cat_workspace` = "Workspace"
  - `settings_page_github_description` → `settings_page_workspace_description` = "Connect source repositories and clone them into your workspace."
  - `settings_github_connection` → `settings_workspace_connection` = "Connection"
- Keep the composable name `GitHubSection` and the `settings_github_*` feature strings unchanged in this task (internal naming migration is Phase 2).

- [ ] **Step 4: Run test to verify it passes**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.ui.settings.DoraiSettingsScreenTest"`
Expected: PASS.

- [ ] **Step 5: Run screenshot gate + spotless**

Run: `gradlew.bat :screenshotTests:verifyPaparazziDebug` and `gradlew.bat spotlessCheck`
Expected: PASS; re-record goldens via `recordPaparazziDebug --tests "..."` if intentional diffs appear.

- [ ] **Step 6: Commit**

```bash
git add <task-scope-files>
git commit -m "feat(settings): rebrand GitHub category identity to Workspace"
```

### Task 2: Rename the GitHub settings section composable to Workspace (Phase 1 completion)

**Goal:** Move the GitHub-backed source-repository connection screen under the now-renamed Workspace category — rename the section composable/file/test to the Workspace identity while keeping the GitHub App domain types and `settings_github_*` feature strings (Phase 3 owns those).

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/WorkspaceSection.kt` (via `git mv` from `GitHubSection.kt`)
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt` (2 call sites)
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/WorkspaceSectionTest.kt` (via `git mv` from `GitHubSectionTest.kt`)
- Modify: `docs/features/github-integration.md` (Key Files row)

**Interfaces:**
- Produces: `internal fun WorkspaceSection()`.
- Consumes: `GitHubIntegrationViewModel`, `GitHubConnectionStatus`, `Res.string.settings_github_*`.

- [ ] **Step 1: Write a failing signature test**

Add to the renamed `WorkspaceSectionTest`:

```kotlin
@Test
fun `workspace section composable is exposed`() {
    val reference: Any = ::WorkspaceSection
    assertTrue(reference.toString().contains("WorkspaceSection"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.ui.settings.WorkspaceSectionTest"`
Expected: FAIL (unresolved `WorkspaceSection`).

- [ ] **Step 3: Implement the rename**

- `git mv GitHubSection.kt WorkspaceSection.kt`; rename `internal fun GitHubSection()` → `internal fun WorkspaceSection()`; update KDoc title/comment.
- `git mv GitHubSectionTest.kt WorkspaceSectionTest.kt`; rename class + add the Step 1 signature test.
- Update both `GitHubSection()` call sites in `DoraiSettingsScreen.kt` → `WorkspaceSection()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.ui.settings.WorkspaceSectionTest"`
Expected: PASS.

- [ ] **Step 5: Run screenshot gate + spotless**

Run: `gradlew.bat :screenshotTests:verifyPaparazziDebug` and `gradlew.bat spotlessCheck`
Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add <task-scope-files>
git commit -m "feat(settings): rename GitHub section composable to Workspace"
```

### Task 3: Phase 2 — internal Workspace-first naming migration

**Goal:** Rename the GitHub integration internals from GitHub-first to Workspace-first: package, integration-level classes/state/VM, vault keys, deep links, notifications, feature string keys.

**Naming decisions (Task 3):**
- Package: `com.dorai.ai.github` → `com.dorai.ai.source`
- Integration-level types: `GitHubIntegrationState` → `WorkspaceIntegrationState`, `GitHubIntegrationViewModel` → `WorkspaceIntegrationViewModel`, `GitHubApiClient` → `WorkspaceApiClient`, `GitHubApiResult` → `WorkspaceApiResult`
- Host-specific types (GitHub App config/device flow/token vault/repo/git runner policy — factual GitHub concepts) migrate in later sub-tasks if desired.

**Sub-tasks:**
- [x] **Task 3.1: Package + integration class/state/VM rename** — move `com/dorai/ai/github` → `com/dorai/ai/source` across source sets; rename the four types above; update all imports/references; verify. (committed as `e03547a8c`)
- [x] **Task 3.2: Vault keys, deep links, notifications, feature strings** — `dorai_github_vault_v1` → `dorai_workspace_vault_v1`, `github-vault.*` → `workspace-vault.*`, deep-link constants, notification keys, `settings_github_*` → `settings_workspace_*`. (committed as `97ca61f`)

### Task 4: Phase 3 — user-facing feature copy, notification text, docs sync

**Goal:** Finish the GitHub → Workspace move: shift remaining user-facing copy (hero/clone/connect/notification/error/disconnect) from the GitHub identity to Workspace, keep `values-bn` in lockstep, and sync the feature doc + CHANGELOG.

**Sub-tasks:**
- [x] **Task 4.1: User-facing string values + hardcoded copy** — hero/section/notification/connect/disconnect/error copy now reads "Workspace"; hardcoded VM message aligned; `values-bn` updated in lockstep. (committed as part of Phase 3 commit)
- [x] **Task 4.2: Docs + changelog** — `docs/features/github-integration.md` + `CHANGELOG.md` updated. (committed as part of Phase 3 commit)

## Out of Scope (later plans)

- Phase 2: internal Workspace-first naming (`com.dorai.ai.workspace` package, `WorkspaceIntegrationState`/`WorkspaceIntegrationViewModel`, token vault keys, deep links, notifications, feature string keys).
- Phase 4: write operations remain deferred (default-off maintainer opt-in + MCP-only approvals, no UI exposure yet).
