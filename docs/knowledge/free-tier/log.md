# Free-tier knowledge update log

## 2026-11-09

* **Alignment fix** (manual, not a live refresh): `openrouter.md` now lists the two routing meta ids present in the runtime since 2026-08-11 (`openrouter/auto`, `auto`), restoring doc ↔ `FreeTierModels.kt` parity at 17 ids. No pricing data was re-fetched; `stale_after` and provenance are unchanged. Refresh from live sources still requires the environment-side `/update-free-tier-models` command.
* **Doc correction**: bundle index files pointed at fictitious repo-local `.grok/skills/update-*` paths; these are environment-side agent commands, not repository scripts.

## 2026-08-11

* **Update**: Live refresh via `process:update-free-tier-models`.
  * **OpenRouter** — added `inclusionai/ling-3.0-tiny:free`, `nvidia/nemotron-3.5-lightning:free`; removed `inclusionai/ling-3.0-flash:free`, `poolside/laguna-m.1:free` (no longer $0 on models API). 15 → 15 ids.
  * **Ollama Cloud** — kept Low-only API ids `gpt-oss:20b`, `gemma4:31b`, `nemotron-3-nano:30b`; removed bare aliases `gemma4`, `nemotron-3-nano` (not on `/v1/models`; cloud usage for tagged ids remains Low). 5 → 3 ids.
* **Initialization**: Created OKF free-tier bundle from the existing `FreeTierModels.kt` snapshot (OpenRouter 15 ids, Ollama Cloud 5 ids). Policy and sources documented; no live re-fetch on seed.
