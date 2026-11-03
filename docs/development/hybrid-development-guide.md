# Hybrid Development Guide

## Development model

Work in the language that owns the capability:

- Kotlin for application lifecycle, Compose UI, agent orchestration, providers, tools, storage, platform integration, and privileged execution.
- TypeScript for approved browser-facing UI/integrations, SDKs, and web tooling.

## Before implementing a cross-language feature

1. Define the use case and owning side.
2. Specify request, response, and event contracts.
3. Add JSON Schema and shared positive/negative fixtures.
4. Define transport, lifecycle, timeout, cancellation, and backpressure.
5. Define authorization, user confirmation, and audit requirements.
6. Add contract and security tests on both sides.
7. Verify independent Kotlin and TypeScript builds.

## Local checks

Use Gradle for Kotlin and pnpm for TypeScript. Neither build should require the other unless a future, reviewed feature explicitly introduces a generated contract artifact.

## Current limitation

The repository currently has schemas and workspace configuration only. There is no bridge, TypeScript runtime feature, plugin SDK, or cross-platform IPC implementation yet.
