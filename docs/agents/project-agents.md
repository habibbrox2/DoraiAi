# Project Agent — Instructions Map

**Last verified:** 2026-11-12

The **Project Agent** is every coding agent or CI bot that changes this repository —
including OpenCode sessions, Claude Code, and CI/CD systems. This document is the
canonical map of where its instructions live and how to change them safely.

> The App Runtime Agent (the in-app Dorai AGENT) has a separate map:
> [runtime-agents.md](runtime-agents.md). Do not add runtime-agent content here.

## Files, by path

| Path | Owner | Purpose |
|---|---|---|
| `AGENTS.md` (repo root) | Repository | **Canonical repository-wide policy for every coding agent.** Tech stack, source-set rules, build/test commands, branch & PR policy, localization policy, multi-agent coordination, documentation rules. `CLAUDE.md` defers to it. |
| `CLAUDE.md` | Claude Code | Entry point for Claude Code; delegates to `AGENTS.md`, adds Claude-specific workflow preferences. |
| `agents/task-protocol.md` | Repository | How multi-agent tasks are claimed, executed and verified (read before parallel or multi-agent work). |
| `agents/file-ownership.md` | Repository | Which agent owns which files; hotspots requiring explicit approval. |
| `agents/validation-policy.md` | Repository | What must be verified before hand-off; which profiles to run. |
| `agents/architecture-guide.md` | Repository | Code architecture orientation for agents working in the repo. |
| `agents/build-and-test-guide.md` | Repository | Build/test recipes and failure triage pointers. |
| `agents/security-guide.md` | Repository | Security-sensitive changes policy (network, shell, storage, signing, sandbox). |
| `agents/source-set-guide.md` | Repository | Kotlin Multiplatform source-set rules for this repo. |
| `agents/compose-api-migration.md` | Repository | Compose API migration guidance. |
| `agents/compose-ui-repair-specialist.md` | Repository | Repair protocol for Compose Multiplatform UI issues. |
| `agents/build-test-verifier.md` | Repository | Verification protocol used by the `build-test-verifier` subagent. |
| `agents/feature-docs-curator.md` | Repository | Updating `docs/features/` correctly. |
| `agents/kmp-compatibility-auditor.md` | Repository | Source-set compatibility audit protocol. |
| `agents/resources-and-localization.md` + `agents/resources-localization-specialist.md` | Repository | Compose Resources / i18n workflow and specialist protocol. |
| `agents/security-reviewer.md` | Repository | Security review protocol for agent-run reviews. |
| `agents/wasm-build-specialist.md` | Repository | Web/Wasm target tasks and constraints. |
| `agents/README.md` | Repository | Index of the `agents/` directory. |
| `.agents/rules/dorai-project.md` | Repository | Agent-platform rules file (skills discovery, repo conventions for tools that read `.agents/rules/`). |
| `.agents/skills/<id>/SKILL.md` | Repository | Agent skills consumed by agent runtimes that support skills (e.g. `compose-resources`, `gradle-build-diagnostics`, `browser`, `skill-creator`). Each skill has frontmatter (`id`, `name`, `description`). |
| `docs/agents/` | Repository | **This directory** — central index and governance of all agent-instruction files. |
| `docs/architecture/` | Repository | Architecture decisions (not agent policy). |
| `.github/workflows/test.yml` | Repository | Read-only CI: formatting/assets checks, platform tests, screenshot verification. Never commits generated content. |

## Rules

1. `AGENTS.md` is authoritative for every coding agent; `CLAUDE.md` must not contradict it.
2. New repository-wide agent guidance is added to an existing guide in `agents/` or as a new
   `agents/<name>.md` with an `agents/README.md` entry. Root files stay thin.
3. Agent skills (`.agents/skills/`) are for agent runtimes, not the app. A skill that would
   change app runtime behavior must be reviewed like app code.
4. `main` is protected: integrate via PR only, one logical task per branch, squash merge.
   See `AGENTS.md` §Branch & Pull Request Policy.
5. Bengali/i18n policy is mandatory reading before any localized string, chat rendering,
   IME or Unicode work. See `AGENTS.md` §Bengali Language and
   `docs/development/bengali-language-and-rendering-policy.md`.

## Verification

Before merging a change to an agent-instruction file or `.github/workflows/`:

- Markdown changes: spell-check/consistency review; no code blocks needed unless an
  existing guide carries one and it is verified against the current source.
- Workflow changes: read-only rule of `test.yml` must hold; never add steps that commit.
- Scope: run the relevant profiler per `agents/validation-policy.md` —
  `powershell scripts/verify-agent-scope.ps1 -Staged` when available, and `spotlessCheck`
  when Kotlin source is touched.

## See also

- [README.md](README.md) — the two-family separation
- [runtime-agents.md](runtime-agents.md) — the App Runtime Agent (in-app)
- [`AGENTS.md`](../../AGENTS.md) — repository-wide policy (authoritative)
- [`docs/development/bengali-language-and-rendering-policy.md`](../development/bengali-language-and-rendering-policy.md) — Bengali/i18n policy
