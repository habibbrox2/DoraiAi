# Tiered Validation Policy

The objective is fast feedback without weakening the merge gate. Select tests from changed files and their dependency/platform impact; do not equate a skipped test with a pass.

| Profile | When | Expected checks |
|---|---|---|
| `docs` / L0 | Markdown/docs-only | `git diff --check`; documentation/link checks if configured; no Gradle invocation |
| `fast` / L1 | Small isolated Kotlin/test change | Formatting check plus the narrowest affected test/compile task |
| `feature` / L2 | Feature/UI/resource change | Affected module compile/tests; screenshot verification for visual changes |
| `full` / L4 | Gradle/dependency/schema/shared-contract/security/merge | Full required CI matrix and relevant supported-platform checks |
| `wasm` | Explicit Wasm-only task or investigation | `:composeApp:compileKotlinWasmJs` (opt-in diagnostic; target is not release-ready) |

## Change classification

- Documentation-only: no Gradle build.
- `composeApp` shared Kotlin: desktop/shared tests and Android compile/unit tests. Wasm is opt-in with `-Profile Wasm` because the target is not release-ready and currently has baseline compiler errors; iOS validation requires macOS/Xcode.
- Android-only: matching Android compile and unit tests.
- Desktop-only: desktop tests.
- UI/theme/global resource: affected compile/tests plus Paparazzi verification.
- Gradle/plugin/version catalog/wrapper, SQLDelight schema, `expect`/`actual`, security, CI, or cross-platform contracts: broaden to full validation.
- Website/frontend: run the corresponding pnpm/site checks, not Gradle unless shared build configuration also changed.

`scripts/validate-changes.ps1` is a conservative local selector. It prints the selected profile/tasks and supports `-WhatIf`/`-NoGradle`. It must never downgrade explicit `-Profile Full` or a high-risk change. Use `-Profile Wasm` only for an explicit Wasm diagnostic.

## CI contract

GitHub Actions may skip irrelevant matrix jobs for docs-only or isolated changes, but the stable `Run Unit Tests` aggregate check must always run and accept only `success` or intentionally `skipped` prerequisite jobs. Any job selected by the change classifier must succeed. Before merge, required validation must be green; a local targeted pass is not a substitute for the merge gate.

## Build performance

- Preserve existing Gradle incremental compilation, parallel execution, and build cache.
- Do not enable Configuration Cache globally until all relevant plugins/tasks are proven compatible in CI.
- Prefer one Gradle invocation with multiple selected tasks to reuse daemon/configuration work.
- Do not run multiple heavy Gradle builds concurrently on a constrained workstation; use a configurable worker/concurrency limit.
- Never run `clean` routinely; never disable tests to make CI green.
