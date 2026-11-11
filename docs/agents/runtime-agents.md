# App Runtime Agent — Instructions Map

**Last verified:** 2026-11-12

The **App Runtime Agent** (Dorai AGENT) is the AI assistant that runs *inside the app* —
chat, heartbeat, scheduled tasks, memories, tools, dynamic/interactive UI, skills.
This document is the central map of everything that instructs it, where the instructions
live, and how to change them.

> The Project Agent (coding agents working on this repo) has a separate map:
> [project-agents.md](project-agents.md). Do not add project-agent content here.

## Source map

The runtime prompt is **code**, composed by pure builders. There is no single "system
prompt file" — every section is composed per request from settings, history and tools.

| Section / component | Source | Notes |
|---|---|---|
| Soul (identity) | `AppSettings.getSoulText()` — key `KEY_SOUL` (`soul_text`); default `Res.string.default_soul` (localized) | User-customizable in Settings → Agent → Soul. Empty means the localized default. |
| Honesty rule | `DEFAULT_HONESTY_RULE` in `ChatSystemPromptBuilder.kt` | Always rendered; one sentence, no `##` header. |
| `## Tool Use` | `DEFAULT_TOOL_USE_SECTION` | Rendered only when tools are available (`hasTools`). |
| `## When to Act` | `DEFAULT_ACTING_SECTION` | Always rendered. |
| `## Memory Instructions` | `memoryInstructions` setting (AppSettings) | Non-empty only when the user set instructions. |
| `## Structured Learning` | `DEFAULT_STRUCTURED_LEARNING_SECTION` | `CHAT_REMOTE` only, when memory is enabled. |
| `## Your Memories` / `## User Preferences` / `## Learnings` / `## Known Issues & Resolutions` | Memory store (`MemoryStore`) via `getActiveSystemPrompt` | Local variant capped at `LOCAL_MEMORY_BUDGET_CHARS` (2_000 chars), entries truncated at entry boundaries. |
| `## Automation` | `DEFAULT_AUTOMATION_SECTION` | `CHAT_REMOTE` only, when scheduling is enabled. |
| `## Email Accounts` (+ Sending policy) | `EmailAccountSummary` list — connected email accounts | Remote-only. Send policy: present full draft and confirm before `compose_email` / `reply_email`. |
| `## Scheduled Tasks` | `pendingTasks` | Remote-only. `execute_at` / `cron` tasks. |
| `## Heartbeat Additions` | `heartbeatAdditions` | Remote-only. Standing `schedule_task(on_heartbeat = true)` instructions. |
| `## Active skill: <name>` | `SkillManifest.body` + bundled files | Only when the user prefixed the turn with `/<skill-id>`. See [`docs/features/skills.md`](../features/skills.md). |
| `## Context` | `ChatPromptRuntimeContext` | Local time with offset + IANA zone, UTC, platform, model, provider. |
| `## Dynamic UI` | `appendDynamicUiSection()` | Remote-only; Dynamic UI setting enabled + model supports tools. |
| `## Interactive UI Mode` | `appendInteractiveUiSection()` | Remote-only; interactive mode flag takes precedence over Dynamic UI. |
| dorai-ui component catalog | `DORAI_UI_COMPONENT_CATALOG` in `ChatSystemPromptBuilder.kt` | Shared by Dynamic UI and Interactive UI sections. |
| Heartbeat prompt | `HeartbeatPromptBuilder.kt` (`buildHeartbeatPrompt`) | Sent as a **user** message, not a system prompt. |
| Splinterlands LLM picker | `buildLlmPrompt` in `SplinterlandsTeamPicker.kt` | Fully isolated; does not use the chat builder. |

## Variants

`SystemPromptVariant` controls composition. Every `if (variant == …)` block in
`buildChatSystemPrompt` is the single source of truth for where a section belongs.

- **`CHAT_REMOTE`** — full prompt for hosted services. No budgets.
- **`CHAT_LOCAL`** — trimmed prompt for on-device LiteRT. Drops Structured Learning,
  Automation, Email Accounts, Scheduled Tasks, Heartbeat Additions, dorai-ui; memory
  sections are char-capped. Small Gemma models cannot coherently attend to the full prompt.

See [`docs/features/system-prompts.md`](../features/system-prompts.md) for the
authoritative section-by-section reference and the Local budget semantics.

## Update procedure

1. Edit the builder (`ChatSystemPromptBuilder.kt` / `HeartbeatPromptBuilder.kt` /
   `SplinterlandsTeamPicker.kt`) — or its inputs (soul text resource, memory
   instructions, skill manifests).
2. Run the prompt tests:
   `./gradlew :composeApp:desktopTest --tests "*SystemPromptBuilder*"`
   (includes `ChatSystemPromptBuilderTest` and `HeartbeatPromptBuilderTest`)
   and, for Splinterlands, `--tests "*SplinterlandsTeamPickerPromptTest*"`.
3. Update [`docs/features/system-prompts.md`](../features/system-prompts.md) (sections,
   budgets, gating) and its "Last verified" date.
4. For user-visible wording in default soul/skills, keep Bengali parity per
   `docs/development/bengali-language-and-rendering-policy.md` and re-verify
   `ChatSystemPromptBuilderTest`.
5. Do not mirror runtime-agent text into `AGENTS.md`/`CLAUDE.md`; keep the two families
   separate (see [README.md](README.md)).

## Prompt-behavior invariants

- Soul customization must not drop `DEFAULT_HONESTY_RULE`, `DEFAULT_ACTING_SECTION`, or
  `DEFAULT_TOOL_USE_SECTION` (when tools exist).
- Sections gated on absence of a capability render **zero bytes** for that capability.
- Local (`CHAT_LOCAL`) memory truncation happens at entry boundaries, never mid-entry.
- Interactive UI Mode is `CHAT_REMOTE`-only; on-device prompts never contain `dorai-ui`.

## See also

- [README.md](README.md) — the two-family separation
- [project-agents.md](project-agents.md) — the Project Agent (coding agents)
- [`docs/features/system-prompts.md`](../features/system-prompts.md) — section-by-section reference
- [`docs/features/skills.md`](../features/skills.md) — skills
- [`docs/features/memories.md`](../features/memories.md) — memories
- [`docs/features/dynamic-ui.md`](../features/dynamic-ui.md) — dorai-ui / dynamic UI
- [`docs/features/heartbeat.md`](../features/heartbeat.md) — heartbeat
