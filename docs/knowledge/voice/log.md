---
type: Log
title: Voice knowledge update log
description: Every change to the voice knowledge bundle, newest first.
tags: [voice, log]
status: stable
generated: { by: human:opencode, at: 2026-11-12T00:00:00Z }
---

# Update log

## 2026-11-13 — Phase 3 hardening pack A (docs + empty state)

- New bundle entries: `pin-bump.md` (how to move a catalog pin — commit, digests, sizes and
  sign-off as one atomic edit, plus the re-measure mandate) and `phase3-device-checklist.md`
  (real-device test list for Phase 3; the emulator proves functionality, never performance).
- Settings → Voice now shows an honest empty state when the model catalog is empty instead of
  hiding the whole Models area silently (`settings_voice_models_empty_title/body`, en + bn).
- Phase 3 items that require a physical device (airplane-mode full-offline test, device tiers,
  battery/thermal, APK size split, golden-audio regression) remain open — see the checklist;
  none of them may be claimed as measured without device numbers.

## 2026-11-13 — Coqui bn VITS license sign-off recorded

- `csukuangfj/vits-coqui-bn-custom_female` flipped to `licenseVerified = true` in
  `VoiceCatalog.kt` — **all flavors** (foss and playStore). The owner signed off accepting
  that the hosting repo declares no license and no model card; the model lineage is Coqui
  TTS (typically MPL-2.0). The sign-off is the owner's decision, not a licensing finding —
  do not describe it as verified licensing.
- The catalog pin (immutable commit, sizes, digests) is unchanged by the sign-off; a bump
  still requires commit + digest + sign-off to move together.
- The Settings download button is now enabled; downloads verify length + SHA-256, mark the
  model READY only after every file passes, and the voice turn stays behind Voice Mode.
- `VoiceCatalogTest` updated: the "no model is downloadable before sign-off" invariant now
  asserts the recorded sign-off instead.

## 2026-11-13 — BTSD structure coverage added to the eval set

- `VOICE_STRUCTURE_EVAL_SENTENCES` (`btsd-001`…`btsd-030`) added alongside the 200 command
  utterances: 10 each of Simple / Compound / Complex declarative sentences from **BTSD —
  Bangla Transformation of Sentence Dataset**, Rajesh Kumar Das, Mirajul Islam, sharun akter
  khushbu, Daffodil International University, Mendeley Data, DOI:10.17632/4k964xyz65.2, CC BY 4.0.
- Selection: evenly spread across file order (step n/10 per structure), pure-script (no Latin)
  so the structure effect is not confounded with code-mix; sentence length 10–120 chars;
  Bengali script presence enforced by tests.
- Source sentences were **not speech-verified**; measure structure CER separately from command CER.
- Integrity tests updated: per-structure × 10, unique ids, no id collision with the command set,
  pure-script assertion.

## 2026-11-12 — Phase 0 eval set replaced with BengVoice subset

- Draft 120-sentence hand-written eval set **replaced** by 200 utterances (20 per intent,
  deterministic even spread) taken verbatim from BengVoice — A Stratified Dataset of Code-Mixed
  Bengali-English Voice Commands for Intent Classification in Conversational AI Systems,
  Md Shahriar Hossain, Mendeley Data, DOI:10.17632/sr99ryf4ns.1, CC BY 4.0.
- Selection: every 6th utterance in file order (index 0, 6, … 114); keeps ~24% code-mixed
  utterances. Ids renumbered `bnv-001`…`bnv-200`.
- Integrity tests updated: 200 sentences, 10 intents × 20, Bengali script present, code-mixed
  utterance count ≥ 20.
- Source of truth for the set is the DOI; attribution lives in `VoiceEvalSet.kt` and
  `benchmark.md` — keep them in sync on regeneration.
## 2026-11-12 — Phase 2 download infrastructure

- `VoiceCatalog` pins the Coqui bn VITS (`csukuangfj/vits-coqui-bn-custom_female`, commit
  `67008db…`) with per-file size + SHA-256. `licenseVerified = false` because the upstream repo
  declares no license and no model card — **download disabled until sign-off is recorded**.
- `AndroidVoiceModelDownloadWorker`: WorkManager + foreground service, unique work
  `dorai-voice-model-download` with `KEEP`, Wi‑Fi-only constraint by default; optional mobile-data
  opt-in in Settings → Voice.
- Resume: byte-`Range`; kept `.part` bytes are re-hashed to rebuild the digest; `200`/`416`
  responses restart from zero; `.part` + atomically-renamed file; `.sha256` marker next to each
  verified file; pre-download storage gate (sizeBytes + 25 %).
- Settings → Voice gains model download/delete/pause/resume/re-download controls and the
  mobile-data toggle.
- Coqui pin verified against the HuggingFace API on 2026-11-12 (commit, sizes); `model.onnx`
  digest from the LFS pointer, `tokens.txt`/`config.json` digests computed by downloading them
  at the pinned commit (git blob OIDs are not SHA-256).

## 2026-11-12 — Phase 0 harness (scorer, eval set, guide)

- Bengali CER/WER scorer in `composeApp/src/commonMain/kotlin/com/dorai/ai/voice/benchmark/SpeechScoring.kt`:
  NFC, case-fold, punctuation-as-space; code-point edit distance (Bengali is combining-mark script);
  WER on whitespace-separated tokens. Platform actuals apply NFC on Android, desktop and iOS; the
  wasm value is a pass-through (target not release-ready, voice disabled there).
- Draft eval set `VOICE_EVAL_SENTENCES`: 120 sentences, 20 per category (general, names,
  code-switch, numbers, conjuncts, long forms). **Not yet reviewed by a native Bengali speaker**;
  both accents (Bangladeshi, West Bengali) are not yet confirmed fairly represented.
- Measurement guide and decision memo template added as `benchmark.md`.
- No measurement has been taken yet — Phase 0 is still open; the gate stays failed until numbers
  from real devices are in.

## 2026-11-12 — Bundle created with the Phase 1 implementation

- Bundle created alongside the first working voice turn.
- **Runtime source of truth:** `composeApp/src/commonMain/kotlin/com/dorai/ai/voice/` and
  `composeApp/src/androidMain/kotlin/com/dorai/ai/voice/AndroidSpeechToText.kt`.
- Speech-to-text is the **Android platform recognizer** — no bundled model, no native dependency,
  no APK size cost. `EXTRA_PREFER_OFFLINE` is set; a device without an offline language model
  reports a network error and the turn fails honestly instead of streaming audio away.
- Text-to-speech stays the **system engine**; the voice turn reuses the existing speech path.
- The pin and bump policy was written **before** any model is bundled, so the rules are settled
  rather than retrofitted. It is not yet exercised by a catalog.

## 2026-11-12 — Model candidate survey (from the feature plan)

- Official Bengali IndicWhisper has no usable artifact on HuggingFace.
- `ai4bharat/indic-conformer-600m-multilingual` is ~2.55 GB in fp32 — too heavy for a phone
  unless an int8 export exists.
- `csukuangfj/sherpa-onnx-whisper-small` (int8) is ~357 MiB.
- TTS candidates: `csukuangfj/vits-coqui-bn-custom_female` ~109 MiB, `csukuangfj/vits-mimic3-bn-multi_low`
  ~73 MB, `facebook/mms-tts-ben` ~290 MB but **CC-BY-NC-4.0** and therefore excluded without written
  permission, `ai4bharat/indic-parler-tts` 3.75 GB and therefore not a phone candidate.
- `csukuangfj2/sherpa-onnx-streaming-zipformer-bn-vosk-2026-02-09` is the first candidate to
  measure for Bengali STT.
