---
type: Checklist
title: Voice Phase 3 device checklist
description: What to measure and verify for Offline Voice in Phase 3 (hardening) — every item requires a real device; the emulator proves functionality, never performance.
tags: [voice, phase3, device, testing]
status: draft
generated: { by: human:opencode, at: 2026-11-13T00:00:00Z }
---

# Voice Phase 3 — device checklist

**Every item below requires a real device.** An emulator proves nothing about performance:
it runs x86_64 translated code, has no real mic DSP, thermals, battery curve or offline
Bengali speech model from the device ecosystem. Record results in this file's appendix or in
`benchmark.md`'s decision-memo table; never claim a pass without numbers.

## Test device matrix (fill in)

| Device | SoC / RAM | Android | Offline Bengali model? | Accent |
|---|---|---|---|---|
| e.g. low-tier (2 GB, Android 10) | | | | |
| e.g. mid-tier (4–6 GB) | | | | |

## 1. Fresh install, airplane mode

1. Install the foss debug APK on a factory-fresh (or freshly app-data-cleared) device.
2. Enable airplane mode; disable Wi-Fi and mobile data.
3. Start DORAI AI, open Settings → Voice, enable Voice Mode.
4. Mic button: dictate “আজ ঢাকায় আবহাওয়া কেমন?” — expect a transcript; if the device has no
   on-device Bengali model, expect the honest `NETWORK_REQUIRED`-style failure, never a
   silent send.
5. Assistant reply is read back; check Bengali conjunct rendering and numeral verbalization
   ("২০২৬" → "দুই হাজার ছাব্বিশ") on the actual letter shapes, not just the string code.

Record: which model (if any) the recognizer used, latency to first token, and the exact error
message when the model is missing.

## 2. Download chain, real network then real airplane mode

1. Fresh install, online: download the Coqui bn VITS model (~110 MB) over Wi-Fi from Settings →
   Voice; watch pause → resume → READY; verify `.sha256` markers and `READY` state survive an
   app kill mid-download.
2. Download over mobile data twice: first refused without the opt-in toggle, then allowed
   with the toggle (it must ask every time or document the gating).
3. Put the device in airplane mode, open DORAI AI, Settings → Voice: model state shows
   READY from the manifest; the UI never tries to re-download or re-hash silently.
4. Delete the model in airplane mode; confirm the `models/voice/<id>/` directory is gone and
   Voice Mode continues with the system recognizer/synthesizer only.

## 3. Device tiers

- **Low-tier** (e.g. 2 GB RAM, Android 10): app start time with the model READY, memory
  residency under a voice turn, background eviction behaviour (dialog or silent, whichever is observed).
- **Mid-tier**: repeat. Record p50/p95 voice-turn latency on both tiers.

## 4. Battery and thermal

- 10 consecutive voice turns, screen at fixed brightness; measure mAh via batterystats /
  the device's own drain accounting; compare against 10 chat turns without voice.
- Sustained use until the device is warm — observe capture bias, recognizer drops, or FGS
  kill (Android may kill the foreground download at aggressively low battery).

## 5. Golden-audio regression hooks

- For any future pin bump, re-run Phase 0 TTS/STT measurements on this same device; a claim
  that the bump "obviously doesn't change quality" is not acceptable.
- BengVoice command set (200) + BTSD structure set (30): CER/WER per structure (Simple /
  Compound / Complex) and per intent; record in `benchmark.md`'s memo table.

## 6. Bengali coverage spots

- Mixed Bengali-English input with code-mixing ("明日의 weather forecast কি বলছে?");
- digits: Eastern Arabic (Bengali-region) numerals ০-৯;
- conjunct-heavy words (বিজ্ঞান, কৃষ্টি, আত্মসম্মান);
- IPA-like insertions from the synthesizer output (should never appear).

## Results appendix

| Date | Device | Item | Result | Notes |
|---|---|---|---|---|
| | | | | |
