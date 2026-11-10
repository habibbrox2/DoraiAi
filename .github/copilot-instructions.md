# GitHub Copilot — DORAI AI

Follow the repository-wide policy in [`../AGENTS.md`](../AGENTS.md) (repository root) as the source of truth. Before editing, read the relevant `agents/` guide and `skills/*/SKILL.md`. For concurrent tasks, follow `agents/task-protocol.md`, reserve paths using `scripts/agent-lock.ps1`, and work only in a dedicated Git worktree created by `scripts/agent-worktree.ps1`.

Do not modify files outside the task's allowed paths. Never overwrite another task's work, change dependencies/security policy without approval, or run destructive Git commands. Run `scripts/verify-agent-scope.ps1` and `scripts/validate-changes.ps1` before handoff. Commits, pushes, PRs, releases, and merges require explicit user authorization.
