---
type: Playbook
title: Phase 0 measurement guide
description: How to run the Bengali STT/LLM/TTS measurement gate — the scorer, the eval set, the device procedure and the decision memo.
tags: [voice, benchmark, phase-0, cer, wer]
status: stable
generated: { by: human:opencode, at: 2026-11-12T00:00:00Z }
---

# Phase 0 — measurement guide

Phase 0 in [`docs/features/offline-voice-ai.md`](../../features/offline-voice-ai.md) is a **blocking
gate**. This page is the harness inventory and the procedure. Nothing here gets called a pass until
numbers from real devices are in the table below.

## What exists (this bundle, code)

| Piece | Where | Role |
|---|---|---|
| CER/WER scorer | `composeApp/src/commonMain/kotlin/com/dorai/ai/voice/benchmark/SpeechScoring.kt` | Normalises (NFC, case-fold, punctuation-as-space) and computes code-point CER and word WER |
| Bengali eval set | `composeApp/src/commonMain/kotlin/com/dorai/ai/voice/benchmark/VoiceEvalSet.kt` | 200 utterances, 20 per intent, taken from BengVoice (CC BY 4.0), DOI:10.17632/sr99ryf4ns.1 |
| Scorer test suite | `composeApp/src/commonTest/kotlin/com/dorai/ai/voice/benchmark/` | Known pairs and eval-set integrity |

The scorer is pure Kotlin and runs on every target. Bengali code points (not UTF-16 code units) are
the edit distance, because Bengali is a combining-mark script.

## Metrics and gates

| Metric | Target | Where |
|---|---|---|
| Clean CER | ≤ 15% | on-device, flagship + mid-range |
| Noisy CER | ≤ 25% | on-device, with background noise |
| TTS comprehensibility | ≥ 5 Bengali native raters | recorded clips, chat answers of 2–3 sentences |
| End-to-end latency | not set — measure, do not guess | model load + STT RTF + first token + first sentence TTS |
| Peak RAM / thermal / battery | record; budget set after measurement | per turn |
| LLM Bengali quality | judged by native speakers, not metrics | decision memo |

Suggested acceptance copy: if clean CER > 15% for the **best** STT candidate, Phase 0 fails and
Bengali voice does not ship regardless of how smooth the rest is.

## Device procedure

1. Install the debug build on one flagship and one mid-range device.
2. For each candidate STT engine (Android system recognizer today; sherpa-onnx after it is wired
   in): speak all 120 sentences cleanly, then again with moderately loud background noise.
3. Capture the recognizer transcripts verbatim — do not correct them.
4. Normalise and score with `SpeechScoring`:

```kotlin
val score = SpeechScoring.score(reference = evalSentence.text, hypothesis = transcript)
println("${evalSentence.id}: CER=%.4f WER=%.4f".format(score.cer, score.wer))
```

5. Record results per device, per accent (Bangladeshi and West Bengali), per noise condition.
6. For TTS: synthesize 5 fixed 2–3-sentence chat answers, play them to ≥ 5 native Bengali raters,
   count how many end the chain as understandable.
7. For LLM Bengali quality: run the same 5 prompts through the on-device candidate models and have
   the same raters judge adequacy.

## Eval set status

`VoiceEvalSet.kt` holds 200 utterances (20 per intent) taken verbatim from **BengVoice —
A Stratified Dataset of Code-Mixed Bengali-English Voice Commands for Intent Classification in
Conversational AI Systems**, Md Shahriar Hossain, Mendeley Data, DOI:10.17632/sr99ryf4ns.1,
**CC BY 4.0**. The subset was selected deterministically (every 6th utterance in file order,
index 0, 6, … 114) and keeps the dataset's 24 % code-mixing rate. The utterances are natively
generated Bangladeshi voice-assistant commands, not hand-written prose.

Remaining caveats before the memo is signed off:

- the selection is a fixed subset, not a random draw — confirm it is representative by intent;
- accent balance (Bangladeshi vs West Bengali) is inherited from the dataset, not benchmarked
  across both speaker populations;
- utterance length is short command-style speech (10–54 chars); long-form prose coverage is
  deliberately small — if long-pass recognition matters, extend the set from the full DOI data.

The full 1,200-utterance dataset (plus fold splits) is at the DOI above; re-download it rather
than committing it wholesale. Attribution is carried in `VoiceEvalSet.kt` and here; keep both in
sync if the set is regenerated.

## Decision memo template

```markdown
# Memo — Bengali voice Phase 0 decision
Date / devices / OS:
STT candidates tested:
TTS candidates tested:
LLM candidate whose Bengali was judged adequate:

| Candidate | Clean CER | Noisy CER | Device(s) |
|---|---|---|---|
| | | | |

TTS intelligibility (raters / understood):
LLM Bengali quality (rater scores):
Latency (model load / STT RTF / first token / first sentence / total):
Peak RAM, thermal, battery per turn:

Licence notes:
Verdict: ship an offline STT/TTS engine | ship system fallback only | "Bengali voice not ready"
```

## What this harness does not cover

- LLM Bengali quality — there is no automated metric for it; it goes in the memo, judged by raters.
- TTS intelligibility — scoring does not judge speech; raters do.
- RTF, load time and RAM — those are measured on the device during the runs above.
