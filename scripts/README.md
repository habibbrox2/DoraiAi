# Local Multi-Agent Tooling

All scripts are safe-by-default. They do not commit, push, merge, release, delete dirty worktrees, or overwrite existing paths.

## Windows (PowerShell)

From any checkout/worktree:

```powershell
# Create a dedicated worktree from the latest origin/main
.\scripts\agent-worktree.ps1 -Action Create -Agent copilot -Task settings-ui `
  -AllowedPaths 'composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/**' `
  -ReservedPaths 'gradle/**','build.gradle.kts' -ValidationProfile Feature

# List and inspect
.\scripts\agent-worktree.ps1 -Action List
.\scripts\agent-worktree.ps1 -Action Status -Path 'G:\path\to\worktree'

# Reserve paths before an agent starts editing
.\scripts\agent-lock.ps1 -Action Acquire -TaskId DORAI-201 -Agent copilot `
  -Paths 'composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/**'

# Check task scope and select relevant tests
.\scripts\verify-agent-scope.ps1
.\scripts\validate-changes.ps1 -Profile Auto -Base origin/main -WhatIf
.\scripts\validate-changes.ps1 -Profile Auto -Base origin/main

# Release the reservation after handoff
.\scripts\agent-lock.ps1 -Action Release -TaskId DORAI-201 -Agent copilot

# Synchronize generated Claude agent/skill copies
.\scripts\sync-agent-configs.ps1
.\scripts\sync-agent-configs.ps1 -Check
```

The `.agent-task.json` file is local metadata and is ignored by Git. Edit its `allowed_paths` if the task is created without `-AllowedPaths`.

## macOS/Linux

Use `bash scripts/agent-worktree.sh create <agent> <task> [base]` or `list/status/remove`. PowerShell 7 (`pwsh`) can run the shared lock, scope, sync, and validation scripts on macOS/Linux.

## Safety

- One worktree and branch per concurrently running task.
- Acquire non-overlapping path reservations before editing. Locks are shared by linked worktrees in the same local Git common directory.
- Locks do not auto-expire; inspect and release stale locks only after confirming the owner task is inactive.
- Removal is refused for dirty or unmerged worktrees. An ancestry check handles merge commits; squash merges are accepted only when the worktree tree exactly matches a commit already reachable from the selected base. Branches with conflict-resolution differences or unique content remain protected.
- These tools coordinate cooperative agents; they are not a security sandbox. Use container/OS permissions for untrusted code.
- Worktree creation fetches `origin/<base>` but never changes the current branch or working tree.

Frontend-only changes select the `Frontend` profile and run `pnpm frontend:check`; install the pinned workspace dependencies first with `pnpm install --frozen-lockfile`. Explicit `Full` and `Wasm` profiles run even when the worktree has no changed files; only `Auto` treats a clean tree as a no-op.

Wasm is not part of the required CI matrix while the target remains non-release-ready; use `scripts/validate-changes.ps1 -Profile Wasm` for an explicit diagnostic. A local targeted pass never replaces the PR's required checks.
