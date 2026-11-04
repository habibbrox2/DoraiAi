# AGENTS.md

## Overview

DORAI AI is a Kotlin Multiplatform (KMP) app built with Compose Multiplatform (CMP). Keep the Gradle root project name `Kai`, root packages, bundle IDs, namespace identifiers, and release names unchanged unless explicitly approved.

## Tech Stack

- **Language:** Kotlin Multiplatform
- **UI:** Compose Multiplatform
- **Targets:** Android, iOS (arm64, simulator arm64), Desktop JVM, Web (`wasmJs`)
- **Future web/integration workspace:** TypeScript + pnpm under frontend/ (independent from Gradle)
- **Modules:** `:composeApp`, `:androidApp`, `:screenshotTests`

## Project Structure

```text
.
├── composeApp/        # Shared code and UI (commonMain + platform source sets)
├── androidApp/        # Android entrypoint
├── screenshotTests/   # Screenshot tests
├── docs/
│   ├── features/      # Product/behavior feature specs (no Kotlin code blocks)
│   └── knowledge/     # OKF knowledge bundles (YAML frontmatter + body)
├── frontend/          # Isolated TypeScript workspace (future features)
├── hybrid/            # Language-neutral protocol schemas
└── gradle/            # Version catalog and build config
```

### Source Set Rules

- `composeApp/src/commonMain/` — Shared business logic, state, and UI only. No `java.*`, `javax.*`, `android.*`, or JVM system calls.
- Platform code goes in `androidMain`, `iosMain`, `desktopMain`, or `wasmJsMain`.
- `androidMain` and `desktopMain` share JVM code via `composeApp/src/jvmShared/kotlin/`.
- If you change an `expect` in `commonMain`, update **all** corresponding `actual` implementations across active targets.
- Use Kotlin stdlib, `kotlinx-coroutines`, `kotlinx-datetime`, or other multiplatform-compatible libraries.

## Setup

- **Java:** 21 required. Verify with `java -version`.
- **Build tools:** Use the Gradle wrapper (`./gradlew` on Unix/macOS, `gradlew.bat` on Windows).
- **Discover tasks:** `./gradlew tasks --all`

## Build & Test

| Task | Purpose |
|---|---|
| `./gradlew :composeApp:test` | Common unit tests |
| `./gradlew :composeApp:jvmTest` | Desktop/JVM tests |
| `./gradlew :composeApp:assembleDebug` | Android debug build |
| `./gradlew :composeApp:wasmJsBrowserDistribution` | Web/Wasm distribution build |

Run the narrowest relevant command first. Only claim a target passes if it returns `BUILD SUCCESSFUL` in this environment.

## Kotlin + TypeScript Boundary

- Kotlin remains authoritative for application lifecycle, Compose UI, agent orchestration, provider credentials, tools, shell, filesystem, sandbox, and privileged capabilities.
- TypeScript work belongs in the independent frontend/ pnpm workspace. Do not add it to Gradle settings or Android runtime dependencies.
- Existing Kotlin/Wasm is a Kotlin target; it is not the TypeScript workspace.
- Cross-language messages use versioned JSON Schema under hybrid/contracts/. Schema validation does not grant authorization; Kotlin must authorize every capability.
- Do not migrate or edit existing Kotlin/JavaScript/TypeScript feature implementation as part of workspace setup.
- Use pnpm frontend:check for isolated workspace checks. Keep Gradle and pnpm dependency graphs independent.

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
|---|---|---|
| `docs/knowledge/free-tier/` | `FreeTierModels.kt` | `/update-free-tier-models` |
| `docs/knowledge/model-catalog/` | `ModelCatalog.kt` | `/update-model-catalog` |
| `docs/knowledge/popular-mcp/` | `PopularMcpServers.kt` | `/update-popular-mcp-servers` |
| `docs/knowledge/litert/` | `LocalModelCatalog.kt` | `/update-litert-models` (no silent bumps) |

## Workflow for Agents

1. **Inspect** — Check `git status --short --branch`, read related files, and review `CLAUDE.md` before editing.
2. **Reproduce** — Run the narrowest failing command and keep its error output.
3. **Plan** — Make a minimal, targeted change. If scope is unclear or touches build/CI/deps, ask first.
4. **Change** — Edit only what's necessary. Follow existing patterns in nearby files.
5. **Verify** — Run relevant tests/builds and confirm `BUILD SUCCESSFUL` where claimed.
6. **Report** — Summarize changed files, root cause, commands run + results (PASS/FAIL), skips, and residual risks.

## Git & CI

- **Commits:** Use short, imperative messages (e.g., `fix(desktop): resolve window resize crash`). Don't add AI attribution/boilerplate footers.
- **Don't push/commit by default:** Only commit/push/create tags if explicitly asked.
- **CI:** `.github/workflows/test.yml` runs `spotlessApply`, captures screenshots, converts assets, and may push changes. Don't trigger it casually.
- **Preserve changes:** Never use destructive git commands or overwrite unrelated user changes.
