# TypeScript Workspace Setup

## Requirements

- Node.js 22.15 or newer, below 27
- pnpm 12.x

The root package manifest pins pnpm to 12.11.2. Use the pinned package manager version to avoid lockfile drift.

## Install

From repository root:

```bash
corepack enable
corepack prepare pnpm@12.11.2 --activate
pnpm install --frozen-lockfile
```

If Corepack is unavailable or disabled by the machine policy, install the exact pnpm version using the organization's approved package-management method.

## Checks

```bash
pnpm frontend:typecheck
pnpm frontend:test
pnpm frontend:format:check
pnpm frontend:check
```

The current type-check validates the TypeScript Vite/Vitest configuration only. No feature source or test file is intentionally included yet.

## Isolation guarantees

- Do not add this workspace to Gradle settings.
- Do not invoke pnpm from Android/Desktop build tasks.
- Do not add Node.js as an Android runtime dependency.
- Existing Kotlin/Wasm builds continue to use the existing Kotlin Gradle plugin/toolchain.
- CI integration should be introduced separately after the initial local workspace is validated.

## Adding the first feature

Before adding a feature, define its runtime target, package boundary, contract, dependencies, threat model, and test plan. Add TypeScript source only within the approved package. Keep Kotlin as the authority for privileged capabilities.
