---
type: Playbook
title: Voice model pin bump
description: How to move a pinned voice model catalog entry — commit, digests, sizes, markers, and when to re-measure.
tags: [voice, pin, catalog, upgrade]
status: stable
generated: { by: human:opencode, at: 2026-11-13T00:00:00Z }
---

# Pin-bump playbook

Use this when an upstream voice model is re-converted or re-published and the catalog must
move to a new commit. Never bump the commit alone — commit, digests, sizes and sign-off move
together.

## 1. Collect the new pin from the source of truth

- Pull `https://huggingface.co/api/models/<repo>` — record `sha` (the commit id).
- Pull `https://huggingface.co/api/models/<repo>/tree/<sha>?recursive=true` — record each
  file's `size`, and the `.lfs.oid` for files stored via LFS (that IS the SHA-256).
- For small non-LFS files, download each at the pinned commit and compute SHA-256 locally
  (git blob OIDs are not usable digests — they were the reason `VoiceCatalogTest` failed
  originally).

## 2. Record the sign-off *before* editing the catalog

- A re-published model is a re-published license question. Get the owner's sign-off that the
  new build is shippable (the 2026-11-13 sign-off applies to the pinned build, not "any
  future Coqui release").
- If the license is ever declared upstream, record it verbatim; if it is silence again,
  record the owner's decision as such.

## 3. Update the catalog as one atomic edit

In `composeApp/src/commonMain/kotlin/com/dorai/ai/voice/VoiceCatalog.kt`:

- `downloadUrlPrefix` — new immutable commit (the test `isImmutableVoiceModelUrl` fails on `main`);
- `files` — new `sizeBytes` and `sha256` per file;
- `licenseVerified` — keep `true` only if this pin replaced one with a recorded sign-off;
  a brand-new voice entry needs its own sign-off (see `VoiceCatalogTest`);
- update the doc header and `log.md` with the date and source of the verification.

## 4. Re-measure before shipping

A new encoder/decoder can change quality, and a wrong digest ships bad bytes silently:

- `:composeApp:desktopTest --tests "com.dorai.ai.voice.*"` must pass;
- on a device, re-run the Phase 0 TTS/ASR measurement against the new model — CER/WER and
  intelligibility re-measured, not assumed (the old numbers are for the old pin);
- confirm `.sha256` markers match the new digests on disk after a download.

## 5. Delete on mismatch

If verification fails after an upgrade, do **not** force READY: delete the `.part` and let the
worker re-download. If the pinned sizes change, re-check the pre-download free-space gate
(sizeBytes + 25 %) still makes sense.

## 6. Rollback

The previous commit remains valid on HuggingFace indefinitely (commits are immutable). To roll
back, revert the single catalog edit; distrust markers written from the new pin until
re-verified.

## Non-goals

- Do not "minor bump" a model by re-uploading identical bytes — the digest catches byte
  changes, but a deliberate re-publish with identical content still invalidates the recorded
  provenance; treat it as a new pin with a new date.
- Do not edit files on disk under `models/voice/` — the worker re-verifies them anyway;
  hand-edited model dirs are a drift risk and should be deleted and re-downloaded.
