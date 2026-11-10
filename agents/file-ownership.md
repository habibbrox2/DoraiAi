# File Ownership and Shared Hotspots

## Default ownership
- Each task owns one dedicated worktree and branch.
- Each file has at most one active task owner. The reservation registry is stored in the repository's shared Git directory, so all linked worktrees on the same machine see the same locks.
- Use `scripts/agent-lock.ps1 -Action Acquire -TaskId <id> -Agent <agent> -Paths <path...>` before editing, and `-Action Release` after handoff.
- Locks do not expire automatically. Inspect with `-Action List`; release only a lock whose task is complete or explicitly abandoned.

## High-contention paths
Require explicit task-owner approval and a single designated owner for:
- `settings.gradle.kts`, root/module `build.gradle.kts`, `gradle/**`, `gradle.properties`, wrapper files, version catalogs.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/AppModule.kt`, navigation, shared DI, provider/model contracts.
- `composeApp/src/commonMain/sqldelight/**` and migration files.
- Shared Compose resources and global theme/design tokens.
- `.github/workflows/**`, agent instructions, security policy, shell/process execution, credentials, network policy, release/signing configuration.

## Scope checks
The lock registry detects overlapping path prefixes (including `/**` globs). `scripts/verify-agent-scope.ps1` checks changed paths against a task contract. These checks reduce accidental overlap but do not replace review or OS-level permissions.
