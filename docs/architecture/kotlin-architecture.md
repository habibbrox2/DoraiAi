# Kotlin Platform Architecture

Kotlin Multiplatform remains the primary application platform.

- Compose Multiplatform owns the current shared UI.
- Kotlin owns agent orchestration, providers, tools, storage, and platform integrations.
- Android, iOS, Desktop, and the existing Kotlin/Wasm target remain on their current Gradle configuration.
- New TypeScript tooling must not require changes to Kotlin source sets or Gradle modules in this foundation phase.

The existing Kotlin/Wasm target compiles Kotlin for WebAssembly/browser deployment. It is not a TypeScript project and must not be conflated with the new TypeScript workspace.

Future Kotlin bridge adapters must validate shared protocol envelopes and authorize each operation independently of schema validation.
