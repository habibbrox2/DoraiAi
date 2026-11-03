# TypeScript Workspace

This workspace is an isolated foundation for future TypeScript-based web, extension, plugin-SDK, and integration features.

It intentionally contains configuration only. No product UI, runtime, provider, agent, or business-logic implementation is introduced in this phase.

## Requirements

- Node.js >= 22.15 and < 27
- pnpm >= 12 and < 13

The repository pins pnpm through the root `package.json` `packageManager` field. Use Corepack where available or install the pinned pnpm release using your approved machine setup.

## Commands

Run from the repository root:

```bash
pnpm install --frozen-lockfile
pnpm frontend:typecheck
pnpm frontend:test
pnpm frontend:format:check
pnpm frontend:check
```

The initial type-check includes only the Vite/Vitest configuration. There are deliberately no application TypeScript source files or tests yet; Vitest is configured to pass when no tests exist.

## Tooling

- TypeScript: strict compiler configuration
- Vite: future web build/dev foundation
- Vitest: future unit-test foundation
- Prettier: formatting for workspace configuration and documentation

ESLint and a UI framework are intentionally deferred until the first actual TypeScript feature is approved. This avoids unused policy/configuration and avoids introducing JavaScript source files prematurely.

## Isolation

This workspace is not included in Gradle settings, Gradle tasks, Android packaging, desktop packaging, or existing Kotlin/Wasm compilation. A failed or missing Node toolchain must not block Kotlin builds.

See [Hybrid Architecture](../docs/architecture/hybrid-architecture.md) and [TypeScript Setup](../docs/development/typescript-setup.md).
