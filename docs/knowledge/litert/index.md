---
okf_version: "0.2"
---

# On-device LiteRT model knowledge

OKF v0.2 bundle for DORAI AI’s **pinned on-device model downloads**. Agents use this folder for pin policy, digest provenance, and freshness when checking or bumping the runtime catalog.

## Runtime vs knowledge

| Layer | Path | Role |
|---|---|---|
| **Runtime** (shipped) | `composeApp/src/commonMain/kotlin/com/dorai/ai/inference/LocalModelCatalog.kt` | Source of truth for download URL, SHA-256, size, and context defaults |
| **Knowledge** (this bundle) | `docs/knowledge/litert/` | Pin policy, attested digest snapshot, sources, stale dates |
| **Refresh skill** | `.grok/skills/update-litert-models/SKILL.md` | Checks HuggingFace pins, updates Kotlin + this bundle only on an explicit bump |
| **Agent skills** (vendored) | `.agents/skills/litert-lm/SKILL.md`, `.agents/skills/litert-runtime/SKILL.md` | `litert-lm` covers the shipped `.litertlm` generative chat path (Engine/Conversation, jvmShared + Android/desktop actuals). `litert-runtime` covers the separate CompiledModel `.tflite` runtime for vision/audio/embeddings (Android-only integration). Each carries a DORAI AI/KMP scope note — read it before applying upstream steps. |

The app **never** reads this directory at runtime. Production downloads use the pinned URL in Kotlin only.

## Scope

- **In:** Catalog `.litertlm` files from HuggingFace `litert-community`: pinned commit, SHA-256, exact byte size.
- **Out:** User imports (no digest). GPU-memory heuristics, tool allowlist, and engine lifecycle — those stay in [`docs/features/on-device-inference.md`](../../features/on-device-inference.md).
- **Out:** Cloud model Elo / free-tier flags.

A refresh **verifies** pins. It does **not** silently move a pin to `main`. A bump (new commit + digest + size) is an explicit product decision because users re-download gigabytes.

## Catalogs

* [Pinned LiteRT models](models.md) — current pins vs HuggingFace

## Policy

* [Pin and bump policy](pin-policy.md) — never `main`, commit + digest + size together

## Playbooks

* [Refresh LiteRT pins](refresh-playbook.md) — check or bump (also `/update-litert-models`)

## History

* [Update log](log.md)
