---
type: Policy
title: Voice speech-model pin and bump policy
description: The rules that apply when DORAI AI bundles a speech-to-text or text-to-speech model instead of using the platform engine.
tags: [voice, on-device, integrity, policy]
status: stable
generated: { by: human:opencode, at: 2026-11-12T00:00:00Z }
---

# Goal

A bundled speech model must be **a fixed byte sequence**. A user must not get a different model
because someone pushed to `main`, and a model must never be loaded before its bytes are verified.

This policy is **not yet exercised** — DORAI AI ships no bundled speech model today, it uses the
platform recognizer and the system synthesizer. It is written now so the rules are settled before
the first model is added, not after.

# Pin shape

Each bundled model pins **three** values that must change together:

1. HuggingFace **commit** (40-char lowercase hex) in the resolve URL — never the branch name `main`
   or any other moving ref.
2. File **SHA-256** (64-char lowercase hex) — checked after download and at load.
3. Exact **sizeBytes** — gates the free-space check and the post-download length check.

URL form:

`https://huggingface.co/<owner>/<repo>/resolve/<commit>/<fileName>`

This mirrors the LiteRT catalog, which pins the same way. The difference is the directory: voice
models live in their own bundle so a speech pin bump never rides along with an LLM pin bump.

# Verification

- The digest is computed while the bytes stream to disk, so verification costs no second read.
- A download is accepted only when **both** the length and the digest match. On a mismatch the
  partial file is deleted and the failure is reported inline — nothing is loaded silently.
- The verified digest is recorded in a marker next to the model, written before the model becomes
  visible under its final name. A model is never present on disk without its marker.
- Load time re-checks the marker. A missing marker means the file is hashed once and the result
  recorded; a digest that does not match is **refused**, not loaded, and the user is told to delete
  and download it again. Nothing is deleted automatically at load time.
- A file the user supplied themselves is exempt from the pinned digest, exactly as for LiteRT
  imports.

# Resume

Downloading a speech model must survive the process being killed. That means:

- `Range` plus `If-Range` (ETag) on resume. A `200` response means the server ignored the range,
  so the download restarts from zero — a full body is **never** appended to a partial file.
- After a process death the streaming digest state is gone, so the partial file is **re-hashed
  locally** to rebuild the digest before the resumed bytes are accepted. That is a local read, not
  network traffic.
- `416` discards the partial file and restarts from zero.

# Licensing

A speech model may only be bundled when its license is compatible with the flavor it ships in.

- A model under a non-commercial license (`CC-BY-NC-*`) is **not** shipped in any flavor without
  written permission.
- GPL-3.0 components (`espeak-ng-data`, eSpeak-ng) require a legal review for the `playStore`
  flavor and are limited to `foss` until that review is done.
- A model under a permissive license (Apache-2.0, MIT) still needs its provenance recorded here.

# Bumping

A bump is an explicit product decision: a user re-downloads hundreds of megabytes. The commit,
digest and size move together, this bundle's snapshot is refreshed in the same change, and the
Phase 0 measurement is repeated when the **model** changes — not only when a URL changes.

# See also

* [Engine inventory](engines.md)
* [Feature doc: Offline Voice AI](../../features/offline-voice-ai.md)
* [LiteRT pin and bump policy](../litert/pin-policy.md)
