# ADR-001: Kotlin-first hybrid language strategy

- **Status:** Accepted for foundation
- **Date:** 2026-11-03
- **Decision owners:** DORAI AI maintainers

## Context

The application is already built on Kotlin Multiplatform and Compose Multiplatform, with Android, iOS, Desktop, and Kotlin/Wasm targets. Future web and integration work may benefit from TypeScript, but replacing the existing UI/runtime or coupling the Gradle build to Node would add migration and operational risk.

## Decision

1. Keep Kotlin as the primary application, agent, provider, tool, storage, and privileged capability platform.
2. Add an independent pnpm-managed TypeScript workspace for future web-facing modules and SDKs.
3. Define cross-language communication using versioned, language-neutral JSON Schema.
4. Keep the current Kotlin/Wasm target independent from the TypeScript workspace.
5. Do not add a runtime bridge or migrate existing features in this foundation phase.
6. Keep TypeScript tooling outside Gradle task dependencies.

## Consequences

- Kotlin and TypeScript can evolve independently.
- Shared contracts become explicit and reviewable.
- A future bridge requires a separate transport/security design and implementation.
- Initial TypeScript application sources remain empty by design.
- Additional frameworks and runtime dependencies require feature-level approval.

## Rejected alternatives

- Replace Compose with React: rejected because it would be a broad UI migration and duplicate existing platform work.
- Execute arbitrary TypeScript inside the agent process: rejected due to security and lifecycle risks.
- Reuse Kotlin/Wasm as if it were TypeScript: rejected because the language/toolchain and module ownership differ.
