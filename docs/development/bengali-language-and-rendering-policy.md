# বাংলা ভাষা, Unicode ও Chat Rendering নীতিমালা

- **প্রকল্প:** Dorai AI
- **প্রযোজ্যতা:** সম্পূর্ণ অ্যাপ, সব প্ল্যাটফর্ম, AI/Agent prompt, chat response, UI, tool output, documentation এবং development workflow
- **অবস্থা:** বাধ্যতামূলক প্রকল্প নীতিমালা
- **ক্যানোনিক্যাল ভাষা কোড:** `bn` (বর্তমান `AppLanguage` ও Compose Resources বিন্যাসের সঙ্গে সামঞ্জস্যপূর্ণ)

এই নথি Dorai AI-তে বাংলা ভাষার সঠিক ব্যবহার, সংরক্ষণ, প্রদর্শন, ইনপুট, AI response generation এবং rendering-এর authoritative specification। সব Coding Agent, AI Agent, contributor ও reviewer-কে প্রাসঙ্গিক পরিবর্তনের আগে এই নথি পড়তে এবং অনুসরণ করতে হবে।

> **মূলনীতি:** বাংলা কেবল অনূদিত UI নয়; এটি end-to-end product capability। বাংলা অক্ষর, কার-ফলা, যুক্তাক্ষর, মাত্রা, বিরামচিহ্ন, emoji, user input এবং AI-generated response কোনো স্তরেই বিকৃত, হারানো, বিচ্ছিন্ন বা ভুলভাবে render হওয়া চলবে না।

## 1. নীতিমালার অগ্রাধিকার ও ভাষা নির্বাচন

1. এই নথি বাংলা ভাষা, Unicode, localization, input এবং rendering বিষয়ে `AGENTS.md`-এর সাধারণ নির্দেশনার পরিপূরক এবং ওই বিষয়গুলোতে বিস্তারিত authoritative source।
2. Repository-wide safety, security, architecture ও Git policy সবসময় বহাল থাকবে। এই নথি কোনো নিরাপত্তা বা data-preservation নিয়ম শিথিল করে না।
3. অ্যাপের নির্বাচিত language preference UI chrome, navigation, settings, labels, dialogs, notifications, accessibility labels এবং app-authored status text-এর ভাষা নির্ধারণ করবে।
4. Assistant-এর conversational response language নির্ধারণে বর্তমান product behavior ও system prompt অনুসরণ করতে হবে: ব্যবহারকারীর স্পষ্ট ভাষা-পছন্দ/অনুরোধ সর্বোচ্চ অগ্রাধিকার পাবে; তা না থাকলে conversation-এর প্রধান ভাষা অনুসরণ করবে। Bengali mode সক্রিয় থাকলে সাধারণ assistant response বাংলায় হবে, যদি ব্যবহারকারী স্পষ্টভাবে অন্য ভাষা না চান।
5. UI locale এবং conversation content language আলাদা concern। একটি পরিবর্তন করলে অন্যটি অনিচ্ছাকৃতভাবে বদলানো যাবে না।
6. User-provided text, quoted content, source code, logs, filenames, URLs, API identifiers, model names, brand names এবং protocol values অনুবাদ বা transliterate করা যাবে না—যদি না ব্যবহারকারী স্পষ্টভাবে অনুবাদ চান।
7. বাংলা থেকে ইংরেজি বা ইংরেজি থেকে বাংলা ভাষায় fallback নীরবে করা যাবে না। অনুবাদ অনুপস্থিত হলে resource parity check-এ তা ধরা পড়তে হবে।

## 2. বাংলা লেখার মান ও ভাষাশৈলী

- প্রাকৃতিক, প্রমিত, পাঠযোগ্য ও প্রসঙ্গ-উপযোগী বাংলা ব্যবহার করতে হবে। যান্ত্রিক শব্দে-শব্দে অনুবাদ, অস্বাভাবিক বাক্যগঠন এবং অপ্রয়োজনীয় ইংরেজি মিশ্রণ এড়িয়ে চলতে হবে।
- সাধারণ UI ও ব্যাখ্যায় বাংলা শব্দ অগ্রাধিকার পাবে; প্রচলিত technical term-এর নির্ভুল বাংলা না থাকলে canonical English term রাখা যাবে এবং প্রয়োজনে প্রথম ব্যবহারে বাংলা ব্যাখ্যা দিতে হবে।
- একই feature, action, status ও concept-এর জন্য পুরো অ্যাপে একই পরিভাষা ব্যবহার করতে হবে। নতুন পরিভাষা যোগের আগে বিদ্যমান resource ও glossary খুঁজতে হবে।
- অপ্রয়োজনীয় বাংলা transliteration নয়। উদাহরণ: `সেটিংস`-এর মতো প্রচলিত UI শব্দ গ্রহণযোগ্য; কিন্তু `কনফিগারেশন`-এর বদলে সহজ ও স্বাভাবিক বাংলা থাকলে সেটি অগ্রাধিকার পাবে।
- বাংলা punctuation, spacing এবং quotation style ধারাবাহিক রাখতে হবে। দাঁড়ি (`।`), কমা ও অন্যান্য বিরামচিহ্ন প্রসঙ্গ অনুযায়ী ব্যবহার করতে হবে; punctuation-এর আগে অপ্রয়োজনীয় space দেওয়া যাবে না।
- বাংলা বাক্যের মধ্যে code, command, path, key, identifier, URL, version এবং literal value অপরিবর্তিত রাখতে হবে।
- সংখ্যার ক্ষেত্রে context ও বিদ্যমান product convention অনুসরণ করতে হবে। prose-এ বাংলা সংখ্যা ব্যবহার করলে একই screen/feature-এ ধারাবাহিকতা রাখতে হবে; code, logs, timestamps, IDs, versions, financial/API values ও machine-readable output-এর digit format পরিবর্তন করা যাবে না।
- User-এর tone, formality এবং প্রয়োজন অনুযায়ী সম্বোধন বজায় রাখতে হবে; অকারণে অতিরিক্ত ঘনিষ্ঠতা, অসম্মানজনক সম্বোধন বা অপ্রয়োজনীয় দীর্ঘ ভূমিকা নয়।

## 3. Localization coverage — সম্পূর্ণ অ্যাপ

বাংলা localization কেবল Settings বা প্রধান navigation-এ সীমাবদ্ধ নয়। অন্তত নিচের সব surface audit করতে হবে:

- App launch, onboarding, navigation, tabs, menus, toolbar, title ও subtitle।
- Settings, preferences, language selection, theme, privacy, permissions ও account flows।
- Chat composer, placeholder, send/stop/retry controls, attachment picker, message actions এবং empty states।
- Assistant response, streaming response, Markdown, code blocks, citations, links, tables, lists, math ও rich content blocks।
- Agent activity timeline, reasoning/status summaries, task planning, tool call labels, tool results, approvals, confirmation cards এবং failure/retry states।
- File browser, editor, diff/review, terminal, shell output, search, downloads, uploads এবং workspace operations।
- Provider/model selection, API configuration, connection state, usage/limits, billing-related labels ও errors।
- Notifications, snackbars, dialogs, sheets, tooltips, validation messages, accessibility descriptions ও keyboard hints।
- Desktop menus/window chrome, Android-specific UI, iOS-specific UI এবং Web/Wasm surface—যে target-এ feature সক্রিয়।
- Agent-generated documents বা user-facing content তৈরি/preview করার সব flow।

নতুন user-facing string যোগ করলে একই change-set-এ সব supported locale resource update করতে হবে। বাংলা mode-এ কোনো English-only label, hardcoded message বা fallback surface দেখা গেলে সেটি localization defect হিসেবে বিবেচিত হবে—যদি না সেটি ইচ্ছাকৃত technical identifier হয়।

## 4. Compose Resources ও translation integrity

1. Shared UI string-এর জন্য বর্তমান canonical Compose Resources location ব্যবহার করতে হবে: `composeApp/src/commonMain/composeResources/`। Android platform-specific resource এবং shared Compose Resources গুলিয়ে ফেলা যাবে না।
2. বিদ্যমান default locale এবং Bengali locale directory/file convention অনুসরণ করতে হবে; নতুন locale code বা directory layout নিজে থেকে উদ্ভাবন করা যাবে না।
3. UI-তে hardcoded user-facing string যোগ করা যাবে না, যদি surrounding feature resource-based হয়। Generated resource output হাতে edit করা যাবে না।
4. Resource key-এর নাম অর্থবহ, স্থিতিশীল এবং context-aware হতে হবে। Key rename/remove করার আগে সব usage ও locale file search করতে হবে।
5. সব supported locale-এ key parity বজায় রাখতে হবে। Missing, extra, duplicate key, malformed XML এবং placeholder mismatch CI/local validation-এ fail করতে হবে।
6. `%1$s`, `%1$d`, named placeholder, ICU/plural argument বা অন্য formatting token-এর সংখ্যা, type, order এবং semantic meaning locale-গুলোর মধ্যে একই রাখতে হবে। বাংলা বাক্যের grammar অনুযায়ী placeholder-এর অবস্থান বদলানো যায়, কিন্তু token হারানো বা type বদলানো যাবে না।
7. Singular/plural, zero/one/other এবং quantity-sensitive message-এর ক্ষেত্রে Compose Resources-এর সমর্থিত plural mechanism ব্যবহার করতে হবে; string concatenation দিয়ে বাক্য জোড়া লাগিয়ে grammar ভাঙা যাবে না।
8. Bengali translation review-এ শুধু key উপস্থিতি নয়, grammar, context, truncation, placeholder rendering এবং বাস্তব screen-এ পাঠযোগ্যতাও যাচাই করতে হবে।
9. নতুন feature-এর resource coverage test বা বিদ্যমান localization validation script update করতে হবে, যাতে ভবিষ্যতে parity regression ধরা পড়ে।
10. **Canonical form:** resource file-এর প্রতিটি string NFC form-এ লেখা হবে। বাংলা য় (U+09DF) আর য+nukta (U+09AF U+09BC) canonically সমতুল্য, কিন্তু Bengali nukta composition Unicode-তে বাদ, তাই NFC-এ canonical spelling হলো decomposed রূপ। একই resource file-এ দুই spelling মিশিয়ে রাখা যাবে না — এটি LocaleParityTest-এর পাশাপাশি BengaliCanonicalFormTest দ্বারা CI-তে fail করে।
11. **Stale locale backlog:** default (English) resource-এর key set বহিরের ৫৩টি locale এখনো পুরো নয়; runtime-এ missing key-এর জন্য English fallback কাজ করে, তাই এটি build failure নয়। তবে কোনো locale ইতিমধ্যে যে key পেয়েছে সেটা হারালে সেটি regression — LocaleCoverageBaselineTest (baseline: scripts/locale-coverage-baseline.json) তা fail করে। অনুবাদের কাজের তালিকা scripts/locale-coverage.ps1 দিয়ে বের করা যায়।

## 5. Unicode ও UTF-8 data integrity

- Source, resource, JSON, Markdown, prompt, test fixture, database payload, IPC/message payload এবং generated user-facing text UTF-8-compatible হতে হবে। Repository-এর `.editorconfig` ও `.gitattributes`-এর encoding/line-ending convention মানতে হবে।
- বাংলা text-কে ANSI, Windows-1252, locale-dependent legacy encoding বা lossy conversion-এ রূপান্তর করা নিষিদ্ধ।
- Read/write API, subprocess, terminal, file import/export, network boundary এবং tool protocol-এ encoding explicit করতে হবে; platform default encoding-এর ওপর নির্ভর করা যাবে না।
- `�`, `???`, mojibake (যেমন `à¦...`), missing glyph box, অপ্রত্যাশিত replacement character বা ভাঙা UTF-8 দেখা গেলে সেটি data-integrity bug; শুধু font/style issue ধরে নেওয়া যাবে না।
- বাংলা text-এর code point sequence অকারণে normalize, strip, reorder বা sanitize করা যাবে না। বিশেষ করে `NFKC`-এর মতো compatibility normalization user content-এর ওপর প্রয়োগ করা যাবে না। NFC normalization প্রয়োজন হলে scope, equivalence এবং round-trip behavior test করে তবেই প্রয়োগ করতে হবে।
- Combining mark, vowel sign, virama/hasanta (`্`), nukta, ZWJ/ZWNJ, variation selector এবং emoji modifier-কে সাধারণ punctuation বা removable control character ধরে বাদ দেওয়া যাবে না।
- Text length, cursor movement, truncation, deletion ও selection-এর ক্ষেত্রে UTF-16 code unit, Unicode code point এবং grapheme cluster-এর পার্থক্য বুঝে কাজ করতে হবে। ব্যবহারকারীর দৃশ্যমান অক্ষর/cluster মাঝখান থেকে কাটা যাবে না।
- Supplementary-plane code point ও emoji-সহ সম্পূর্ণ Unicode range সঠিকভাবে preserve করতে হবে; এক `Char` মানেই এক Unicode character—এমন অনুমান করা যাবে না।
- Log sanitization বা security filtering-এর সময় বাংলা text-এর বৈধ code point মুছে ফেলা যাবে না। Sanitization অবশ্যই field/context-specific হতে হবে।

## 6. Bengali input ও IME composition

Android, Desktop, iOS এবং Web—প্রতিটি সক্রিয় input target-এ সংশ্লিষ্ট platform-এর native IME composition lifecycle সম্মান করতে হবে।

- Composing/pre-edit text এবং committed text আলাদা state হিসেবে বিবেচনা করতে হবে। IME composition চলাকালে intermediate text-কে shell, model, database বা remote tool-এ committed input হিসেবে পাঠানো যাবে না।
- IME composition replace/update/commit/cancel করতে পারবে; application intermediate composition text duplicate করবে না বা আগেভাগে consume করবে না।
- Composition commit হওয়ার পর committed text ঠিক একবার পাঠাতে হবে। Stale composing range/state পরবর্তী input-এ থেকে যাওয়া চলবে না।
- Cursor, selection, backspace, delete, paste, autocorrect, suggestion এবং keyboard navigation বাংলা যুক্তাক্ষর/কার/ফলা ও emoji cluster ভাঙবে না।
- Android `InputConnection`/`TextInput`-এর composing contract এবং অন্য platform-এর native text-input API-র lifecycle অক্ষুণ্ণ রাখতে হবে। Platform-specific workaround করলে regression test আবশ্যক।
- Terminal input-এ Bengali/Indic composition-এর intermediate text echo বা shell execution ঘটানো যাবে না। Committed Unicode text যথাযথ code point/UTF-8 byte sequence-এ পৌঁছাতে হবে।
- Copy/paste এবং selection-এর round trip-এ মূল text অপরিবর্তিত থাকতে হবে।

## 7. Chat response generation ও language policy

- Chat system prompt, model-specific instruction, fallback prompt, retry prompt, tool-result summarizer এবং agent subtask prompt-এ language behavior সামঞ্জস্যপূর্ণ রাখতে হবে। একটি prompt বাংলা চাইলে অন্য prompt যেন অনিচ্ছাকৃতভাবে English-only output বাধ্য না করে।
- Bengali mode সক্রিয় থাকলে assistant-এর সাধারণ ব্যাখ্যা, progress update, clarification, error explanation, summary এবং final answer বাংলায় হবে—যদি ব্যবহারকারী স্পষ্টভাবে অন্য ভাষা না চান।
- User ইংরেজিতে code, log বা technical identifier পাঠালেও Bengali response mode থাকলে ব্যাখ্যা বাংলায় হবে; code/identifier অপরিবর্তিত থাকবে।
- Assistant-এর উত্তর অপ্রয়োজনীয়ভাবে বাংলা-ইংরেজি মিশ্রিত হবে না। প্রচলিত technical term, API name, product name এবং code token প্রয়োজনমতো ইংরেজিতে রাখা যাবে।
- Tool output-এ structured machine data এবং localized user-facing explanation আলাদা রাখতে হবে। `error`, `status`, `message`, `messageBengali`-এর মতো field থাকলে কোনটি authoritative machine value এবং কোনটি display text—তা স্পষ্ট করতে হবে; একই তথ্য দুইবার দেখানো যাবে না।
- Tool failure, permission denial, empty result, timeout, confirmation, retry এবং success path—সবগুলোর বাংলা display message দিতে হবে। শুধু success path অনুবাদ করে error path English রাখা যাবে না।
- Agent-generated plan, activity label, tool description ও completion summary user-facing হলে নির্বাচিত language policy মানতে হবে। Internal protocol, schema, command এবং tool identifier অনুবাদ করা যাবে না।
- JSON-only, XML-only, `dorai-ui` বা অন্য structured-output mode থাকলে format contract অক্ষুণ্ণ রেখে string value-গুলো উপযুক্ত ভাষায় দিতে হবে। JSON key, enum, schema field, component type বা machine-readable constant অনুবাদ করা যাবে না।
- Model response language mismatch হলে UI layer-এ অন্ধভাবে character substitution/translation দিয়ে ঠিক করার চেষ্টা করা যাবে না। Prompt construction, conversation context, model routing বা response validation-এর সঠিক স্তরে defect সমাধান করতে হবে।

## 8. Chat Markdown ও rich response renderer

Chat renderer-এর কাজ হলো model/user content-এর অর্থ ও text অক্ষুণ্ণ রেখে সঠিক visual structure দেখানো। Bengali rendering-এর জন্য আলাদা fragile parser বা destructive preprocessing তৈরি করা যাবে না।

### 8.1 Parsing ও source preservation

- Markdown parser, Dorai UI block parser এবং streaming parser-এর input/output-এ মূল Unicode text অক্ষুণ্ণ রাখতে হবে।
- Markdown syntax শনাক্ত করতে parser/structured representation ব্যবহার করতে হবে; বাংলা অক্ষর, punctuation বা whitespace-কে regex দিয়ে নির্বিচারে rewrite করা যাবে না।
- Headings, paragraphs, emphasis, inline code, fenced code, links, lists, nested lists, blockquotes, tables, horizontal rules, math এবং app-specific rich UI block সঠিকভাবে parse/render করতে হবে।
- Inline code ও fenced code block-এর content verbatim রাখতে হবে; syntax highlighting যেন source text, Bengali comment, string literal বা copy result বদলে না দেয়।
- Link label বাংলায় হতে পারে; URL ও destination হুবহু preserve করতে হবে। Unsafe URL scheme block করতে হবে, কিন্তু বৈধ বাংলা URL/path display ভাঙা যাবে না।
- User/assistant message-এর original raw content copy/export/history-তে সংরক্ষণ করতে হবে। Rendered text থেকে source reconstruct করার চেষ্টা করে data loss ঘটানো যাবে না।

### 8.2 Streaming ও partial content

- Token-by-token streaming চলাকালে UTF-8 decoding, surrogate pair এবং grapheme boundary ভাঙা যাবে না। Network chunk boundary-তে আসা অসম্পূর্ণ byte/code unit পরের chunk না আসা পর্যন্ত নিরাপদে buffer করতে হবে।
- Streaming Markdown parser অসম্পূর্ণ fence, link, emphasis বা table syntax-কে সাময়িক plain text হিসেবে দেখালেও পরবর্তী chunk-এ স্থিতিশীলভাবে reparse করতে পারতে হবে; content duplicate বা drop করা যাবে না।
- Composing/streaming update-এ অকারণে পুরো message-এর state reset করে scroll jump, cursor jump বা selection loss ঘটানো যাবে না।
- Final response এবং streaming-এর পর final rendered document একই source-এর জন্য একই semantic content দেখাবে।

### 8.3 Typography ও layout

- Bengali glyph-এর উপযুক্ত shaping, conjunct formation, vowel sign placement, mark positioning এবং line metrics সমর্থন করে এমন font/fallback stack ব্যবহার করতে হবে। বিদ্যমান `DoraiFonts`/`DoraiTypography` design system অনুসরণ করতে হবে।
- Font selection-এ platform fallback-এর বাস্তবতা যাচাই করতে হবে। Noto Sans Bengali বা platform-এর উপযুক্ত Bengali-capable fallback ব্যবহার করা যেতে পারে; কোনো নির্দিষ্ট font file/dependency যোগের আগে licensing, bundle size, platform support ও approval যাচাই করতে হবে।
- Bengali glyph-এর ascender/descender ও combining mark clip হওয়া চলবে না। Line height, baseline, paragraph spacing, button height, chip height এবং vertical alignment বাংলা text দিয়ে যাচাই করতে হবে।
- Text wrapping শব্দ/cluster-এর মাঝখানে ভাঙা যাবে না। Narrow screen, large font scale, long unbroken token, mixed Bengali/Latin, URL এবং code block-এর জন্য intentional overflow/wrapping behavior থাকতে হবে।
- RTL layout প্রয়োজন ধরে নেওয়া যাবে না; বাংলা সাধারণত left-to-right script। তবে mixed-direction content, URL, code, punctuation ও bidi control থাকলে Unicode Bidirectional Algorithm-এর আচরণ পরীক্ষা করতে হবে।
- Ellipsis/truncation হলে grapheme-safe boundary ব্যবহার করতে হবে এবং গুরুত্বপূর্ণ action/error text অদৃশ্য করা যাবে না।
- Light/Dark theme, high contrast, font scaling ও accessibility mode-এ glyph contrast এবং readability যাচাই করতে হবে।

### 8.4 Interaction ও accessibility

- Selection, copy, share, text search, find-in-message, accessibility text extraction এবং screen reader output-এ বাংলা text অক্ষুণ্ণ থাকতে হবে।
- Screen reader label ও semantics localization অনুসরণ করবে; শুধু visual text বাংলা করে content description English রাখা যাবে না।
- Interactive Markdown/rich blocks keyboard ও accessibility navigation-এ ব্যবহারযোগ্য হতে হবে।
- Code block copy করলে formatting policy অনুযায়ী code-এর আসল content কপি হবে; UI label বা syntax highlight markup কপি হবে না।

## 9. Terminal, process output, files ও network boundary

- Shell/guest/container process-এ locale ও encoding explicit করতে হবে। Linux guest-এ UTF-8 locale available কিনা যাচাই করে `LANG`/`LC_ALL`-এর উপযুক্ত মান নির্ধারণ করতে হবে; locale অনুপস্থিত হলে সেটি নীরবে সফল ধরে নেওয়া যাবে না।
- Android Proot, Desktop process, subprocess stdout/stderr, file reader/writer এবং terminal emulator-এ UTF-8 decoding/encoding explicit হতে হবে।
- Terminal cell width নির্ধারণে Bengali grapheme, combining mark ও East Asian width assumptions সতর্কভাবে সামলাতে হবে; code point count-কে display column ধরে নেওয়া যাবে না।
- File import/export, Markdown preview, diff, search index, clipboard এবং archive path-এ Bengali filename/content round-trip test করতে হবে।
- HTTP/API/JSON/WebSocket/IPC payload UTF-8 ও protocol contract অনুযায়ী decode/encode করতে হবে। Double-encoding বা lossy fallback নিষিদ্ধ।
- Logs/debug output-এ বাংলা text inspectable থাকতে হবে, কিন্তু secrets/credentials log করা যাবে না।

## 10. Documentation, prompts ও agent instruction

- User-facing documentation এবং in-app help selected locale policy অনুসরণ করবে। Developer documentation-এর ভাষা repository convention অনুযায়ী হতে পারে, তবে বাংলা ভাষা support-এর নিয়ম এই নথির সঙ্গে সামঞ্জস্যপূর্ণ হবে।
- Agent prompt-এ Bengali mode, language precedence, technical identifier preservation, Unicode integrity এবং renderer constraints স্পষ্ট করতে হবে। Prompt পরিবর্তন করলে সব relevant prompt builder, model-specific prompt ও tool prompt পরীক্ষা করতে হবে।
- Agent-কে বাংলা text সম্পাদনার সময় PowerShell/shell encoding, heredoc, patch tool এবং editor encoding সতর্কভাবে ব্যবহার করতে হবে। UTF-8 explicit না হলে non-ASCII content write করার আগে tool behavior যাচাই করতে হবে।
- কোনো Agent বাংলা string-কে ইংরেজি placeholder দিয়ে সাময়িক বদলে রেখে কাজ শেষ বলতে পারবে না।
- Agent output-এ malformed Bengali দেখা গেলে সেটিকে cosmetic typo হিসেবে উপেক্ষা করা যাবে না; source, transport, storage, prompt এবং rendering boundary ধরে root cause খুঁজতে হবে।

## 11. Testing requirements ও regression matrix

বাংলা-সম্পর্কিত পরিবর্তনে সংশ্লিষ্ট স্তরের test যোগ/আপডেট করতে হবে। অন্তত নিচের representative corpus বিবেচনা করতে হবে:

- সাধারণ বাংলা: `আমি বাংলায় লিখছি।`
- কার/মাত্রা: `কী, কি, নীড়, শ্রেণি, প্রয়োজনীয়`
- যুক্তাক্ষর/ফলা: `ক্ষ, জ্ঞ, ত্র, শ্র, ক্র, প্র, গ্র, স্ত্র, র্য`
- হসন্ত/যৌগিক রূপ: `বাংলাদেশ, স্বাধীনতা, রাষ্ট্র, কার্যক্রম`
- যতিচিহ্ন ও mixed text: `বাংলা (Bangla) — API v2.1: /path/to/file`
- Emoji ও supplementary code point: `বাংলা 😀 👩‍💻 ❤️`
- ZWJ/ZWNJ/variation selector এবং combining sequence যেখানে প্রাসঙ্গিক।
- Long paragraph, nested Markdown, Bengali table, inline/fenced code, Bengali link label এবং streamed partial Markdown।
- Paste, composition update/commit/cancel, cursor movement, backspace, selection, copy এবং terminal forwarding।

প্রাসঙ্গিক validation:

1. Locale resource XML parse, duplicate-key detection, key parity, placeholder/plural parity।
2. Bengali UI text smoke test—সব প্রধান screen, dialog, menu, tool confirmation, error ও empty state।
3. Chat renderer unit/UI/screenshot test—Markdown block ও plain text উভয় ক্ষেত্রে Bengali glyph shaping, wrapping, clipping, selection এবং copy।
4. Streaming chunk-boundary test—UTF-8 byte, surrogate pair ও Markdown delimiter ভিন্ন chunk-এ এলে output অক্ষুণ্ণ থাকা।
5. IME regression test—composition replacement/commit/cancel, Bengali conjunct/diacritic এবং emoji।
6. Terminal/process UTF-8 round-trip test—বাংলা input থেকে guest/process output পর্যন্ত।
7. Android/Desktop/iOS/Web-এর প্রাসঙ্গিক target-এ compile/test; target অনুপলব্ধ হলে স্পষ্টভাবে উল্লেখ করতে হবে।
8. UI typography পরিবর্তনে Paparazzi/screenshot regression verification আবশ্যক; baseline update কেবল review করা intentional visual change-এর জন্য।
9. `git diff --check`, formatting/static analysis এবং repository validation script চালাতে হবে।

Test কেবল English string বা ASCII fixture দিয়ে pass করিয়ে বাংলা support দাবি করা যাবে না। Screenshot baseline update করে unexplained regression আড়াল করা নিষিদ্ধ।

## 12. Agent-এর বাধ্যতামূলক কাজের পদ্ধতি

বাংলা, localization, prompt, chat renderer, typography, input, terminal, file encoding বা Unicode-সংশ্লিষ্ট task পেলে Agent-কে:

1. `AGENTS.md` এবং এই সম্পূর্ণ policy পড়তে হবে।
2. বিদ্যমান localization guide, relevant prompt builder, renderer, typography, platform input এবং test fixture inspect করতে হবে।
3. কাজের আগে `git status --short --branch` দেখে user/other-agent পরিবর্তন শনাক্ত ও সংরক্ষণ করতে হবে।
4. কোন layer-এ defect তা চিহ্নিত করতে হবে—prompt, resource, parser, font/shaping, input/IME, serialization, process encoding নাকি storage।
5. Minimal targeted fix করতে হবে; বাংলা text-এর ওপর broad regex replacement বা unverified normalization করা যাবে না।
6. Regression test যোগ করতে হবে এবং বাংলা corpus দিয়ে যাচাই করতে হবে।
7. Source diff-এ malformed character, unexpected English fallback এবং unrelated file change পরীক্ষা করতে হবে।
8. Test result সত্যভাবে জানাতে হবে; skipped/unavailable test-কে passed বলা যাবে না।
9. অন্য Agent বা user-এর uncommitted পরিবর্তন থাকলে সেগুলো stage/commit/overwrite করা যাবে না।
10. Encoding corruption, unexplained text loss, conflicting agent edit বা destructive operation-এর ঝুঁকি দেখা দিলে থেমে user approval নিতে হবে।

## 13. Definition of Done

বাংলা-সংশ্লিষ্ট কোনো কাজ complete বলা যাবে কেবল যখন:

- নির্বাচিত ভাষা অনুযায়ী সংশ্লিষ্ট UI ও user-facing surface সঠিক ভাষায় আছে;
- Assistant response language policy মানে এবং অপ্রয়োজনীয় language mixing নেই;
- বাংলা glyph, যুক্তাক্ষর, কার-ফলা, emoji ও mixed-script content সঠিকভাবে render হয়;
- Input, IME composition, selection, copy/paste ও terminal round trip অক্ষুণ্ণ;
- Markdown/rich renderer plain, streaming ও final response-এ content drop/duplicate/corrupt করে না;
- Locale key/placeholder/plural parity বজায় আছে;
- সংশ্লিষ্ট automated tests ও screenshot checks সফল, অথবা অসম্পূর্ণ validation স্পষ্টভাবে নথিবদ্ধ;
- diff-এ unrelated user/agent changes নেই এবং repository safety policy মানা হয়েছে।

## 14. সংশ্লিষ্ট implementation reference

Implementation পরিবর্তনের আগে বর্তমান source inspect করতে হবে; নিচের path-গুলো orientation, exhaustive contract নয়:

- `composeApp/src/commonMain/composeResources/` — shared localized resources
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/AppSettings.kt` — app language preference
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/ChatSystemPromptBuilder.kt` — chat/system prompt construction
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/RemoteDataRepository.kt` — remote/agent prompt and response integration
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/markdown/` — Markdown/rich response parsing and rendering
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/` — chat UI and message rendering
- `composeApp/src/commonMain/kotlin/com/dorai/ai/theme/tokens/DoraiTypography.kt` — typography and font family policy
- `composeApp/src/androidMain/kotlin/com/dorai/ai/workspace/TerminalInputView.kt` — Android terminal IME/input integration
- `agents/resources-and-localization.md` — Compose resource workflow
- `agents/resources-localization-specialist.md` — localization specialist agent guidance

এই path-গুলোর implementation বদলাতে পারে। কাজের আগে `git grep`/IDE search দিয়ে বর্তমান authoritative implementation খুঁজতে হবে।

---

**নীতি-পরিবর্তন:** এই নথির নিয়ম পরিবর্তন করতে হলে সংশ্লিষ্ট Agent workflow, prompt, renderer ও test coverage একসঙ্গে পর্যালোচনা করতে হবে। শুধু documentation edit করে বাস্তব implementation বা validation gap উপেক্ষা করা যাবে না।
