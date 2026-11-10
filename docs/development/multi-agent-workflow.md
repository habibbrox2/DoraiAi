# Multi-Agent Development Workflow

DORAI AI supports local Windows/macOS/Linux development and GitHub pull-request validation with one canonical policy and native editor/agent adapters.

## Core rules

- `AGENTS.md` is the only repository-wide policy source. `CLAUDE.md` is a Claude Code entry point and may contain Claude-only additions.
- Canonical specialist definitions live in `agents/`; canonical reusable skills live in `skills/`.
- Native adapters are intentionally short and point back to canonical rules. Generated Claude copies are synchronized with `scripts/sync-agent-configs.ps1`.
- One concurrent task = one Git branch + one Git worktree + one task contract. Do not share a working directory between agents.
- Reserve overlapping paths before editing. Locks are shared by linked worktrees on the same machine, not across separate machines.
- Task contracts and lock files are coordination, not hard security boundaries. Use containers/OS permissions for untrusted agents.

## Supported agent entry points

| Agent | Repository entry point |
|---|---|
| GitHub Copilot Chat | `AGENTS.md`, `.github/copilot-instructions.md`, `.github/instructions/` |
| Claude Code | `CLAUDE.md`, `.claude/settings.json`, generated `.claude/agents/` and `.claude/skills/` |
| Antigravity | `AGENTS.md`, `.agents/rules/`, `.agents/skills/` |
| Codex | `AGENTS.md`, `.agents/skills/` |
| Kilo Code | `AGENTS.md`, `.kilocode/rules/`, `.agents/skills/` |
| OpenCode CLI/Desktop | `AGENTS.md`, `.opencode/agents/`, `.agents/skills/` |
| Freebuff | `AGENTS.md`, `.agents/rules/`, `.agents/skills/` |
| Cursor | `AGENTS.md`, `.cursor/rules/` |
| Windsurf | `AGENTS.md`, `.windsurf/rules/` |

Agent versions and discovery behavior can change. On upgrades, verify each adapter is actually loaded in the installed client; a file's presence alone is not proof of discovery.

## Local setup (Windows)

From a clean/current checkout, run:

```powershell
# Verify native Claude copies match canonical sources
.\scripts\sync-agent-configs.ps1
.\scripts\sync-agent-configs.ps1 -Check

# Create a separate worktree and task branch from origin/main
.\scripts\agent-worktree.ps1 -Action Create -Agent copilot -Task chat-ui `
  -AllowedPaths 'composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/**' `
  -ReservedPaths 'gradle/**','build.gradle.kts' -ValidationProfile Feature
```

Open the printed worktree path as a separate VS Code/Cursor/Antigravity/OpenCode workspace, or launch Claude/Codex/Kilo/Freebuff from that directory. Never launch multiple agents against the same worktree.

Before the agent edits, reserve the same scope:

```powershell
.\scripts\agent-lock.ps1 -Action Acquire -TaskId DORAI-CHAT-UI -Agent copilot `
  -Paths 'composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/**'
```

The task's `.agent-task.json` is local and ignored by Git. It contains task ID, agent, branch, base ref, allowed paths, reserved paths, and validation profile.

## Handoff

```powershell
.\scripts\verify-agent-scope.ps1
.\scripts\validate-changes.ps1 -Profile Auto -Base origin/main -WhatIf
.\scripts\validate-changes.ps1 -Profile Auto -Base origin/main
.\scripts\agent-lock.ps1 -Action Release -TaskId DORAI-CHAT-UI -Agent copilot
```

Commit only task-owned files when authorized. Push and open a PR only when explicitly authorized. The PR should target `main` and include task scope, tests run, tests skipped, and residual risks.

## Worktree lifecycle

- `List` and `Status` are read-only.
- `Create` fetches `origin/<base>` and refuses to overwrite existing paths or branches.
- `Remove` only removes a clean branch whose merge is verifiable against the selected `origin/<base>`: ancestry, an identical final tree (including squash merges), or a merged GitHub PR targeting that base when `gh` is installed and authenticated. Dirty/unmerged worktrees must be preserved and reviewed manually.
- The helper never commits, pushes, merges, stashes, resets, or cleans.
- Do not run `git worktree prune` as routine cleanup while agents may be active.

## Validation tiers

See `agents/validation-policy.md`. `Auto` selects docs-only, fast, feature, or full validation from changed paths. Screenshot-only changes run Paparazzi verification; `frontend/`, the root package manifest, pnpm lock/workspace files, and shared hybrid contracts run `pnpm run frontend:check`. Explicit `Full` and `Wasm` profiles still execute when the worktree has no changed files; `Full` includes screenshots and frontend checks. Gradle/version catalog/wrapper, SQLDelight schema, shared contract, CI, security, and high-contention architecture changes are treated conservatively. A local targeted pass never replaces the PR's required checks.

## GitHub CI behavior

`.github/workflows/test.yml` first classifies changed paths, then runs only applicable quality, frontend, Desktop, Android, and screenshot jobs. Frontend changes install the pinned pnpm toolchain and run the frozen-lockfile frontend check. The stable `Run Unit Tests` aggregate job always runs. It accepts an intentionally skipped job only when the classifier says that job is irrelevant; every selected job must pass. `workflow_dispatch` requests the full validation set. CI uses read-only repository permissions and never commits/pushes generated artifacts.

## Known platform limits

- Windows/macOS/Linux local scripts support Git worktree operations. PowerShell 7 (`pwsh`) is needed to use the shared PowerShell lock/scope/sync/validation tools on macOS/Linux.
- Wasm is not a required CI job while the target remains non-release-ready; use `scripts/validate-changes.ps1 -Profile Wasm` for an explicit diagnostic. iOS compilation requires macOS/Xcode and is not run by the Linux GitHub Actions matrix.
- Heavy Gradle tasks can contend for CPU/RAM when many agents build simultaneously. Coordinate validation concurrency; prefer one Gradle invocation per task.
- GitHub CI does not share local filesystem locks. PR-level conflict review and required checks remain necessary for work from different machines.
