---
applyTo: ".github/workflows/**"
---

Follow `AGENTS.md` and `agents/validation-policy.md`. Keep workflows least-privilege, read-only by default, concurrency-aware, and deterministic. Preserve the stable `Run Unit Tests` aggregate check. A skipped job is acceptable only when the change classifier explicitly determines that the job is not applicable; every selected job must succeed. Never add workflow write-back, secrets exposure, or unreviewed third-party actions.
