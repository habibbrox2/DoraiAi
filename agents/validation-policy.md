# Tiered Validation Policy

The objective is fast feedback without weakening the merge gate. Select tests from changed files and their dependency/platform impact; do not equate a skipped test with a pass.

| Profile | When | Expected checks |
|---|---|---|
| `docs` / L0 | Markdown/docs-only | `git diff --check`; documentation/link checks if configured; no Gradle invocation |
| `fast` / L1 | Small isolated Kotlin/test change | Formatting check plus the narrowest affected test/compile task |
| `feature` / L2 | Feature/UI/resource change | Affected module compile/tests; screenshot verification for visual changes |
| full / L4 | Gradle/dependency/schema/shared-contract/security/merge | Run Full Local Validation as feasible, then require the complete applicable GitHub Actions CI merge gate |
| `wasm` | Explicit Wasm-only task or investigation | `:composeApp:compileKotlinWasmJs` (opt-in diagnostic; target is not release-ready) |
| `frontend` | Frontend-only or hybrid contract change | `pnpm frontend:check` (typecheck, schema compatibility, unit tests, formatting) |

## Change classification

- Documentation-only: no Gradle build.
- `composeApp` shared Kotlin: desktop/shared tests and Android compile/unit tests. Wasm is opt-in with `-Profile Wasm` because the target is not release-ready and currently has baseline compiler errors; iOS validation requires macOS/Xcode.
- Android-only: matching Android compile and unit tests.
- Desktop-only: desktop tests.
- UI/theme/global resource: affected compile/tests plus Paparazzi verification.
- Gradle/plugin/version catalog/wrapper, SQLDelight schema, `expect`/`actual`, security, CI, or cross-platform contracts: broaden to full validation.
- Website/frontend: run `pnpm frontend:check`; frontend-only changes use the `Frontend` profile, while shared build/configuration changes retain the broader `Full` profile and also run frontend checks when applicable.
- Screenshot-only changes under `screenshotTests/`: select `Feature` and run `:screenshotTests:verifyPaparazziDebug`.

`scripts/validate-changes.ps1` is a conservative local selector. It prints the selected profile/tasks and supports `-WhatIf`/`-NoGradle`. A clean tree is a no-op only for `Auto`; explicit `Full` and `Wasm` profiles still run. It must never downgrade explicit `-Profile Full` or a high-risk change. Use `-Profile Wasm` only for an explicit Wasm diagnostic.

Full Local Validation is the finite set of local tasks selected by the script; it is not the complete CI matrix. Full CI Merge Gate means every applicable required GitHub Actions check has completed successfully (or is intentionally skipped by the workflow contract). Never report local Full as equivalent to Full CI.

## CI contract

GitHub Actions may skip irrelevant matrix jobs for docs-only or isolated changes, but the stable `Run Unit Tests` aggregate check must always run and accept only `success` or intentionally `skipped` prerequisite jobs. Any job selected by the change classifier must succeed. Before merge, required validation must be green; a local targeted pass is not a substitute for the merge gate.

## Build performance

- Preserve existing Gradle incremental compilation, parallel execution, and build cache.
- Do not enable Configuration Cache globally until all relevant plugins/tasks are proven compatible in CI.
- Prefer one Gradle invocation with multiple selected tasks to reuse daemon/configuration work.
- Serialize Gradle work on a constrained workstation: one agent owns the Gradle execution slot at a time. Other agents may perform read-only inspection or prepare commands, but must not launch a second Gradle build concurrently.
- Before a build, record available disk space on the Gradle cache/workspace volumes and available memory. If space or memory is insufficient, stop and report the blocker; do not purge caches to make room.
- Prefer CI-hosted validation when local resource headroom is inadequate. Do not change JVM heap, worker count, parallelism, or cache settings without measured evidence and explicit approval.
- Never run clean routinely; never disable tests to make CI green.
