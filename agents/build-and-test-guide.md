# Build and Test Guide

## Before building

1. Run `git status --short --branch` and inspect diffs. Preserve existing user modifications.
2. Verify Java with `java -version`. Existing GitHub workflows use Temurin JDK 21; Java 8 is insufficient for the current toolchain.
3. Use the checked-in Gradle wrapper. Do not upgrade Gradle, Kotlin, AGP, or dependencies just to work around a source error.
4. Discover tasks first: Windows `.\gradlew.bat tasks --all`; Unix `./gradlew tasks --all`. Confirm exact task names before running.

## Suggested validation ladder

Run from repository root (use `./gradlew` on Unix/macOS, `.\gradlew.bat` on Windows); confirm task availability before running:

1. Formatting/lint check: `.\gradlew.bat spotlessCheck` (if the task is available).
2. Wasm compiler diagnostics: `.\gradlew.bat :composeApp:compileKotlinWasmJs` (confirm exact task name with `tasks --all`).
3. Browser distribution: `.\gradlew.bat :composeApp:wasmJsBrowserDistribution` (target exists; not release-ready).
4. Shared/Desktop tests: `.\gradlew.bat :composeApp:test` and/or `.\gradlew.bat :composeApp:desktopTest` as relevant.
5. Android debug build: `.\gradlew.bat :androidApp:assembleFossDebug` (default flavor) or `.\gradlew.bat :androidApp:assemblePlayStoreDebug`; verify flavor/task names in this checkout.
6. Run targeted screenshot tests only when relevant and when baseline screenshots are protected.

## Existing CI behavior

- `.github/workflows/test.yml` runs on pushes and pull requests to `main`.
- It runs `spotlessApply`, screenshot updates, WebP conversion and desktop tests. If generated changes exist, it can commit and push them for non-fork runs.
- `.github/workflows/apk.yml` supports `workflow_dispatch` and `workflow_call`, uses Java 21 and builds `:androidApp:assembleFossRelease`.
- CI behavior is not automatically read-only. Do not trigger workflows or push changes without the user's explicit authorization.

## Error triage

- Fix the first meaningful compiler error first; later errors may be cascades.
- Separate Kotlin syntax/brace errors from unresolved imports, wrong source set, API signature changes, missing resources, dependency resolution and JDK/Gradle incompatibility.
- Save the complete command, exit code and relevant error context. Never report success based on a task starting or partial output.
- Avoid `clean` until incremental build/cache issues are demonstrated; it can cost time and erase useful diagnostic state.
