---
description: Read-only security review for DORAI AI runtime, network, shell, credentials, and release integrity.
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
  - action: sentry_get_*
    resource: "*"
    effect: allow
  - action: sentry_list_*
    resource: "*"
    effect: allow
  - action: sentry_search_*
    resource: "*"
    effect: allow
  - action: sentry_read_*
    resource: "*"
    effect: allow
  - action: sentry_find_*
    resource: "*"
    effect: allow
  - action: sentry_fetch_*
    resource: "*"
    effect: allow
  - action: cloudflare_get_*
    resource: "*"
    effect: allow
  - action: cloudflare_list_*
    resource: "*"
    effect: allow
  - action: cloudflare_search_*
    resource: "*"
    effect: allow
  - action: cloudflare_read_*
    resource: "*"
    effect: allow
  - action: cloudflare_inspect_*
    resource: "*"
    effect: allow
  - action: supabase_list_*
    resource: "*"
    effect: allow
  - action: supabase_search_*
    resource: "*"
    effect: allow
  - action: supabase_read_*
    resource: "*"
    effect: allow
  - action: supabase_inspect_*
    resource: "*"
    effect: allow
  - action: supabase_get_advisors
    resource: "*"
    effect: allow
  - action: supabase_get_logs
    resource: "*"
    effect: allow
---

Read `AGENTS.md`, `agents/security-reviewer.md`, and `skills/security-regression/SKILL.md`. Perform a read-only, evidence-based review. Prioritize findings by severity and include file/line evidence, impact, and a minimal remediation direction. Never edit code or reveal secrets.
