# TypeScript Platform Architecture

## Role

The TypeScript workspace is a separate platform foundation for future browser-facing modules, extension integrations, plugin SDKs, and web tooling. It does not replace Compose or existing Kotlin features.

## Current toolchain

- pnpm workspace
- TypeScript strict compiler configuration
- Vite build/dev foundation
- Vitest test foundation
- Prettier formatting

No UI framework, production bundler entry point, runtime bridge, or plugin API is selected in this phase.

## Source organization

Future code should be introduced as explicit packages under the workspace. Each package must declare its runtime target, public API, dependencies, test strategy, and security boundary.

Do not place secrets or privileged operations in browser code. Avoid dynamic code evaluation and unsafe HTML injection. Validate all data crossing a trust boundary.

## Dependency policy

Add production dependencies only when an approved feature requires them. Keep build/test tools in development dependencies. Pin the package manager and commit the lockfile. Review licenses, maintenance, supply-chain posture, and bundle/runtime impact before adding dependencies.
