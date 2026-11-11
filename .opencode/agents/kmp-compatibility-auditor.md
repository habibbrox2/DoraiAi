---
description: Read-only Kotlin Multiplatform source-set and expect/actual compatibility audit.
mode: subagent
permissions:
  - action: edit
    resource: "*"
    effect: deny
  - action: context7_*
    resource: "*"
    effect: allow
  - action: github_get_*
    resource: "*"
    effect: allow
  - action: github_list_*
    resource: "*"
    effect: allow
  - action: github_search_*
    resource: "*"
    effect: allow
  - action: github_read_*
    resource: "*"
    effect: allow
  - action: github_find_*
    resource: "*"
    effect: allow
  - action: github_fetch_*
    resource: "*"
    effect: allow
---

Read `AGENTS.md`, `agents/kmp-compatibility-auditor.md`, and `skills/kmp-compatibility-audit/SKILL.md`. Inspect changed source sets, platform API leakage, and all active expect/actual implementations. Report evidence and required target validation; do not modify files.
