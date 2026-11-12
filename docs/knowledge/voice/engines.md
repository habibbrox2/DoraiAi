---
type: Reference
title: Voice engine inventory
description: Which speech-to-text and text-to-speech engines DORAI AI uses today, and which are still candidates.
tags: [voice, speech, stt, tts, android]
status: stable
generated: { by: human:opencode, at: 2026-11-12T00:00:00Z }
---

# Engine inventory

## Shipped

| Role | Engine | Where | Notes |
|---|---|---|---|
| Speech-to-text | Android platform recognizer (`android.speech.SpeechRecognizer`) | `composeApp/src/androidMain/.../voice/AndroidSpeechToText.kt` | No model to download, no native dependency, no APK size cost. `EXTRA_PREFER_OFFLINE` is set, so the device prefers its on-device model. |
| Text-to-speech | System engine (`nl.marc-apps:tts`, `TextToSpeechEngine.SystemDefault` then `Google`) | `androidApp/src/main/kotlin/com/dorai/ai/MainActivity.kt` | Existing speech path; the voice turn reuses it. Bengali depends on the device language pack. |

The platform recognizer is **device-dependent**. A device with no offline Bengali model reports a
network error, and the voice turn fails with `NETWORK_REQUIRED` rather than quietly sending audio
away. The user is told what happened instead of the app pretending the turn was private.

## Download and integrity (Phase 2 infrastructure)

The Coqui bn VITS files are pinned in `composeApp/src/commonMain/kotlin/com/dorai/ai/voice/VoiceCatalog.kt`
(`models/voice/<id>/`, immutable commit `67008db…`, per-file size + SHA-256). The download runs in
`AndroidVoiceModelDownloadWorker` — foreground service, unique work keeping existing work, Wi‑Fi-only
constraint by default (mobile data only after the user opts in). Downloads resume with a byte-Range;
kept `.part` bytes are re-hashed to rebuild the digest; each file's length and SHA-256 are checked
before rename; a `.sha256` marker sits next to each verified file; the settings surface exposes
download / pause / resume / delete / re-download plus the mobile-data opt-in.

**The catalog entry ships with `licenseVerified = false`** because the upstream repo declares no
license and no model card. The download button is disabled and no bytes move until the sign-off is
recorded in the catalog.

## Still candidates

These are **not** wired up. They are the Phase 0 measurement candidates listed in
[`docs/features/offline-voice-ai.md`](../../features/offline-voice-ai.md); the table there is the
source of truth for their sizes and licenses.

| Role | Candidate | Why it is still a candidate |
|---|---|---|
| Speech-to-text | `csukuangfj2/sherpa-onnx-streaming-zipformer-bn-vosk-2026-02-09` | Needs a native binding and an APK size measurement; the Bengali CER/WER is unmeasured. |
| Speech-to-text | `csukuangfj/sherpa-onnx-whisper-small` (int8) | Same native binding question; Bengali Whisper quality is known to be weak. |
| Text-to-speech | `csukuangfj/vits-coqui-bn-custom_female` | License sign-off not complete. |
| Text-to-speech | `csukuangfj/vits-mimic3-bn-multi_low` | `espeak-ng-data` is GPL-3.0, so the `playStore` flavor needs a legal review. |
| Text-to-speech | eSpeak-ng `bn` | GPL-3.0; restricted to the `foss` flavor. |

## Why the contract, not the engine

`SpeechToText` in `composeApp/src/commonMain/.../voice/SpeechToText.kt` is the only thing the chat
pipeline knows about. An on-device sherpa-onnx or whisper.cpp binding is expected to implement that
same interface, which is why adding one must not change the chat screen, the settings screen, or the
speech text pipeline.

## See also

* [Pin and bump policy](pin-policy.md)
* [Feature doc: Offline Voice AI](../../features/offline-voice-ai.md)
