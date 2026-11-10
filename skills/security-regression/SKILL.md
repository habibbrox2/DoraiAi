---
name: security-regression
description: Review DORAI AI code changes for regressions in MCP SSRF protection, shell hardening, network policy, encrypted storage and release integrity.
---

# Security Regression Review

1. Read `SECURITY.md`, root `AGENTS.md`, and the relevant implementation/tests before judging a change.
2. Trace untrusted inputs end-to-end to network, shell/process, filesystem or persistence sinks.
3. Preserve deny-by-default behavior, MCP redirect/connection validation, header sanitization and shell allowlisting.
4. Preserve Android HTTPS-first policy, documented local cleartext exceptions, backup exclusions and encrypted settings.
5. Check logs and diffs for secrets, credentials, conversation content or accidental sensitive data.
6. Do not bypass validation or weaken tests to resolve compilation/build failures.
7. Report threat scenarios and tests run; mark untested assumptions clearly. Do not edit security code without explicit authorization.
