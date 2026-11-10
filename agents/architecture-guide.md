# Architecture Guide

## Confirmed repository structure
- `settings.gradle.kts` includes `:composeApp`, `:androidApp`, and `:screenshotTests`.
- `composeApp/build.gradle.kts` applies Kotlin Multiplatform, Android Multiplatform Library, Compose Multiplatform/compiler, Kotlin serialization, and SQLDelight plugins.
- Configured targets: Android, iOS arm64, iOS simulator arm64, JVM target named `desktop`, and `wasmJs` browser executable.
- The main shared application code is under `composeApp/src/commonMain/kotlin`; Compose Resources are under `composeApp/src/commonMain/composeResources`.
- Android, desktop, iOS and Wasm platform implementations are kept under their matching source sets. `src/jvmShared/kotlin` is added to both `androidMain` and `desktopMain` by Gradle.
- Tests include `commonTest` and `desktopTest`; screenshot tests have a separate `:screenshotTests` module.

## Dependency and toolchain policy
- Check exact versions in `gradle/libs.versions.toml`; do not copy API examples from another Compose/Kotlin version without checking compatibility.
- Current catalog versions include Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material 3 1.10.0-alpha05, Ktor 3.6.0, and SQLDelight 2.4.1. These values may change; always re-read the checked-out catalog.
- Existing CI uses JDK 21. Verify the active local JDK with `java -version` before interpreting Gradle results. Do not change machine configuration without approval.

## Product and feature documentation
- Product name shown to users: DORAI AI; navy and cyan are the current brand colors.
- Read root `CLAUDE.md` and the matching `docs/features/` spec before feature changes.
- `docs/knowledge/` contains maintained data/policy bundles. Follow each bundle's refresh playbook; never silently regenerate or bump curated snapshots.
- Keep SQLDelight schema/migration rules in mind: `composeApp/build.gradle.kts` enables `verifyMigrations`; schema changes need numbered `.sqm` migration files as specified in the SQL source headers.

## Do not infer
This guide does not assert that every platform currently builds or that all dependencies support every target. Confirm compatibility by compiling the exact target and inspecting resolved diagnostics.
