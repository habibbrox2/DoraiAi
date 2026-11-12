---
type: Log
title: Voice knowledge update log
description: Every change to the voice knowledge bundle, newest first.
tags: [voice, log]
status: stable
generated: { by: human:opencode, at: 2026-11-12T00:00:00Z }
---

# Update log

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
