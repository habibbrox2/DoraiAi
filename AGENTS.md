# AGENTS.md

## Overview

DORAI AI is a Kotlin Multiplatform (KMP) app built with Compose Multiplatform (CMP). Keep the Gradle root project name `dorai`, root packages, bundle IDs, namespace identifiers, and release names unchanged unless explicitly approved.

## Tech Stack

- **Language:** Kotlin Multiplatform
- **UI:** Compose Multiplatform
- **Targets (composeApp):** Android library target, iOS (arm64, simulator arm64), Desktop JVM (`desktop`), Web (`wasmJs` target exists; Web/Wasm distribution is not release-ready)
- **Modules:** `:composeApp` (shared KMP library/UI), `:androidApp` (Android app entrypoint, `playStore`/`foss` flavors), `:screenshotTests`
- **Future web/integration workspace:** TypeScript + pnpm under `frontend/` (independent from Gradle)

## Project Structure

```text
.
├── composeApp/        # Shared KMP code/UI (commonMain + platform source sets)
├── androidApp/        # Android application entrypoint
├── iosApp/            # iOS application/Xcode project
├── screenshotTests/   # Screenshot tests
├── docs/
│   ├── features/      # Product/behavior feature specs (no Kotlin code blocks)
│   └── knowledge/     # OKF knowledge bundles (YAML frontmatter + body)
├── frontend/          # Isolated TypeScript workspace (future features)
├── hybrid/            # Language-neutral protocol schemas/contracts
└── gradle/            # Version catalog and build config
```

### Source Set Rules

- `composeApp/src/commonMain/` — Shared business logic, state, and UI only. No `java.*`, `javax.*`, `android.*`, or JVM system calls. Common resources live in `commonMain/composeResources/`; SQLDelight schema lives in `commonMain/sqldelight/`.
- Platform code goes in `androidMain`, `iosMain`, `desktopMain`, or `wasmJsMain` (plus test source sets like `commonTest`, `desktopTest`).
- `androidMain` and `desktopMain` share JVM code via `composeApp/src/jvmShared/kotlin/`.
- If you change an `expect` in `commonMain`, update **all** corresponding `actual` implementations across active targets.
- Use Kotlin stdlib, `kotlinx-coroutines`, `kotlinx-datetime`, or other multiplatform-compatible libraries.

## Setup

- **Java:** 21 required. Verify with `java -version`.
- **Build tools:** Use the Gradle wrapper (`./gradlew` on Unix/macOS, `gradlew.bat` on Windows).
- **Discover tasks:** `./gradlew tasks --all`

## Build & Test

| Task | Purpose |
| --- | --- |
| `./gradlew :composeApp:test` | Common/shared tests (as configured) |
| `./gradlew :composeApp:desktopTest` | Desktop JVM tests |
| `./gradlew :androidApp:assembleFossDebug` | Android debug build (`foss`, default flavor) |
| `./gradlew :androidApp:assemblePlayStoreDebug` | Android debug build (`playStore` flavor) |
| `./gradlew :composeApp:wasmJsBrowserDistribution` | Web/Wasm distribution build (target exists; not release-ready) |

Run the narrowest relevant command first. Android release builds may require signing env vars (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`) or `ENFORCE_RELEASE_SIGNING=true` in release workflows. Only claim a target passes if it returns `BUILD SUCCESSFUL` in this environment.

## Kotlin + TypeScript Boundary

- Kotlin remains authoritative for application lifecycle, Compose UI, agent orchestration, provider credentials, tools, shell, filesystem, sandbox, and privileged capabilities.
- TypeScript work belongs in the independent `frontend/` pnpm workspace. Do not add it to Gradle settings or Android runtime dependencies.
- Existing Kotlin/Wasm is a Kotlin target; it is not the TypeScript workspace.
- Cross-language messages use versioned JSON Schema under `hybrid/contracts/`. Schema validation does not grant authorization; Kotlin must authorize every capability.
- Do not migrate or edit existing Kotlin/JavaScript/TypeScript feature implementation as part of workspace setup.
- Use `pnpm frontend:check` for isolated workspace checks. Keep Gradle and pnpm dependency graphs independent.

## Coding Conventions

- **State:** Prefer state hoisting, unidirectional data flow (UDF), and Material 3 patterns.
- **Strings/resources:** Use Compose Resources (`Res.string.*`) from `composeApp/src/commonMain/composeResources/`. Avoid hardcoded strings.
- **APIs:** Never guess signatures. Check `gradle/libs.versions.toml` and existing usages first.
- **Dependencies:** Don't add dependencies/plugins to fix errors. Verify multiplatform support and get approval before changing `gradle/libs.versions.toml`.
- **Safety:** Don't weaken security configs (network security, SSRF filters, shell allowlists, encrypted storage, signing) to make builds/tests pass.

## Documentation Rules

### Feature docs (`docs/features/`)

If you modify logic for a feature that has a doc in `docs/features/`:

- Update the doc to reflect the new behavior
- Update the "Last verified" date in the doc header
- Keep the "Key Files" table accurate (add/remove files as needed)

### Knowledge bundles (`docs/knowledge/`) — OKF format

Knowledge lives under `docs/knowledge/` as OKF-style markdown (YAML frontmatter + body). Runtime values come from code; refresh **both** the bundle and runtime when updating policy:

| Bundle | Runtime source | Update via |
| --- | --- | --- |
| `docs/knowledge/free-tier/` | `FreeTierModels.kt` | `/update-free-tier-models` |
| `docs/knowledge/model-catalog/` | `ModelCatalog.kt` | `/update-model-catalog` |
| `docs/knowledge/popular-mcp/` | `PopularMcpServers.kt` | `/update-popular-mcp-servers` |
| `docs/knowledge/litert/` | `LocalModelCatalog.kt` | `/update-litert-models` (no silent bumps) |

**Slash-command availability:** The update commands above are agent-environment commands, not repository-local scripts. Verify that each command is available in the active agent environment before invoking it. If unavailable, do not claim it exists or silently substitute another workflow; inspect the available skills/scripts and request direction when no equivalent is clearly defined.

### `CHANGELOG.md`

- Update `CHANGELOG.md` when a change is user-facing, release-relevant, or otherwise notable (features, fixes, security hardening, behavior changes, migrations, deprecations/removals). Do not add entries for routine refactors or internal-only doc maintenance unless they affect users/builds/releases.
- Follow the existing file style and section structure; place entries under the correct release/unreleased section used by the project.
- Be factual and specific. Never invent shipped versions, dates, or changes; if release status/versioning is unclear, ask before editing.

### `docs/` cleanup

- Keep authoritative/current docs: `docs/features/`, `docs/knowledge/`, `docs/architecture/`, `docs/development/`, `docs/branding/`, `docs/design-system/`, release/update/provider/getting-started docs, and other docs actively referenced by code, README, AGENTS, CI, or users.
- Before deleting docs, search for references (`git grep`, path/name search) and check `git status`/ownership of untracked files. Do not delete untracked docs without explicit user confirmation.
- Candidates for removal are obsolete/superseded planning, migration, phase, or audit artifacts for completed work that are no longer referenced or needed. Prefer archiving only if the user asks; otherwise delete with explicit confirmation and list what was removed.
- If a doc is historical but still useful (security implementation record, release/smoketest procedure, schema/contract reference), keep it unless the user explicitly approves deletion.

## Workflow for Agents

1. **Inspect** — Check `git status --short --branch`, read related files, and review `CLAUDE.md` before editing.
2. **Reproduce** — Run the narrowest failing command and keep its error output.
3. **Plan** — Make a minimal, targeted change. If scope is unclear or touches build/CI/deps, ask first.
4. **Change** — Edit only what's necessary. Follow existing patterns in nearby files.
5. **Verify** — Run relevant tests/builds and confirm `BUILD SUCCESSFUL` where claimed.
6. **Report** — Summarize changed files, root cause, commands run + results (PASS/FAIL), skips, and residual risks.

## Agent Behavior

- **Subagents:** Use subagents when helpful to complete work faster, especially for independent searches, checks, or parallelizable tasks. Keep delegation scoped and report merged results clearly.
- **Session task queue:** If a new task is requested while work is already in progress in the same session, record it in the todo/task plan so the current task is completed first, then start the new task. Do not silently drop or switch context mid-task.
- **Question Pool:** Use AskUserQuestion (Question Pool) for suggestions, choices, clarifications, and decisions that need user input. Do not guess when a concise question can resolve ambiguity.
- **After completion:** When a task is complete, suggest the next urgent or necessary follow-up work with concrete, actionable options.

## Git & CI

- **Commits:** Use short, imperative messages (e.g., `fix(desktop): resolve window resize crash`). `CLAUDE.md` has no commit-footer/attribution rule; follow the active session/assistant instructions for any required commit or PR attribution when creating commits/PRs.
- **Don't push/commit by default:** Only commit/push/create tags if explicitly asked.
- **Pre-commit formatting gate:** After tests and verification succeed, and before creating any git commit or push, run `./gradlew spotlessCheck` (`gradlew.bat spotlessCheck` on Windows). Only commit and push if `spotlessCheck` returns `BUILD SUCCESSFUL`. If it fails, fix formatting issues (e.g., `./gradlew spotlessApply` if appropriate and safe), re-run relevant verification as needed, and re-run `spotlessCheck` before committing/pushing.
- **CI:** `.github/workflows/test.yml` runs formatting/screenshot/asset steps and may push changes. Don't trigger it casually.
- **Preserve changes:** Never use destructive git commands or overwrite unrelated user changes.
