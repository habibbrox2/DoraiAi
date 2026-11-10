---
trigger: always_on
description: DORAI AI repository-wide engineering and safety policy
---

Follow `AGENTS.md` as the canonical repository-wide policy. Read relevant `agents/` guides and `skills/*/SKILL.md`. For parallel work, use one isolated Git worktree per task and reserve paths with `scripts/agent-lock.ps1`. Respect `agents/task-protocol.md`, `agents/file-ownership.md`, and `agents/validation-policy.md`. Do not edit outside allowed paths, overwrite other work, weaken security, change dependencies without approval, or run destructive Git commands. Run scope verification and targeted validation before handoff. Commits/pushes/PRs/releases/merges require explicit authorization.
