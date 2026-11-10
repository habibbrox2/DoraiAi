# Multi-Agent Task Protocol

This protocol applies to every human and coding agent working in DORAI AI, locally or through a hosted Git provider.

## Before editing
1. Read `AGENTS.md` and the relevant specialist guide/skill.
2. Record `git status --short --branch`, current commit, and active worktree. Never assume a clean tree.
3. Work in a dedicated Git worktree and task branch. Do not switch branches in a worktree owned by another task.
4. Create a task contract with a unique task ID, agent identity, objective, allowed paths, reserved paths, and validation profile.
5. Acquire path reservations with `scripts/agent-lock.ps1` before editing. If a reservation conflicts, stop and coordinate; do not edit around it.
6. Inspect existing code, tests, platform boundaries, and feature docs before changing anything.

## While editing
- Modify only the declared allowed paths. Ask before expanding scope.
- Do not edit reserved/shared files unless the task contract explicitly grants ownership.
- Do not add/upgrade dependencies, change Gradle/plugin versions, alter schemas, change public API contracts, or weaken security without explicit approval.
- Never modify another task's worktree, branch, lock, or uncommitted changes.
- Keep changes minimal and update relevant tests/docs.
- Do not run `clean`, delete caches, rewrite snapshots, or execute destructive commands as a troubleshooting shortcut.

## Before handing off
1. Run `scripts/verify-agent-scope.ps1` and inspect `git diff --check` and the full diff.
2. Run the narrowest relevant validation using `scripts/validate-changes.ps1`.
3. Report exact commands, pass/fail/not-run status, exit codes, and environmental blockers. Never claim a still-running command passed.
4. Commit only task-scoped files when the user or task owner explicitly authorizes commits. Never push, open PRs, merge, tag, release, or deploy unless explicitly authorized.
5. Release path reservations after work is complete. Keep the worktree if changes are unmerged or dirty.

## Task contract template

```yaml
task_id: DORAI-000
agent: <agent-id>
branch: agent/<agent-id>/<task-slug>
worktree: <absolute-path>
objective: <one-sentence outcome>
allowed_paths:
  - <path-or-glob>
reserved_paths:
  - gradle/**
  - build.gradle.kts
validation_profile: auto # docs | fast | feature | full
commit_authorized: false
push_authorized: false
```

## Conflict policy
- One active owner per file/path at a time. Prefer disjoint feature directories.
- Shared hotspots include root Gradle files, `gradle/**`, `AppModule.kt`, navigation/routing, common contracts, SQLDelight schema/migrations, shared resources, CI workflows, and security boundaries.
- A path reservation is advisory coordination, not an OS sandbox. Git worktrees isolate working trees; use OS/container sandboxing where hard filesystem isolation is required.
- If another agent changes the same file, stop and coordinate. Do not auto-resolve by choosing one side or overwriting either change.
