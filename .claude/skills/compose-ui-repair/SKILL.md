---
name: compose-ui-repair
description: Diagnose Compose Multiplatform syntax, import, state, focus, accessibility and API-signature errors using this repository's resolved versions.
---

# Compose UI Repair

1. Read `AGENTS.md`, the full affected composable, imports, call sites, current diff and related feature spec.
2. Classify the first error: syntax/nesting, unresolved symbol, wrong source set, signature/opt-in mismatch, resource generation, or target limitation.
3. Search this repository for a working usage of the same API and inspect the exact dependency versions.
4. Preserve state flow, dismissal, keyboard/focus behavior, accessibility semantics, empty states and visual intent.
5. Do not remove UI or accessibility behavior simply to compile. Do not upgrade Compose or add dependencies without approval.
6. After approval, patch the smallest coherent unit; compile the affected target and test the interaction.
7. Report what changed and which checks passed or remain unverified.
