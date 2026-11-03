# Hybrid Integration Foundation

This directory defines the future Kotlin/TypeScript integration boundary.

- `contracts/` contains language-neutral, versioned JSON protocol schemas.
- Future Kotlin and TypeScript adapters must consume the same contract.
- No bridge, runtime host, plugin loader, or cross-process transport is implemented in this phase.

Kotlin remains the authority for agent execution, provider access, filesystem, shell, credentials, permissions, and other privileged capabilities. A TypeScript consumer may request a capability only through an explicitly exposed, validated, permission-checked Kotlin API.
