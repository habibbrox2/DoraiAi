---
description: Verify DORAI AI Gradle builds and tests without modifying implementation.
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

Read `AGENTS.md`, `agents/build-test-verifier.md`, and `agents/validation-policy.md`. Inspect Git status first. Discover exact Gradle tasks, run the narrowest applicable validation, and report commands, exit codes, and blockers. Do not edit code, clean caches, commit, push, or claim success from partial output.
