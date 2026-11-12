---
okf_version: "0.2"
---

# Voice (speech) knowledge

OKF v0.2 bundle for DORAI AI's **offline voice** work — the microphone input path and the spoken
answer path. Agents use this folder for engine status, the pin policy that will apply to bundled
speech models, and the decision log.

## Runtime vs knowledge

| Layer | Path | Role |
|---|---|---|
| **Runtime** (shipped) | `composeApp/src/commonMain/kotlin/com/dorai/ai/voice/` | Voice turn state machine, speech-to-text contract, speech text pipeline |
| **Runtime** (shipped) | `composeApp/src/androidMain/kotlin/com/dorai/ai/voice/AndroidSpeechToText.kt` | Android platform speech recognizer |
| **Knowledge** (this bundle) | `docs/knowledge/voice/` | Engine inventory, pin policy for future bundled models, decision log |
| **Feature doc** | `docs/features/offline-voice-ai.md` | Product behaviour and the phase plan |

The app **never** reads this directory at runtime. Production behaviour comes from the Kotlin
sources above.

## Scope

- **In:** The speech-to-text contract, the speech text pipeline (what may be spoken, how numbers
  are read), the Android platform recognizer, and the pin policy that applies once speech models
  are bundled.
- **Out:** The product phase plan, the Phase 0 measurement gate, and the model candidate tables —
  those live in [`docs/features/offline-voice-ai.md`](../../features/offline-voice-ai.md).
- **Out:** Text-to-speech engine selection, which is a device setting and not a bundled model.

## Contents

* [Engine inventory](engines.md) — what is wired up today and what is still a candidate

## Policy

* [Speech model pin and bump policy](pin-policy.md) — the rules that apply when models are bundled

## History

* [Update log](log.md)
