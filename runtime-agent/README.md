# DORAI AI — Runtime Agent Documentation

**Last verified:** 2026-11-12

This directory is the single home for everything the **App Runtime Agent** (the AI assistant that runs inside the app) is instructed by — the system prompts the model sees, the skills it can load, the memories it stores, the heartbeat/autonomous loop, the dynamic/interactive UI, and the documentation that maps them.

> The **Project Agent** (coding agents and CI working on this repository) has a separate documentation tree under [`docs/agents/`](../docs/agents/README.md); it is never composed into the app prompt and must not be mixed with these files.

## Layout

```
runtime-agent/
├── README.md            ← this index
├── runtime-agents.md    ← App Runtime Agent: source map, composition, update procedure
└── features/            ← App Runtime Agent feature docs
    ├── system-prompts.md  — prompt variants, section map, Local memory budget
    ├── skills.md            — installable SKILL.md instruction bundles
    ├── memories.md          — memory lifecycle, categories, reinforcement, promotion
    ├── heartbeat.md         — autonomous self-checks, active hours, scheduled tasks
    └── dynamic-ui.md        — dorai-ui: AI-generated interactive screens inline in chat
```

## Source of truth

Behavioral documentation in `features/` describes **what the app does**; the instruction source files (`ChatSystemPromptBuilder.kt`, `HeartbeatPromptBuilder.kt`, `SplinterlandsTeamPicker.kt`) and the tests (`ChatSystemPromptBuilderTest`, `HeartbeatPromptBuilderTest`) describe and verify **what the model is told**. Keep them in sync: every prompt change must update `features/system-prompts.md` and its tests.

## See also

- [`runtime-agents.md`](runtime-agents.md) — full source map and update procedure
- [`docs/agents/README.md`](../docs/agents/README.md) — the two-agent separation (Project vs Runtime)
- [`docs/features/`](../docs/features/) — remaining app feature docs
