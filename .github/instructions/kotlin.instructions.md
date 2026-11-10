---
applyTo: "**/*.kt,**/*.kts"
---

Follow `AGENTS.md`. Respect Kotlin Multiplatform source-set boundaries; keep `commonMain` free of platform APIs and update all active `actual` implementations when changing an `expect` declaration. Check existing APIs and version catalog before editing. Do not add dependencies or alter Gradle versions without approval. Add focused regression tests and select validation using `agents/validation-policy.md`.
