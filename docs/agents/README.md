# Agent Instructions — Central Index

**Last verified:** 2026-11-12

DORAI AI has two distinct agent families. They are documented and maintained separately, in two separate paths, and must not be mixed.

| | **Project Agent** | **App Runtime Agent** |
|---|---|---|
| **What it is** | Coding agents and CI bots that change this repository | The in-app AI assistant (Dorai AGENT) that runs inside the app |
| **Who it serves** | Developers, maintainers, CI (GitHub Actions) | App users (chat, heartbeat, scheduled tasks, Splinterlands) |
| **Where instructions live** | [project-agents.md](project-agents.md) maps [`AGENTS.md`](../../AGENTS.md), [`CLAUDE.md`](../../CLAUDE.md), [`agents/`](../../agents/), [`.agents/`](../../.agents/) | [runtime-agents.md](runtime-agents.md) maps the in-app instruction sources (prompt builders, soul, skills) |
| **Governed by** | Branch/PR policy, validation policy, file ownership | System-prompt invariants, prompt unit tests |
| **Entry doc** | [project-agents.md](project-agents.md) | [runtime-agents.md](runtime-agents.md) |

## Layout

```
docs/agents/
├── README.md            ← this index
├── project-agents.md    ← Project Agent: paths, rules, ownership, verification
└── runtime-agents.md    ← App Runtime Agent: source map, composition, update procedure
```

## Separation rules

1. **Project-Agent rules never ship in the app prompt.** Nothing from `AGENTS.md`,
   `CLAUDE.md`, `agents/`, or `.agents/` is composed into the runtime system prompt
   (`ChatSystemPromptBuilder` / `HeartbeatPromptBuilder`).
2. **Runtime-Agent instructions never live in root files.** Prompt sections, soul defaults,
   tool-use policy, dorai-ui catalog and skill instructions are owned by app source and
   verified by prompt unit tests — not by editing `AGENTS.md`.
3. **Behaviour changes go through tests, not through docs.** Editing `ChatSystemPromptBuilder.kt`
   requires `ChatSystemPromptBuilderTest`; editing `HeartbeatPromptBuilder.kt` requires
   `HeartbeatPromptBuilderTest`. Editing this directory does not require a rebuild.
4. **Root files stay thin.** `AGENTS.md` carries repository-wide policy; tooling-specific
   entries live in `agents/` / `.agents/`. New agent-facing guidance goes into an existing
   guide file, or a new one under `agents/`, never scattered across feature docs.

## Maintenance

When a path, owner, rule, or verification command in this directory changes, update the
matching file and its "Last verified" date. Feature behavior documentation stays in
[`docs/features/`](../features/) (e.g. `system-prompts.md`, `skills.md`); this directory
documents **who instructs what, and where**.

## See also

- [Multi-Agent Development in `AGENTS.md`](../../AGENTS.md#multi-agent-development) — worktree/lock coordination
- [`docs/architecture/`](../architecture/) — system architecture
- [`docs/features/system-prompts.md`](../features/system-prompts.md) — prompt section reference
