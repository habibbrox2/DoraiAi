# DORAI AI System Architecture

**Status:** Foundation architecture
**Scope:** Kotlin-first hybrid platform

## Architectural direction

DORAI AI remains a Kotlin Multiplatform application with Compose Multiplatform as its native/shared UI foundation. TypeScript is introduced as an independent workspace for future web-facing, extension, SDK, and integration capabilities. It is not a replacement for the existing Kotlin application.

```tex
Android / Desktop / iOS / Web
            |
     Kotlin Multiplatform
  UI · Agent · Providers · Tools
            |
    Explicit capability boundary
            |
    Versioned JSON contracts
            |
     TypeScript workspace
 Web modules · SDK · Integrations
```

This diagram describes intended ownership and contracts, not a currently implemented runtime bridge.

## Ownership

| Area                                       | Primary owner      |
| ------------------------------------------ | ------------------ |
| Application lifecycle and native UI        | Kotlin / Compose   |
| Agent orchestration and provider adapters  | Kotlin             |
| Tool execution, shell, filesystem, sandbox | Kotlin             |
| Credentials and permission decisions       | Kotlin             |
| Existing Kotlin/Wasm target                | Kotlin toolchain   |
| Future browser-facing modules and SDKs     | TypeScript         |
| Cross-language message format              | Shared JSON Schema |

## Build isolation

- Gradle remains the authoritative Kotlin build.
- pnpm manages only the TypeScript workspace.
- Node.js is a development/build toolchain, not an Android runtime dependency.
- TypeScript build/test failure must not become a dependency of Android/Desktop Gradle tasks.
- Existing Kotlin/Wasm compilation remains independent and unchanged.

## Security boundary

TypeScript code is not trusted merely because it is bundled with the application. It must not receive unrestricted access to filesystem, shell, credentials, network secrets, or platform APIs. Any future bridge must expose narrow capabilities through Kotlin-owned validation, authorization, user approval, and audit logging.

## Non-goals of this foundation

- No migration of existing Kotlin features.
- No React or other UI framework selection.
- No plugin loader or arbitrary code execution.
- No WebView bridge or IPC implementation.
- No change to the current app runtime.
