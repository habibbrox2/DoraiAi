# DORAI AI — Project Agent Catalog

These are project-specific subagent definitions. Canonical, Git-trackable copies live in `agents/`; matching local Claude Code copies live in `.claude/agents/` because this checkout's `.gitignore` intentionally ignores `.claude/`.

## Agents and when to use them

| Agent | Use for | Default posture |
|---|---|---|
| `kmp-compatibility-auditor` | Source-set leaks, JVM-only APIs, expect/actual drift | Read-only audit |
| `wasm-build-specialist` | Browser Wasm compiler errors and distribution failures | Diagnose first; edit only when explicitly asked |
| `compose-ui-repair-specialist` | Compose syntax/import/state/focus/API mismatch | Diagnose first; edit only when explicitly asked |
| `security-reviewer` | MCP SSRF, shell execution, Android network policy, storage and release integrity | Read-only review |
| `build-test-verifier` | Gradle task discovery and Android/Desktop/Wasm validation | Verification only; no implementation |
| `resources-localization-specialist` | Compose Resources, missing strings, locale consistency | Diagnose first; edit only when explicitly asked |
| `feature-docs-curator` | Feature specs, knowledge bundles, agent documentation | Documentation-only when authorized |

## Recommended routing
- **Wasm build failure:** `kmp-compatibility-auditor` → `wasm-build-specialist` → `build-test-verifier`.
- **Compose UI error:** `compose-ui-repair-specialist` → `build-test-verifier`.
- **Security-sensitive change:** `security-reviewer` before and after implementation.
- **Missing string/resource:** `resources-localization-specialist` → `build-test-verifier`.
- **Feature behavior change:** inspect feature docs first; use `feature-docs-curator` to review authorized doc updates.

## Shared rules
All agents must follow root `AGENTS.md` and read the relevant project skills under `skills/`. No agent may overwrite unrelated user changes, silently change dependency versions, weaken security, or commit/push/release/deploy without explicit authorization. Read-only diagnosis is the default unless the task clearly authorizes implementation.


# AI Agent Guide Index — DORAI AI

These guides describe this repository's current structure and workflows, not a generic Compose template. Re-verify paths, task names, and versions in the checked-out tree before relying on examples.

## Read in this order
1. [Repository architecture](architecture-guide.md)
2. [Source sets and platform APIs](source-set-guide.md)
3. [Build and test workflow](build-and-test-guide.md)
4. [Compose API repair](compose-api-migration.md)
5. [Resources and localization](resources-and-localization.md)
6. [Security and regression checklist](security-guide.md)

## Skills
Canonical portable skills live in `skills/<skill-name>/SKILL.md`. Claude-discoverable copies are generated into `.claude/skills/` by the sync script. Other agents use `.agents/skills/` where supported.
- `kmp-compatibility-audit`
- `compose-ui-repair`
- `gradle-build-diagnostics`
- `compose-resources`
- `security-regression`
- `cross-platform-validation`

## Repository-specific notes
- Gradle modules: `:composeApp` (shared KMP library/UI), `:androidApp` (Android app, `playStore`/`foss` flavors), `:screenshotTests`. `iosApp/` contains the iOS Xcode project.
- `composeApp` configures Android, iOS arm64/simulator arm64, JVM desktop (`desktop` target), and browser `wasmJs` (target exists; Web/Wasm distribution is not release-ready).
- `commonMain` includes Compose, Navigation, Lifecycle, Ktor, Koin, FileKit, Coil, multiplatform settings, kotlinx-datetime/serialization, and SQLDelight runtime (verify current deps in `gradle/libs.versions.toml`).
- Shared expect/actual platform contracts are in `composeApp/src/commonMain/kotlin/com/dorai/ai/Platform.kt` and matching platform `actual` files.
- The version catalog is the source of truth for plugin/library versions: `gradle/libs.versions.toml`. The Gradle root project name is `dorai`; namespaces/packages use `com.dorai.ai` (do not rename identifiers without explicit approval).
- `AGENTS.md` is the sole repository-wide agent/contributor policy. `CLAUDE.md` is the Claude Code entry point and must not redefine project-wide rules. Product feature specs remain in `docs/features/`; curated knowledge bundles remain in `docs/knowledge/`.

## Multi-agent workflow

Read [task protocol](task-protocol.md), [file ownership](file-ownership.md), and [validation policy](validation-policy.md). Use the worktree, lock, scope-check, and validation scripts in `../scripts/`. See [the full development workflow](../docs/development/multi-agent-workflow.md).
