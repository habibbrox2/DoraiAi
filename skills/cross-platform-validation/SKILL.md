---
name: cross-platform-validation
description: Build a target-aware validation matrix for DORAI AI across Android, desktop, iOS and browser Wasm.
---

# Cross-Platform Validation

1. Read the configured targets in `composeApp/build.gradle.kts` and inspect the changed files/source sets.
2. Discover real Gradle task names with the wrapper; do not assume a task exists.
3. Verify JDK 21 and required SDK/toolchain availability before interpreting failures.
4. Start with the smallest relevant compile/test, then validate all targets affected by shared code.
5. For commonMain changes, validate desktop/shared tests and Android compile/tests. Wasm is opt-in with `scripts/validate-changes.ps1 -Profile Wasm` because the target is not release-ready; iOS requires macOS/Xcode. For expect/actual changes, inspect every actual implementation and validate supported targets.
6. Run desktop tests and relevant Android build/test tasks when applicable; iOS validation may require macOS/Xcode.
7. Do not trigger CI workflows with write-back behavior or commit/push artifacts without approval.
8. Report a matrix of target, command, result, and blocker; distinguish not run from failed.
