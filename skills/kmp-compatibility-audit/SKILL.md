---
name: kmp-compatibility-audit
description: Audit Kotlin Multiplatform source-set errors and platform API leakage in DORAI AI before proposing a minimal repair.
---

# KMP Compatibility Audit

1. Read root `AGENTS.md`, `CLAUDE.md`, the relevant feature spec, `settings.gradle.kts`, `composeApp/build.gradle.kts`, and the version catalog.
2. Inspect `git status` and the complete diff before touching any file; preserve all existing modifications.
3. Identify the failing target and source set. Confirm whether code is in `commonMain`, `jvmShared`, Android, desktop, iOS or Wasm.
4. For platform contracts, compare `Platform.kt` expect declarations with every platform actual file.
5. Flag JVM/Android-only APIs in shared code; recommend an existing multiplatform abstraction or a narrowly scoped expect/actual change.
6. Check dependency target support and resolved API versions; do not guess from current online docs alone.
7. Return evidence, root cause, affected files, minimal plan and validation commands. Do not edit code unless explicitly authorized.
8. Never claim a target works until its compile/build task completes successfully.
