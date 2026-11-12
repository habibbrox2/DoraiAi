# Offline Voice AI (Bengali) — Android Implementation Plan (সংশোধিত)

Nov 12, 2026 · @Habib Brox

## অবস্থা ও পরিসর

এই নথি **Android অ্যাপের** জন্য বাংলা offline voice-এর বাস্তবায়ন পরিকল্পনা। **Phase 1 এখন implement করা হয়েছে** — নিচের "বাস্তবায়িত Phase 1" অংশটি shipped অবস্থা বর্ণনা করে; বাকি অংশগুলো এখনো পরিকল্পনা, বর্তমান ফিচারের বিবরণ নয়।

- **Status:** Phase 1 IMPLEMENTED (Android; platform speech recognizer)। Phase 2+ পরিকল্পিত।
- **Last verified:** 2026-11-12 (HuggingFace API; সাইজ ও pin Phase 0-এ আবার যাচাই হবে)।
- **লক্ষ্য:** DORAI AI-এর সাথে বাংলায় কথোপকথন — mic input, spoken output — সম্পূর্ণ on-device, API key, network বা per-turn খরচ ছাড়া।
- **পরিসর:** শুধু Android। iOS ও Desktop আলাদা পরিকল্পনা; `commonMain`-এ interface থাকবে যাতে পরে যোগ করা যায়, কিন্তু সেসব `actual` এই নথির বাইরে।
- **ভাষা:** launch ভাষা `bn`; architecture অন্য ভাষা আটকাবে না; English পুরো সমর্থিত থাকবে।
- **নিরাপত্তা সীমা:** বিদ্যমান remote provider, on-device LiteRT chat ও chat UI ভাঙা যাবে না। Voice Mode আলাদা module, flag-এর পেছনে।

### Non-goals

- Wake-word বা continuous hands-free listening। (শুধু end-of-speech ধরার VAD non-goal নয়।)
- Full-duplex (বলার মাঝে interrupt)।
- Voice cloning, speaker identification।
- Streaming TTS। v1-এ STT-র UX হবে batch (record → transcribe)। streaming-architecture-এর মডেল ব্যবহার করলেও সেটা batch মোডে চলবে।
- dorai-ui / interactive-UI মোডে voice input। Voice turn সাধারণ chat turn।
- iOS, Desktop ও Wasm।
- Remote-provider কোড, `MODEL_CATALOG` বা LiteRT chat engine বদলানো।

## বর্তমান অবস্থা ও লক্ষ্য

Android-এ ডাউনলোডের কাঠামো (WorkManager, streaming SHA-256, atomic rename) আগে থেকেই আছে; speech-to-text, mic ও Voice UI একেবারে নতুন।

| বিষয় | আজ | মন্তব্য |
| --- | --- | --- |
| Local LLM | LiteRT-LM (`inference/` প্যাকেজ) | `.litertlm` ছাড়া অন্য ফরম্যাট নয় |
| Model catalog | `MODEL_CATALOG` (`LocalModelCatalog.kt`) | immutable HF commit URL + SHA-256 + `sizeBytes` |
| Model ডাউনলোড | `LiteRTInferenceEngine`-এ in-process coroutine + `ModelDownloadService` (FGS) | WorkManager নয়; app মরলে ডাউনলোড থামে, resume নেই |
| APK আপডেট worker | `AndroidUpdateDownloadWorker` (unique work, `CONNECTED`, `.part` → atomic rename, ৩ retry) | পুনর্ব্যবহারযোগ্য pattern; `setForeground` ডাকে না — voice worker-কে ডাকতে হবে |
| TTS | `TextToSpeechService` (`nl.marc_apps.tts`); rate/pitch/volume/voice settings আছে | বাংলা ভয়েস device/Google TTS language pack-এর ওপর নির্ভর, সব OEM-এ offline নিশ্চিত নয় |
| STT | নেই | greenfield |
| Mic permission | `RECORD_AUDIO` manifest-এ নেই | manifest + runtime permission flow লাগবে |
| FGS | `FOREGROUND_SERVICE_DATA_SYNC` আছে | `ModelDownloadService` ব্যবহার করে |

### লক্ষ্য

1. ব্যবহারকারী Voice Mode চালু করলে প্রয়োজনীয় মডেল না থাকলে ডাউনলোড queue হবে — কখনো নীরবে নয়, Voice Mode-এ সম্মতির আগেও নয়।
2. ডাউনলোড app background-এ গেলে ও process মরলেও টিকবে, বাধার পর resume করবে, বৈধ মডেল আবার নামাবে না।
3. প্রতিটি মডেল usable হওয়ার আগে length ও SHA-256 মিলবে।
4. অডিও ও transcript device-এই থাকবে; voice turn log, sync বা settings export-এ যাবে না।
5. বাংলা launch-মান; design bilingual-ready।
6. **(নতুন)** Voice turn-এর end-to-end latency ও RAM ব্যবহার Phase 0-এ মাপা ও বাজেটভুক্ত হবে।

## বাস্তবায়িত Phase 1 (shipped)

Phase 1 Android-এ implement করা হয়েছে। STT-এর জন্য **Android-এর নিজস্ব speech recognizer** (`android.speech.SpeechRecognizer`) ব্যবহার হয় — কোনো মডেল ডাউনলোড নেই, কোনো native dependency নেই, তাই APK সাইজ বাড়ে না। এটি মূল পরিকল্পনার "প্রথম প্রার্থী" (sherpa-onnx) নয়, বরং পরিকল্পনার STT টেবিলে থাকা **system `android.speech` fallback**। পরে on-device sherpa-onnx binding একই `SpeechToText` interface-এর পেছনে বসবে, ফলে chat UI বা settings বদলাতে হবে না।

### কী কী কাজ করে

- **Voice Mode টগল** — Settings-এর "Voice" সেকশন। ডিফল্ট **OFF**, কারণ Phase 0-র on-device measurement gate এখনো sign-off হয়নি।
- **Mic বাটন** — composer-এর ডান পাশে; খালি composer-এর সময় দেখা যায়। চাপলে mic চালু, আবার চাপলে শোনা বন্ধ ও লেখা পাঠানো হয়।
- **Permission** — `RECORD_AUDIO` runtime permission; শুধু mic বাটন চাপলেই চাওয়া হয়। অস্বীকার করলে text chat অপরিবর্তিত থাকে এবং settings-এর পথ দেখানো হয়।
- **Transcript একটি সাধারণ USER turn** — voice turn-এর কোনো বাড়তি tool, shell বা network access নেই; এটি ঠিক টাইপ করা বার্তার মতোই persist হয়।
- **উত্তর সাধারণভাবে পড়া হয়** — শুধু চূড়ান্ত ASSISTANT turn। উত্তর আগে markdown থেকে বিশুদ্ধ করা হয়, তারপর বাক্যে ভাগ হয় (`।`, `?`, `!`, অনুচ্ছেদ-বিচ্ছেদ), আর প্রতিটি বাক্য আলাদাভাবে synthesize হয়।
- **সংখ্যা বাংলা শব্দে** — prose-এর সংখ্যা ("৫", "২০২৬") পড়ার সময় "পাঁচ", "দুই হাজার ছাব্বিশ" হয়। কিন্তু identifier, version, decimal বা টাকার পরিমাণ ("v2.1", "৳৫০", "1.5GB") যেমন লেখা, তেমনই থাকে।
- **বাংলা-ইংরেজি মিশ্র শব্দ** — "কোডAPI" লেখা থাকলে spoken আকারে "কোড API" হয়, যাতে synthesizer একটি অবাচ্ছন্ন্য টোকেন না পড়ে।
- **Emoji ও URL** — কথায় পড়া হয় না। Bengali লেখা অক্ষত থাকে; ZWJ, variation selector বা যুক্তাক্ষর কোনো code point সরানো হয় না।

### সীমাবদ্ধতা (সৎভাবে বলা)

- অফলাইন কাজ করা **ডিভাইসের ওপর নির্ভর করে**: recognizer-কে offline model ব্যবহার করতে বলা হয়, কিন্তু ডিভাইসে বাংলা offline language pack না থাকলে network error আসে। তখন ভয়েস turn ব্যর্থ হয় এবং ব্যবহারকারীকে বলা হয় — অডিও নিঃশব্দে পাঠিয়ে দেওয়া হয় না।
- Offline বাংলার **মান এখনো মাপা হয়নি** (Phase 0 gate)।
- Desktop, iOS ও Wasm-এ voice entry point নেই; `SpeechToText` interface commonMain-এ থাকে, platform actual-গুলো `null` ফেরত করে।

### Key Files

| File | Purpose |
|------|---------|
| `composeApp/src/commonMain/.../voice/VoiceModeState.kt` | Voice turn-এর state (`IDLE`/`LISTENING`/`TRANSCRIBING`/`THINKING`/`SPEAKING`) ও error enum |
| `composeApp/src/commonMain/.../voice/SpeechToText.kt` | Platform-নিরপেক্ষ STT contract ও `createSpeechToText()` factory |
| `composeApp/src/commonMain/.../voice/VoiceModeController.kt` | Voice turn-এর state machine; transcript একটি সাধারণ USER turn হিসেবে বের হয় |
| `composeApp/src/commonMain/.../voice/VoiceText.kt` | Speech-এর জন্য লেখা পরিষ্কার করা: markdown/code/URL/emoji সরানো, সংখ্যা বাংলা শব্দে, বাক্যে ভাগ |
| `composeApp/src/commonMain/.../voice/VoiceReply.kt` | যে উত্তরটি voice turn-এর জন্য পড়া হবে |
| `composeApp/src/androidMain/.../voice/AndroidSpeechToText.kt` | Android platform recognizer-এর wrapper |
| `composeApp/src/{desktopMain,iosMain,wasmJsMain}/.../voice/SpeechToText.*.kt` | অ-Android actual: এখানে voice নেই |
| `composeApp/src/commonMain/.../ui/chat/ChatViewModel.kt` | Voice turn চালায়, transcript পাঠায়, উত্তর চিহ্নিত করে |
| `composeApp/src/commonMain/.../ui/chat/DoraiChatScreenConnected.kt` | Mic বাটন, permission flow, উত্তর পড়া |
| `composeApp/src/commonMain/.../ui/components/v2/DoraiComposer.kt` | Recording অবস্থা ও status line |
| `composeApp/src/commonMain/.../ui/settings/VoiceSettings.kt` | Settings-এর "Voice" সেকশন |
| `composeApp/src/commonMain/.../tools/PermissionController.kt` | `AppPermission.RECORD_AUDIO` |
| `composeApp/src/commonMain/.../data/AppSettings.kt` | `voice_mode_enabled` flag |
| `docs/knowledge/voice/` | OKF bundle: engine inventory, pin policy, log |
## প্রোডাক্ট আচরণ

Voice Mode চালু করাই একমাত্র automatic trigger; সম্মতি ছাড়া কিছু নামে না, আর STT তৈরি হলেই Voice Mode ব্যবহারযোগ্য।

### প্রথমবার চালু করা

1. ব্যবহারকারী **Voice Mode** টগল করে (settings-এ একটি "Voice" সেকশন + chat composer-এ mic button)। সব লেবেল ও বার্তা `bn`-সহ সব সমর্থিত locale-এ থাকবে; English fallback নয়।
2. অ্যাপ পরীক্ষা করে: RAM floor, খালি storage, বিদ্যমান মডেলের marker/digest ঠিক আছে কিনা।
3. যাচাই পাস করলে ও মডেল না থাকলে **একটি** সম্মতি স্ক্রিন: কোন মডেল, মোট সাইজ, নেটওয়ার্ক শর্ত (Wi‑Fi ডিফল্ট; mobile data শুধু আলাদা সম্মতিতে), auto-start আচরণ। এক ট্যাপে নিশ্চিত, ডাউনলোড সাথে সাথে শুরু।
4. যাচাই ব্যর্থ হলে ঠিক কোনটি ব্যর্থ ও কী করতে হবে দেখানো হবে (জায়গা খালি করুন / Wi‑Fi ধরুন / ডিভাইস অসমর্থিত) — সাধারণ error নয়।
5. **(নতুন)** STT ready হলেই Voice Mode চালু। Offline TTS ডাউনলোড শেষ না হওয়া পর্যন্ত system TTS ব্যবহার হবে, সাথে স্পষ্ট অবস্থা-লেবেল।

### ডাউনলোড নিয়ম

- Voice Mode first-enable ছাড়া কিছু নামে না।
- ডিফল্ট `NetworkType.UNMETERED`। Mobile data টগল অন থাকলে `NetworkType.CONNECTED`, এবং প্রতিটি mobile-data ডাউনলোডে আলাদা নিশ্চিতকরণ। (মূল প্ল্যানের `METERED` বাদ: সেটা Wi‑Fi-তে চলে না।)
- Storage gate: `sizeBytes + 25%` খালি না থাকলে শুরুই হবে না। (দুই জায়গার ২০%/২৫% একত্র করা হলো।)
- Resume: `Range` header, `.part` ফাইলের দৈর্ঘ্য থেকে offset। unique work নাম `dorai-voice-model-download`, existing-work policy **`KEEP`** (`REPLACE` চলমান ডাউনলোড ভেঙে দিত)।
- মডেল usable দেখানো হবে শুধু length **এবং** SHA-256 মিললে। না মিললে partial ফাইল মুছবে, ব্যর্থতা রেকর্ড করবে, inline দেখাবে।
- একবারে একটি ডাউনলোড; অগ্রাধিকার STT, তারপর TTS।
- প্রতি মডেলে নিয়ন্ত্রণ: Pause / Resume / Cancel / Delete / Re-download। Delete marker-ও মোছে; re-download শূন্য থেকে যাচাই করে।
- Voice Mode বন্ধ বা মডেল delete করলে সংশ্লিষ্ট কাজ থামবে; অনুরোধ ছাড়া কিছু মুছবে না, তবে "X MB খালি করবেন?" প্রস্তাব দেখানো হবে।
- Settings import কখনো ডাউনলোড শুরু করবে না।

## মডেল কৌশল

কোনো মডেল catalog-এ ঢুকবে না Phase 0 gate পাস করার আগে; নিচের সব মান 2026-11-12-র HF API যাচাই, কিন্তু বাংলার মান এখনো মাপা হয়নি।

### কঠিন শর্ত

- প্রতিটি মডেল **immutable HF commit**, সঠিক `sizeBytes` ও SHA-256 দিয়ে pin করা। branch URL থাকলে unit test build ফেল করবে।
- Pin-bump পদ্ধতি `docs/knowledge/litert/` (`pin-policy.md`, `refresh-playbook.md`)-এর মতো; আলাদা `docs/knowledge/voice/` bundle হবে।
- Gate পাসের আগে কোনো build-এ (flag-এর পেছনেও) manifest, catalog entry বা ডাউনলোড যাবে না।

### STT প্রার্থী

| প্রার্থী | সাইজ | লাইসেন্স | অবস্থান |
| --- | --- | --- | --- |
| `csukuangfj2/sherpa-onnx-streaming-zipformer-bn-vosk-2026-02-09` (upstream `alphacep/vosk-model-small-streaming-bn`) | Phase 0-এ মাপা হবে | Apache-2.0 | **প্রথম মাপার প্রার্থী:** ছোট ও দ্রুত; batch মোডে চালানো যাবে |
| `csukuangfj/sherpa-onnx-whisper-small` (int8) | 374,668,597 B (\~357 MiB) | Whisper MIT; repo-তে tag নেই, ফাইল ধরে যাচাই বাকি | দ্বিতীয় প্রার্থী; বাংলায় Whisper-এর মান দুর্বল বলে পরিচিত, মাপতে হবে |
| বাংলায় fine-tune করা Whisper-small (community) | মাপা হবে | provenance ও লাইসেন্স যাচাই বাকি | sherpa-onnx ফরম্যাটে রূপান্তর লাগবে; accuracy বাড়াতে পারে |
| `ai4bharat/indic-conformer-600m-multilingual` | \~2.55 GB (fp32) | MIT, `gated: auto` | সবচেয়ে সঠিক হতে পারে, কিন্তু mid-range phone-এর জন্য ভারী; int8 export না মিললে বাদ |
| Whisper tiny / base | \~75 / \~145 MB | MIT | শুধু control arm |
| System `android.speech` | ০ | device-নির্ভর | শুধু fallback; offline নিশ্চিত নয় |
| SenseVoice-Small, Moonshine | — | — | বাদ (বাংলা সমর্থন নেই / শুধু English) |

Official IndicWhisper-এর কোনো ব্যবহারযোগ্য বাংলা artifact HF-এ পাওয়া যায়নি।

### TTS প্রার্থী

| প্রার্থী | সাইজ | লাইসেন্স | অবস্থান |
| --- | --- | --- | --- |
| `csukuangfj/vits-coqui-bn-custom_female` | 114,287,329 B (\~109 MiB) | Coqui; tag নেই, সাধারণত MPL-2.0 — যাচাই বাকি | **প্রধান প্রার্থী** |
| `csukuangfj/vits-mimic3-bn-multi_low` | 76,363,490 B (\~73 MB) | Mimic3 lineage; **`espeak-ng-data` GPL-3.0** — playStore flavor-এ আইনি যাচাই বাধ্যতামূলক | ছোট-সাইজ প্রার্থী |
| eSpeak-ng `bn` | \~15–25 MB | GPL-3.0 | যান্ত্রিক, কিন্তু সম্পূর্ণ offline; লেবেল-করা fallback, শুধু foss flavor-এ |
| System TTS | ০ | device-নির্ভর | ডিফল্ট zero-download; offline নিশ্চিত নয় |
| `facebook/mms-tts-ben` | \~290 MB repo | **CC-BY-NC-4.0** | লাইসেন্স বাধা; লিখিত অনুমোদন ছাড়া কোনো flavor-এ নয় |
| `ai4bharat/indic-parler-tts` | 3,751,321,772 B | Apache-2.0 | ফোনের জন্য অসম্ভব ভারী; স্থগিত |

### Phase 0 gate (blocking)

1. Bengali eval set: **২০০টি** utterance — BengVoice (Md Shahriar Hossain, Mendeley Data, DOI:10.17632/sr99ryf4ns.1, CC BY 4.0) থেকে প্রতিটি intent থেকে ২০ করে, ১০ intent; native-generated code-mixed Bengali-English। Harness তৈরি হয়েছে — CER/WER স্কোরার ও eval set `composeApp/src/commonMain/.../voice/benchmark/`-এ।
2. প্রতিটি STT প্রার্থীর CER ও WER on-device মাপা (flagship + mid-range, noise-সহ), Unicode NFC normalization-এর পর। লক্ষ্য (প্রস্তাবিত, মাপার পর পুনর্বিবেচ্য): clean CER ≤ 15%, noisy ≤ 25%।
3. TTS বোধগম্যতা: বাংলাভাষী ≥ ৫ জন rater; ২–৩ বাক্যের chat উত্তরে শেষ পর্যন্ত বোঝা যাচ্ছে কিনা।
4. **(নতুন)** On-device LLM-এর বাংলা উত্তরের মান যাচাই। STT/TTS ভালো হয়েও LLM দুর্বল বাংলা দিলে ফিচার কাজ করবে না।
5. **(নতুন)** পারফরম্যান্স মাপা: RTF, মডেল load time, peak RAM, তাপ ও ব্যাটারি, end-to-end latency (বিস্তারিত পারফরম্যান্স সেকশনে)।
6. সব ফলাফল, pin ও বাতিল-কারণ `docs/knowledge/voice/`-এ।
7. Gate-এর ফল: একটি decision memo — মডেল বাছাই, অথবা "বাংলা voice এখনো প্রস্তুত নয়"।

## আর্কিটেকচার ও integrity

Voice একটি আলাদা `com.dorai.ai.voice` module; এটি বিদ্যমান `ask()` chat pipeline-এ ঢোকে, আলাদা কোনো conversation store ছাড়া।

### Module কাঠামো

- `commonMain/.../voice`: `VoiceCatalog` (pin, size, digest), `VoiceModelRepository` (catalog, manifest, marker check, progress `StateFlow`), `VoiceModelManager` (state machine), `VoiceManifestStore` (`SettingsJson` primitive ব্যবহার করে), `SpeechToText` ও `OfflineSynthesizer` interface, `VoiceModeController` (mic → STT → chat → TTS)।
- **State machine:** `absent → downloading ⇄ paused → verifying → ready`; যেকোনো ধাপ থেকে `failed`; `failed` ও `ready` থেকে delete করলে `absent`। (মূল প্ল্যানের ডুপ্লিকেট `verifying → ready` সরানো হয়েছে।)
- `androidMain`: `AndroidSpeechToText`, `AudioRecord` capture, `RECORD_AUDIO` runtime flow, `AndroidVoiceModelDownloadWorker`। `commonMain`-এ কোনো permission নয়।
- STT ইঞ্জিন (sherpa-onnx বনাম whisper.cpp binding) Phase 0-এ ঠিক হবে। **foss flavor-এ non-free binary blob চলবে না**; jniLibs-এর APK/AAB size প্রভাব মাপতে হবে।
- LiteRT-LM STT-তে ব্যবহার হবে না: সেটি `.litertlm` নেয়, speech মডেল সেই ফরম্যাটে আসে না।
- TTS ইঞ্জিন একটি user setting: `System (default)` / `Offline — Coqui bn VITS` / `Offline — eSpeak fallback` (শেষটি শুধু foss)। বিদ্যমান rate/pitch/volume settings offline synthesizer-ও মানবে।
- Chat pipeline: STT output সাধারণ (non-hidden) USER turn হিসেবে যায়; TTS শুধু চূড়ান্ত ASSISTANT turn পড়ে।

### Storage ও integrity

- মডেল app-private `models/voice/<model-id>/`-এ; LiteRT directory থেকে আলাদা।
- `.part`-এ ডাউনলোড, streaming SHA-256, সঠিক length যাচাই, atomic rename, তারপর `.sha256` marker — marker লেখার আগে মডেল load-যোগ্য নয়।
- Load-এর সময় marker তুলনা: মিললে চলবে; না মিললে বা না থাকলে refuse করে delete/re-download-এর পথ দেখাবে; নিজে নিজে কিছু মুছবে না।
- **Resume-এর সময় digest:** process মরার পর streaming digest-এর state থাকে না। তাই resume-এ বিদ্যমান `.part` আবার hash করে digest পুনর্গঠন করতে হবে। এটি শুধু local read (\~375 MB-এ কয়েক সেকেন্ড), network নয়।
- **Resume-এর শর্ত:** সার্ভার `206` না দিলে বা `ETag`/`If-Range` মিলে না গেলে ০ থেকে শুরু; কখনো পুরো body কোনো `.part`-এ append নয়।
- প্রতিটি catalog entry-তে `sha256` ও `sizeBytes` বাধ্যতামূলক; না থাকলে unit test ফেল।
- Android worker ও foreground path **একই verification কোড** ব্যবহার করবে।
- Settings export-এ voice মডেল ও marker বাদ; export শুধু metadata।

## Android ডাউনলোড অর্কেস্ট্রেশন

`AndroidVoiceModelDownloadWorker` হবে `AndroidUpdateDownloadWorker`-এর মতো `CoroutineWorker`, তবে foreground progress notification সহ এবং resume-সক্ষম।

- **Unique work:** `dorai-voice-model-download`, policy `KEEP`। নতুন মডেল queue করতে চাইলে একই chain-এ পরের কাজ হিসেবে যোগ হবে (STT, তারপর TTS)।
- **Constraints:** ডিফল্ট `UNMETERED`; mobile-data টগল অন থাকলে `CONNECTED`। Constraints প্রতিটি enqueue-এ সেট হয়।
- **`setForeground()`:** worker নিজের progress notification-সহ ডাকবে। `ModelDownloadService` (`dataSync` FGS) পুনর্ব্যবহার করা যাবে, অথবা আলাদা `VoiceModelDownloadService`। সার্ভিসের কাজ শুধু ডাউনলোড বাঁচিয়ে রাখা ও progress দেখানো; bytes worker-ই নামায়।
- **Resume:** `doWork()`-এ `<model>.part` থাকলে `.part` আবার hash করে digest পুনর্গঠন, তারপর `Range: bytes=<len>-` সহ `If-Range` (ETag)। `206` না এলে ০ থেকে শুরু।
- **Retry:** `Result.retry()`, সর্বোচ্চ ≥ ৩ বার, exponential backoff। `429`/`5xx`-এ `Retry-After` মানা; `416` পেলে `.part` বাতিল করে ০ থেকে।
- **Host নিরাপত্তা:** শুধু HTTPS ও allowlist-ভুক্ত host। HF `resolve` URL CDN host-এ redirect করে, তাই allowlist-এ redirect-এর গন্তব্য host-ও রাখতে হবে; pinned commit ছাড়া "latest" URL চলবে না।
- **ডিস্ক পূর্ণ:** লেখার সময় `ENOSPC` পেলে `.part` রেখে স্পষ্ট বার্তা সহ `failed(storage)`।
- **Doze / Battery Saver:** কাজ পিছিয়ে গেলে UI-তে "অপেক্ষমাণ — Wi‑Fi দরকার" ধরনের সৎ অবস্থা।
- **Notification:** Pause / Cancel action; progress শতাংশ ও বাকি সাইজ।
- **পরীক্ষা:** WorkManager নিজে নয়; offset হিসাব, digest accumulation, state transition ও resume-এর ধারা pure orchestration layer-এ unit test।

## পারফরম্যান্স ডিজাইন

সবচেয়ে বড় ঝুঁকি RAM ও latency: STT, LLM ও TTS একসাথে মেমরিতে রাখা যাবে না, আর প্রতিটি ধাপ মাপা না হলে কোনো লক্ষ্য মানে নেই।

একটি voice turn-এর ক্রম: mic → VAD → STT → (STT আনলোড) → LLM → বাক্য ভাগ → TTS → স্পিকার। প্রতিটি তীরে latency জমে, তাই নিচের কৌশলগুলো ধাপ ধরে সাজানো।

### মেমরি

- **ক্রমিক লোড:** STT শেষ হলে আনলোড, তারপর LLM; TTS মডেল চালানোর সময় STT মেমরিতে থাকবে না। কখনো তিনটি একসাথে নয়।
- **RAM floor** নির্ধারণে LLM-এর পূর্ণ footprint ধরা হবে, শুধু STT মডেলের নয়। Floor না মিললে Voice Mode চালু করার সময়ই কারণসহ জানানো।
- Pre-warm: mic button চাপার মুহূর্তে STT লোড শুরু; idle ১–২ মিনিট (প্রস্তাবিত, মেপে ঠিক হবে) পরে আনলোড।

### Audio ইনপুট

- `AudioRecord`, 16 kHz mono PCM16, source `VOICE_RECOGNITION`; ডিভাইসে থাকলে noise suppressor।
- **VAD (Silero, sherpa-onnx-এ আছে):** নীরবতা ছেঁটে ফেলা ও end-of-speech-এ auto-stop। Whisper সব input ৩০ সেকেন্ডে pad করে encoder চালায়, তাই ছোট clip-ও সস্তা নয়; VAD বাকি অংশে ভুল ও দেরি কমায়।
- Buffer শুধু memory-তে; সর্বোচ্চ দৈর্ঘ্য সীমিত (Whisper-এর জন্য ৩০ সেকেন্ড)।

### Inference

- int8 মডেল; থ্রেড সংখ্যা big core অনুযায়ী ২–৪ (সব core নয়), Phase 0-এ ডিভাইস-ভিত্তিক মাপা।
- Execution provider (XNNPACK / NNAPI)-এর তুলনা Phase 0-এ; NNAPI অনেক ডিভাইসে ধীর বা অস্থির হতে পারে, তাই ডিফল্ট CPU।

### LLM ও TTS

- Voice turn-এ system prompt-এ সংক্ষিপ্ত, কথ্য-ধাঁচের উত্তর চাওয়া (markdown, তালিকা, code ছাড়া); এতে LLM ও TTS দুটোরই সময় কমে।
- **বাক্য-ভিত্তিক TTS:** উত্তর `।`, `?`, `!`-এ ভাগ; প্রথম বাক্য বাজার সময় পরেরটা synthesize। (এটি streaming STT/full-duplex নয়, তাই non-goal ভাঙে না।)
- **TTS-পূর্ব পরিষ্কার:** markdown, code block, URL, emoji সরানো; সংখ্যা বাংলা শব্দে রূপান্তর; বাংলা-ইংরেজি মিশ্র শব্দের জন্য লেখ্য-রূপ নিয়ম।
- প্রথম বাক্য পৌঁছাতে দেরি হলে "ভাবছি…" অবস্থা দেখানো, মৌন অপেক্ষা নয়।

### Latency বাজেট (লক্ষ্য Phase 0 মাপার পর ঠিক হবে)

| ধাপ | কী মাপা হবে | প্রধান উপায় |
| --- | --- | --- |
| মডেল load | cold ও warm load time | pre-warm, idle-unload |
| STT | RTF, "কথা শেষ → text" সময় | VAD, int8, থ্রেড টিউনিং |
| LLM | প্রথম token ও tokens/sec | ছোট উত্তর, বিদ্যমান LiteRT সেটিং |
| TTS | প্রথম বাক্যের synthesis সময় | বাক্য-ভিত্তিক pipeline |
| মোট | "কথা শেষ → প্রথম শব্দ বাজা" | উপরের সব; লক্ষ্য মেপে লেখা হবে |

সাথে মাপা: peak RAM, তাপ-throttling, প্রতি turn ব্যাটারি খরচ, mid-range ও flagship ডিভাইসে আলাদাভাবে।

### Audio session

প্রস্তাবিত সিদ্ধান্ত: mic চালু হলে `AUDIOFOCUS_GAIN_TRANSIENT` নিয়ে অন্য অডিও থামানো, TTS শেষে ফোকাস ছাড়া। Bluetooth headset ও phone call-এর সময় আচরণ Phase 1-এ পরীক্ষা।

## প্রাইভেসি ও নিরাপত্তা

সব অডিও ও transcript device-এর বাইরে যায় না; Voice একটি ইনপুট-পদ্ধতি, নতুন ক্ষমতা নয়।

- অডিও শুধু active turn-এর memory buffer-এ; ডিস্ক, log বা sandbox-এ কখনো লেখা হবে না।
- Transcript শুধু সাধারণ chat message হিসেবে, বিদ্যমান persistence নিয়মে; কোনো shadow transcript store নেই।
- Voice turn টেক্সট turn-এর চেয়ে বেশি tool, shell বা network অ্যাক্সেস পায় না।
- STT/TTS পুরোপুরি on-device; অডিও বা transcript কোনো পথে পাঠানো হয় না; voice কনটেন্টে telemetry নেই।
- ডাউনলোডে pinned HF commit + pinned digest + HTTPS host allowlist (redirect-গন্তব্যসহ); "latest" URL নয়।
- `RECORD_AUDIO` শুধু Voice Mode ব্যবহারের মুহূর্তে চাওয়া হয়; rationale UI দেখানো হয়; অস্বীকার করলে voice অনুপলব্ধ, text chat অপরিবর্তিত, settings-এ যাওয়ার এক-ট্যাপ পথ।
- Settings export শুধু metadata; import কখনো ডাউনলোড চালু করে না।

## ধাপ ভিত্তিক পরিকল্পনা

মূল প্ল্যানের Phase 0 ও Phase 1 পরস্পরের ওপর নির্ভর করত (on-device মাপতে harness লাগে, যা Phase 1-এ আসত); এখানে সেগুলো সমান্তরালে চলে এবং একই harness ব্যবহার করে।

1. **Phase 0 — Measurement (২–৪ সপ্তাহ, blocking):** ছোট benchmark harness (internal-only) দিয়ে eval set, STT-র CER/WER, TTS intelligibility panel, LLM-এর বাংলা মান, latency/RAM/তাপ/ব্যাটারি; লাইসেন্স sign-off (Coqui/Mimic3, `espeak-ng-data` GPL প্রভাব); `docs/knowledge/voice/` bundle। *Deliverable: একটি decision memo — মডেল বাছাই, অথবা "এখনো নয়"।*
2. **Phase 1 — Android prototype (flag, internal-only; Phase 0-র সাথে সমান্তরাল):** mic button, `AudioRecord`, VAD, STT → বিদ্যমান `ask()`, চূড়ান্ত উত্তরে বাক্য-ভিত্তিক system-TTS, `RECORD_AUDIO` flow, audio focus। auto-download নেই; ডেভেলপাররা হাতে মডেল বসায়। *Deliverable: recordable demo, দুটি ডিভাইসে crash-free।*
3. **Phase 2 — Android GA:** `VoiceCatalog`, `VoiceModelRepository`, worker + FGS + resume, settings "Voice" সেকশন (enable / delete / re-download / TTS ইঞ্জিন), সব জায়গায় বাংলা স্ট্রিং, integrity, storage gate, fresh install থেকে airplane-mode-এ পূর্ণ offline পরীক্ষা, empty-state লেখা। Phase 0 sign-off ফাইলে না থাকলে ডিফল্ট **OFF**। *Deliverable: production build-এ Voice Mode টগল।*
4. **Phase 3 — Hardening:** নিম্নমানের ডিভাইস tier, ব্যাটারি ও তাপ, AAB/APK size (flavor split বা dynamic feature delivery), golden-audio regression test (পিন বদলালে WER পুনরায় মাপা), pin-bump playbook।

**সব ধাপে:** screenshot test-এ Voice Mode UI অবস্থা যোগ; `bn` স্ট্রিং coverage PR check-এর অংশ; আচরণ বদলালে feature doc হালনাগাদ। Phase 1 shipped হয়েছে; Phase 2 (মডেল ডাউনলোড) শিপ হলে তবেই পুরো নথি "Verified" হবে।

## টেস্টিং ও QA

প্রতিটি ব্যর্থতার পথে ব্যবহারকারী একটি স্পষ্ট বার্তা ও পুনরুদ্ধারের উপায় পাবে, আর text chat সব ক্ষেত্রে অপরিবর্তিত থাকবে।

- **প্রবাহ:** fresh install → Voice Mode চালু → প্রত্যাখ্যান → text chat ঠিক; চালু → সম্মতি → ডাউনলোড → app kill → relaunch → resume → verify → usable।
- **Integrity:** corrupt ফাইল → মডেল refuse, inline error, delete/re-download পথ; নীরবে load নয়।
- **Resume কোণ:** সার্ভার `200` দিলে (Range উপেক্ষা), `416`, ETag বদল, `429`/`5xx`, নেটওয়ার্ক মাঝপথে হারানো।
- **Storage:** `sizeBytes + 25%`-এর কম জায়গা → শুরুর আগেই refuse; ডাউনলোডের মাঝে ডিস্ক পূর্ণ।
- **নেটওয়ার্ক:** Wi‑Fi-only ডিফল্ট মানা; mobile data টগল ও প্রতিবার নিশ্চিতকরণ।
- **Doze / Battery Saver:** ডাউনলোড শেষ হয় অথবা সৎভাবে পিছিয়ে দেখায়।
- **Permission:** mic অস্বীকার → rationale ও settings-পথ; text chat অক্ষত।
- **Audio:** audio focus, Bluetooth headset, ফোন কলের মাঝে আচরণ।
- **বাংলা UI:** Bengali keyboard, IME composition, TalkBack লেবেল; `bn` locale-এ কোনো untranslated English নেই।
- **ফিচার সঠিকতা:** golden বাংলা অডিও clip দিয়ে STT regression; TTS-পূর্ব পরিষ্কার (সংখ্যা, URL, markdown) unit test।
- **বিদ্যমান suite সবুজ:** `:composeApp:desktopTest`, `:screenshotTests:verifyPaparazziDebug`, `spotlessCheck`, offline chat flow।

## ঝুঁকি তালিকা

সবচেয়ে বড় দুটি ঝুঁকি হলো বাংলা ASR/LLM-এর মান এবং ফোনের RAM; দুটির জন্যই Phase 0 gate রক্ষাকবচ।

| ঝুঁকি | মাত্রা | প্রশমন |
| --- | --- | --- |
| On-device বাংলা ASR-এর মান গ্রহণযোগ্য নয় | Critical | Phase 0 gate শিপিং আটকায়; বিকল্প: "বাংলা voice এখনো প্রস্তুত নয়" |
| On-device LLM-এর বাংলা উত্তর দুর্বল | Critical | Phase 0-এ LLM মূল্যায়ন যুক্ত; দুর্বল হলে voice শিপ নয় |
| STT + LLM + TTS একসাথে RAM ছাড়িয়ে যায় | High | ক্রমিক লোড/আনলোড; RAM floor-এ LLM ধরা; Voice Mode চালুর সময় আগেই জানানো |
| End-to-end latency ব্যবহারযোগ্য নয় | High | VAD, বাক্য-ভিত্তিক TTS, ছোট উত্তর, Phase 0-এ বাজেট |
| MMS-TTS (সেরা বাংলা কণ্ঠ) CC-BY-NC | High | ডিফল্ট Coqui/Mimic3/eSpeak; লিখিত অনুমোদন ছাড়া NC নয় |
| `espeak-ng-data` / eSpeak GPL-3.0 প্রভাব | High | playStore flavor-এ আইনি পর্যালোচনা; foss-এ সীমিত |
| APK/AAB size +30–80 MB (sherpa-onnx / whisper.cpp native) | High | Phase 0-এ মাপা; flavor split বা dynamic feature delivery |
| HF CDN redirect-এ allowlist ভাঙে বা Range আচরণ বদলায় | Medium | redirect-host allowlist, Range/ETag পরীক্ষা, `200`-এ ০ থেকে শুরু |
| Mid-range ডিভাইসে তাপ-throttling ও ব্যাটারি | Medium | থ্রেড সীমা, মাপা, ছোট মডেল ফলব্যাক |
| Upstream HF-এ pin-এর নিচে bytes বদল | Low | pinned digest + exact size; LiteRT pin policy অনুসরণ |
| ব্যবহারকারী না জেনে mobile data-এ ডাউনলোড | Low | Wi‑Fi-only ডিফল্ট + প্রতি ডাউনলোডে নিশ্চিতকরণ |
| বাংলা font/shaping regression | Low | বিদ্যমান typography stack; `bn` screenshot test |

## খোলা প্রশ্ন ও সিদ্ধান্ত

মূল প্ল্যানের ছয়টি খোলা প্রশ্নের তিনটির এখানে প্রস্তাবিত সিদ্ধান্ত দেওয়া হলো; বাকিগুলোর উত্তর Phase 0 থেকে আসবে।

### প্রস্তাবিত সিদ্ধান্ত (অনুমোদন দরকার)

- **Knowledge bundle:** আলাদা `docs/knowledge/voice/`, LiteRT bundle-এর একই ফরম্যাটে। কারণ voice pin-এর বদলের ছন্দ আলাদা।
- **Settings কাঠামো:** settings-এ গ্রুপ করা "Voice" সেকশন (টগল, TTS ইঞ্জিন, delete/re-download, mobile-data অনুমতি)। composer-এ শুধু mic button।
- **Audio session:** `AUDIOFOCUS_GAIN_TRANSIENT` — mic চালু হলে অন্য অডিও থামে, TTS শেষে ফোকাস ছাড়া।

### এখনো খোলা

- [ ] Coqui-VITS (bn) ও Mimic 3 bn-এর প্রতিটি repo-র লাইসেন্স — foss ও playStore flavor-এ অনুমোদন? `espeak-ng-data`-এর GPL-3.0 প্রভাব সহ।
- [ ] IndicConformer-600M-এর গ্রহণযোগ্য quantized build আছে কি, নাকি বাদ?
- [ ] ন্যূনতম hardware tier: RAM floor (LLM সহ ধরে) ও ARMv8.2+ শর্ত।
- [ ] On-device LLM-এর বাংলা মান কোন মডেলে গ্রহণযোগ্য, নাকি voice-এর জন্য আলাদা মডেল ধরতে হবে?
- [ ] Latency লক্ষ্য কত (Phase 0-এ মাপার পর)?

### Decision log

- 2026-11-12 — পরিসর: বাংলা প্রথম, **শুধু Android**; ডাউনলোড WorkManager-এ `AndroidUpdateDownloadWorker`-এর মডেলে; integrity LiteRT `.sha256` marker pattern-এ; system TTS ডিফল্ট; মডেল বাছাই Phase 0 gate পর্যন্ত স্থগিত।
- 2026-11-12 — HF API যাচাই: official বাংলা IndicWhisper নেই; IndicConformer-600M fp32-এ ভারী; Whisper-small int8 ≈ 357 MiB; Mimic3 bn VITS ≈ 73 MB ও Coqui bn VITS ≈ 109 MiB TTS প্রার্থী; MMS-TTS bn CC-BY-NC-4.0; Indic Parler-TTS 3.75 GB ফোনের জন্য অসম্ভব।
- 2026-11-12 — Phase 1 implement: STT হিসেবে Android-এর নিজস্ব `android.speech.SpeechRecognizer` (কোনো native dependency বা ডাউনলোড মডেল নেই, APK সাইজ অপরিবর্তিত); `EXTRA_PREFER_OFFLINE` চালু, অফলাইন ভাষা মডেল না থাকলে network error-এ ব্যর্থ হয় এবং অডিও নিঃশব্দে পাঠানো হয় না; Voice Mode সেটিংসে ডিফল্ট OFF; `RECORD_AUDIO` শুধু mic বাটন চাপলে চাওয়া হয়; transcript একটি সাধারণ USER turn; চূড়ান্ত উত্তর বাক্যে ভাগ করে পড়া হয় এবং prose-এর সংখ্যা বাংলা শব্দে বলা হয়, কিন্তু identifier/version/decimal অপরিবর্তিত থাকে।

## মূল প্ল্যান থেকে পরিবর্তন

পরিসর Android-এ সীমিত করা হয়েছে, ভুল ঠিক করা হয়েছে, আর পারফরম্যান্স ও LLM-মান যুক্ত হয়েছে।

| বিষয় | মূল প্ল্যান | এই সংস্করণ |
| --- | --- | --- |
| পরিসর | Android + iOS + Desktop | শুধু Android |
| Storage buffer | ২০% ও ২৫% দুই জায়গায় | ২৫% |
| Work policy | `REPLACE` | `KEEP` |
| Mobile-data constraint | `METERED` | `CONNECTED` |
| Resume + SHA-256 | digest state-এর কথা নেই | `.part` আবার hash + `If-Range`/ETag |
| Host allowlist | HF host | redirect-গন্তব্য CDN host-ও |
| Mimic3 লাইসেন্স | Apache-class | `espeak-ng-data` GPL-3.0 যাচাই |
| Whisper-small লাইসেন্স | MIT ও "no tag" দুটোই | Whisper MIT; repo tag যাচাই বাকি |
| STT অগ্রাধিকার | Whisper-small প্রথম | streaming zipformer-bn আগে মাপা |
| Eval set | ~৫০ বাক্য | ২০০টি BengVoice utterance, NFC normalization, CER+WER |
| LLM-এর বাংলা মান | নেই | Phase 0 gate-এ |
| Phase 0/1 | ক্রমিক, পরস্পর-নির্ভর | সমান্তরাল, একই harness |
| TTS | শুধু চূড়ান্ত উত্তর, এক টানে | বাক্য-ভিত্তিক pipeline |
| মেমরি | RAM check-এ শুধু মডেল | LLM সহ; ক্রমিক load/unload |
| Audio input | `AudioRecord` উল্লেখমাত্র | VAD, auto-stop, 16 kHz mono |
| TTS fallback | STT ready হলেও TTS-এর অপেক্ষা | system TTS দিয়ে সাথে সাথে চালু |
| Desktop TTS লাইন | "java.util fallback" ও "system TTS only" | নথির বাইরে |
| State machine | `verifying → ready` ডুপ্লিকেট | `paused` যুক্ত, ডুপ্লিকেট সরানো |
