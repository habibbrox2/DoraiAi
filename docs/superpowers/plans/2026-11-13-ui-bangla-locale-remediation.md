# DORAI AI — UI/UX ও বাংলা/ইংরেজি Language System Remediation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`. Task এক এক করে execute করো; এই ফাইলের checkbox list আপডেট রাখো। প্রতিটি phase শেষে phase-sized commit, এবং পরের phase শুরু করার আগে তার verification command সবুজ হতে হবে।

**Goal:** 2026-11-13 read-only audit-এ পাওয়া সব Critical/Major/Minor সমস্যা (বাংলা mojibake, ভাষা pipeline, ~৬০ hardcoded English string, resource parity gate-এর অনুপস্থিতি, renderer/low-level defects, UI/UX accessibility ও consistency) ঠিক করা — কোনো behavioral regression ছাড়াই।

**Architecture:** Phase-ভিত্তিক remediation — data integrity → language pipeline → localization sweep → renderer → a11y/UX → theming/dead code/fake data → responsive → tests/docs/gates। প্রতিটি phase আলাদাভাবে merge/verify-যোগ্য; phase-গুলো পরস্পরের ওপর কম নির্ভরশীল (P1 → P3 hardcoded-string task গুলো P2-এর parity gate-এর ওপর নির্ভর করে; P6-এর fake-data task গুলো P5-এর home UI change-এর সাথে conflict করতে পারে, তাই ক্রমে রাখা হয়েছে)।

**Tech Stack:** Kotlin Multiplatform 2.4.21, Compose Multiplatform 1.12.2, kotlinx-serialization, Compose Resources, Paparazzi screenshotTests, desktopTest/commonTest।

**Spec:** কোনো committed spec নেই — এই plan 2026-11-13 in-session audit report থেকে derive করা (চ্যাটে deliver হয়েছিল)। Audit-এর file:line reference-গুলো working tree-এর (branch `feat/voice-mode-foundation`, voice-mode uncommitted কাজসহ) হিসেবে সঠিক।

## Global Constraints

- **Uncommitted voice-mode কাজ (`M Platform*.kt`, `M AppModule.kt`, `M AppSettings.kt`, `M RemoteDataRepository.kt`, `M DataRepository.kt`, `M gradle/libs.versions.toml`, `M iosApp/Configuration/Config.xcconfig`, `M GeneralSettings.kt`, `M Settings*.kt`, `M values*/strings.xml` (voice keys), `?? voice/` ইত্যাদি) কোনো অবস্থাতেই revert/overwrite/stage করা যাবে না।** এই plan-এর task গুলো সেই ফাইলগুলো স্পর্শ করলে diff-এ শুধু সেই task-এর hunk থাকতে হবে।
- Voice-mode কাজ আগে merge হলে এই branch-কে `origin/main`-এ rebase করতে হবে; না হলে এই branch `feat/voice-mode-foundation`-এর HEAD থেকেই কাটা হবে (P0 দেখো)।
- নতুন dependency বা `gradle/libs.versions.toml` পরিবর্তন **ছাড়াই** কাজ শেষ করতে হবে। Grapheme-breakage-এর জন্য expect/actual + platform BreakIterator/NSString API ব্যবহার করো (নতুন লাইব্রেরি নয়)।
- `.github/workflows/test.yml` modify করা যাবে না (repo policy অনুযায়ী read-only)। Locale parity gate `desktopTest`/`commonTest` + `scripts/validate-locales.ps1` দিয়ে enforce হবে — দুটোই CI-তে আগে থেকেই চলে।
- Bengali policy (`docs/development/bengali-language-and-rendering-policy.md`) mandatory: কোনো broad regex replacement বা unverified normalization বাংলা text-এ নয়; renderer পরিবর্তনে Bengali corpus test বাধ্যতামূলক।
- `spotlessCheck` প্রতি commit-এর আগে; theme/token বা shared component বদলালে `:screenshotTests:verifyPaparazziDebug` একই change-set-এ; intentional visual change-এ golden re-record একই PR-এ।
- User-facing পরিবর্তনে `CHANGELOG.md`-তে unreleased section-এ entry; chat/settings feature docs (`docs/features/chat.md`, `settings-export-import.md` ইত্যাদি) বদলালে "Last verified" তারিখ আপডেট।
- বাংলা string edit করতে হলে UTF-8 explicit রাখতে হবে; editor-এ mojibake (`à¦…`) দেখা দিলে সাথে সাথে থামা — root cause খোঁজার আগে file save করা যাবে না।
- সব task-এর শেষে `git status --short` দিয়ে unrelated change আছে কিনা দেখা বাধ্যতামূলক।
- Commit message: short, imperative, conventional prefix (`fix:`, `feat:`, `test:`, `docs:`, `refactor:`)।

## Review Focus

1. **Mojibake ফিরে আসা** — resource file-এ double-encoded UTF-8 আবার ঢুকে পড়লে heartbeat prompt contract ভাঙা যায় → P1-এ lint test যা পুরো `composeResources/` scan করে; `ValuesBnMojibakeTest` (desktopTest)।
2. **Parity gate false-green** — শুধু `values/` আর `values-bn/` check করলে বাকি ৫৩ locale-এর drift ধরা পড়ে না → gate সমস্ত `values-*` directory cover করবে এবং placeholder/plural parity ও verify করবে।
3. **Android-এ language switch no-op** — `Locale.setDefault()` দিয়ে Compose Resources re-localize হয় না → P2-তে platform actual fix + explicit "verify on device" step (device না থাকলে PASS দাবি করা যাবে না)।
4. **ভাষা policy bypass** — soul-এ "DORAI AI" থাকলে rules হারিয়ে যাওয়া bug → P2-তে `contains("DORAI AI")` heuristic সরানোর test: custom soul containing "DORAI AI" হলেও Bengali rules inject হয়।
5. **Grapheme-boundary কাটা** — truncation আবার UTF-16 index-এ হলে Bengali cluster ভাঙবে → P4-এর `GraphemeUtils`-এ Bengali conjunct + ZWJ emoji fixture test বাধ্যতামূলক।

---

## File Structure

### নতুন ফাইল
- `composeApp/src/commonMain/kotlin/com/dorai/ai/text/GraphemeUtils.kt` — grapheme-safe take/truncate (expect/actual)।
- `composeApp/src/androidMain/kotlin/com/dorai/ai/text/GraphemeUtils.android.kt` — `android.icu.text.BreakIterator`।
- `composeApp/src/desktopMain/kotlin/com/dorai/ai/text/GraphemeUtils.desktop.kt` — `java.text.BreakIterator`।
- `composeApp/src/iosMain/kotlin/com/dorai/ai/text/GraphemeUtils.ios.kt` — `NSString.enumerateSubstringsInRange(NSStringEnumerationByComposedCharacterSequences)`।
- `composeApp/src/wasmJsMain/kotlin/com/dorai/ai/text/GraphemeUtils.wasmJs.kt` — code-point fallback (documented limitation)।
- `composeApp/src/desktopTest/kotlin/com/dorai/ai/resources/LocaleParityTest.kt` — XML parse, key parity, duplicate keys, placeholder/plural parity, mojibake scan।
- `scripts/validate-locales.ps1` — local/CI-runnable parity + mojibake validator (desktopTest থেকে regex logic share করবে না; standalone, XML regex-based)।
- `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/markdown/BengaliCorpusTest.kt` — renderer Bengali corpus (streaming, conjuncts, emoji, mixed script)।
- (প্রয়োজনে) `composeApp/src/commonTest/kotlin/com/dorai/ai/data/PromptLanguagePolicyTest.kt` — prompt builder language-injection tests।

### পরিবর্তিত ফাইল (প্রধান)
- `composeApp/src/commonMain/composeResources/values-bn/strings.xml` — mojibake repair, terminology consistency, punctuation/numeral fixes।
- `composeApp/src/commonMain/composeResources/values/strings.xml` — প্রয়োজনে `dynamic_ui_form_submission` wire-up; নতুন key-এর master source।
- `values-bn/strings.xml` + `values/strings.xml` + নতুন string ব্যবহারকারী সব UI ফাইল (P3-এ file group অনুযায়ী)।
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/RemoteDataRepository.kt` — `getActiveSystemPrompt` language-rule injection; bailout prompt; compaction prompt comment।
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/ChatSystemPromptBuilder.kt` — language-policy section।
- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/HeartbeatPromptBuilder.kt`, `HeartbeatManager.kt` (default prompt/sentinel comment), `TaskScheduler.kt` (HEARTBEAT_OK check context)।
- `composeApp/src/jvmShared/kotlin/com/dorai/ai/ApplyAppLanguageJvm.kt` (ও প্রযোজ্য platform actual) — Android `Configuration`-based locale।
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ToolEventGrouper.kt` — Json.parseToJsonElement; `DynamicUiModel(title=...)` resource।
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/DoraiChatScreenConnected.kt`, `composables/ChatInputUtils.kt`, `data/TaskScheduler.kt` — grapheme-safe truncation sites।
- `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/markdown/InlineTokenizer.kt` (emphasis regex), `MarkdownInlineRenderer.kt` (scheme allow-list), `MarkdownTextRenderer.kt` (sentence-end punctuation)।
- UI/UX a11y: `DoraiSnackbar.kt`, `DoraiModeSelector.kt`, `DoraiStructureComponents.kt`, `DoraiDynamicUiBlock.kt`, `adaptive/DoraiAdaptiveScaffold.kt`, `SettingsNavigation.kt` (pattern reference), `DoraiChatScreen.kt`, `EmptyState.kt`, `MessageActionRow.kt`, `ConversationRail.kt`, `ChatHistorySheet.kt`, `ChatCommandPalette.kt`, icon-only button sites।
- Theme/tokens: `ui/Theme.kt` (chat typography line-heights), `ui/components/DoraiChip.kt`, `DoraiSlider.kt`, `DoraiAvatar.kt`, `DoraiSnackbar.kt`, `SemanticColors` pipeline (`DoraiSemanticColors`, `SettingsStatusColors`, `OperationalComponents`, `SettingsStatusBanner`), `components/v2/DoraiToolCard.kt`, `DoraiShellBlock.kt`, `DoraiApprovalActions.kt`, workspace/dashboard token files, `settings/*`, `home/DoraiHomeScreen.kt`, `App.kt`, `DoraiChatScreenConnected.kt` (fake state removal)।
- Dead code: `ui/components/DoraiDialog.kt`, `chat/composables/SmallIconButton.kt`, `components/v2/DoraiComponentsPreview.kt` (grep-verify আগে)।
- Docs: `CHANGELOG.md`, `docs/features/chat.md`, প্রয়োজনে `docs/features/settings-*.md`, `agents/resources-and-localization.md` (gate উল্লেখ)।

---

## Phase 0 — Branch ও baseline

- [ ] **Step 1:** `git fetch origin`; `git status --short --branch` দিয়ে working tree verify (voice-mode changes intact আছে কিনা)।
- [ ] **Step 2:** Branch কাট: `git checkout -b fix/ui-bangla-remediation` (current HEAD থেকে — voice-mode কাজের ভিত্তি হারানো যাবে না)। Voice-mode আগে merge হলে plan অনুযায়ী rebase।
- [ ] **Step 3:** Baseline: `./gradlew :composeApp:desktopTest` — PASS কিনা লিখে রাখো (pre-existing failure পেলে note করো, ঠিক করার চেষ্টা এই plan-এ নয়)।
- [ ] **Step 4:** Audit reference ফাইল হিসেবে চ্যাটের report এই session-এ available আছে ধরে নেওয়া; নতুন করে file লেখা লাগে না।

---

## Phase 1 — বাংলা data integrity ও locale parity gate (Critical C-1, L-3, L-4-minor)

### Task 1.1: Mojibake repair — ৪টি string

**Files:** Modify `composeApp/src/commonMain/composeResources/values-bn/strings.xml:126,141,382,388`

**Interfaces:** Consumes — audit C-1; Produces — সঠিক বাংলায় `হৃৎস্পন্দন` ও অক্ষত `HEARTBEAT_OK` sentinel; পরে P2 Task 2.3 এটি ব্যবহার করে।

- [ ] Step 1: `values-bn/strings.xml` UTF-8 হিসেবে read করে ৪টি string-এ `à¦¹à§ƒà§Ž...` bytes খুঁজে বের করো (hex dump confirm করো)।
- [ ] Step 2: Replace — ঠিক `হৃৎস্পন্দন` বসবে; `settings_heartbeat_default_prompt`-এ `[হার্টবিট] ... HEARTBEAT_OK` marker verbatim ASCII-তে রাখতে হবে (en-এর structure-এর সাথে মিলিয়ে)।
- [ ] Step 3: `git diff composeResources/values-bn/strings.xml` — শুধু ৪টি string-ই বদলেছে; অন্য কোনো hunk নেই।
- [ ] Step 4: Verification — `Select-String -Path values-bn/strings.xml -Pattern 'à¦' -Encoding utf8` → 0 matches (comment সহ)।

### Task 1.2: Missing bn keys + unused resource wire-up

**Files:** Modify `values-bn/strings.xml`, `values/strings.xml`, `ui/chat/ToolEventGrouper.kt:203`

- [ ] Step 1: bn-তে যোগ: `agent_fallback_name` = `এজেন্ট`, `dynamic_ui_form_submission` = `ফর্ম সাবমিশন` (en: "Agent" / "Form Submission" — অর্থ ও length matching রেখে)।
- [ ] Step 2: `ToolEventGrouper.kt:203`-এ hardcoded `"Form Submission"` → `Res.string.dynamic_ui_form_submission` (stringResource context-এ না হলে `getString` equivalent pattern — file-এর existing pattern follow করো)।
- [ ] Step 3: Desktop test বা resource-gen verify: `./gradlew :composeApp:generateComposeResClass` (অথবা desktopTest compile) — generated accessor resolve হয়।

### Task 1.3: Locale parity + mojibake gate — `LocaleParityTest.kt`

**Files:** Create `composeApp/src/desktopTest/kotlin/com/dorai/ai/resources/LocaleParityTest.kt`

**Interfaces:** Produces — `localeResourceParityCheck(): List<String>` (empty list = pass) যা desktopTest-এ fail করে; পরে `scripts/validate-locales.ps1` একই নিয়ম enforce করে।

- [ ] Step 1: Test লেখো: (a) প্রতিটি `composeApp/src/commonMain/composeResources/values*/strings.xml` XML-parse হবে (well-formed); (b) duplicate `name=` → fail; (c) প্রতিটি locale-এর key set == `values/`-এর key set **অথবা** test-এ explicit allowlist — **Decision required:** বাকি ৫৩ locale-এ 800 missing key আছে। Maintainer-কে জিজ্ঞেস না করে gate fail করা যাবে না। তাই এই task প্রথম ধাপে **en↔bn pair parity** (en == bn exact, placeholder + plural সহ) enforce করবে; বাকি locale-গুলো `stalenessReport()` (report-only, fail নয়) হিসেবে print করবে, CHANGELOG-এ limitation note। 53-locale propagation আলাদা সিদ্ধান্ত (Review Focus-2)।
- [ ] Step 2: Test লেখো: placeholder (`%1$s`/`%1$d`) multiset + order en vs bn প্রতি key-তে সমান।
- [ ] Step 3: Test লেখো: mojibake signature scan — regex `à¦|à§|â€”|ï¿½|Ã` যেকোনো values*/**.xml-এ (comment সহ) → fail; plus `\xEF\xBF\xBD` (U+FFFD) scan।
- [ ] Step 4: Test লেখো: bn-তে plurals (`relative_time_*`) এক-একটি `one`/`other` সমান key সহ আছে; en-bn placeholder একই।
- [ ] Step 5: `./gradlew :composeApp:desktopTest --tests "*LocaleParityTest*"` → সব PASS।

### Task 1.4: Validator script `validate-locales.ps1`

**Files:** Create `scripts/validate-locales.ps1`; Modify `scripts/validate-changes.ps1` (composeResources change-এ script call যোগ — file-এর existing profile pattern-এ)

- [ ] Step 1: Script: XML regex-based key extraction, en↔bn exact parity + mojibake scan; exit code 1 on violation; 53-locale staleness → warning only। বাংলা content PowerShell-এ **byte/UTF-8-aware** রাখতে কেবল ASCII patterns (`à¦` lead bytes) দিয়েই mojibake ধরবে; বাংলা rendering-এ নির্ভর করবে না।
- [ ] Step 2: `pwsh scripts/validate-locales.ps1` → exit 0; values-bn-এ test violation দিয়ে exit 1 confirm (temporarily, revert)।
- [ ] Step 3: `validate-changes.ps1`-এ `Feature` profile-এ যোগ করো।

### Task 1.5: Bengali terminology ও punctuation consistency sweep

**Files:** Modify `values-bn/strings.xml` only

- [ ] Step 1: Glossary টেবিল (এই ফাইলে fixed): memory → **স্মৃতি** (bn:89-92-এর `মেমরি` গুলো বদলাও); theme → **থিম** (bn:552-566 `থীম` → `থিম`); service → **পরিষেবা** (bn:809-810,1200,1334-1335 `সার্ভিস` → `পরিষেবা`; bn:60 `সেবাসমূহ` → `পরিষেবাসমূহ`); provider → **প্রোভাইডার** রাখবে (৮× ব্যবহৃত, transliteration convention) কিন্তু bn:37 native টা প্রোভাইডারে মেলাবে অথবা উল্টো — **একবার সিদ্ধান্ত নিয়ে সবজায়গা একই করো।** Workspace → **ওয়ার্কস্পেস** (bn:456,462 → ওয়ার্কস্পেস)। GitHub → **GitHub** (bn:865,867,902,904,905,908… `গিটহাব` → `GitHub`)।
- [ ] Step 2: Punctuation: `settings_status_checking`-এর ASCII `...` → `…`; `sidebar_tagline` ও `empty_state_subtitle`-এর ASCII `.` → `।`; `%1$d টি` → `%1$dটি` (bn:1331-1332, দুই form-এ `one` variant-ও ঠিক — Bangla numeral policy অনুযায়ী ASCII digit + ASCII-space consistency, en-এর সাথে `dash_projects_count_one` মিল রাখো)।
- [ ] Step 3: `repository` (bn:892) → `রিপোজিটরি`; `palette_hint`-এ `conversation` → `কথোপকথন`; `home_terminal`-এর `Terminal` → `টার্মিনাল`; `palette_open_settings`-এ `Settings` → `সেটিংস` (এবং "Open" verb en-এর সাথে মিলিয়ে: en intent অনুযায়ী `সেটিংস খুলুন`)।
- [ ] Step 4: `sp_posting_key` → `পোস্টিং কী`; `v2_api_header` → `API হেডার`; skill titles (`v2_skill_code_review_title`, `v2_git_commit_title`, `v2_compose_tips_title`) → Bengali বর্ণনা সহ অর্থপূর্ণ শিরোনাম (`কোড রিভিউ`, `Git কমিট মেসেজ`, `Compose UI টিপস`)।
- [ ] Step 5: সংখ্যা/ellipsis sweep শুধুমাত্র ঐ ফাইলে; file-wide regex নয় — প্রতিটি change targeted, `git diff` দিয়ে eyeball।
- [ ] Step 6: Verification: Task 1.3-এর test আবার run → PASS; `git diff --stat` → শুধু strings.xml।

### Task 1.6: `য়` normalization — **approval-gated**

U+09AF+U+09BC (364×) vs U+09DF (20×) — দুটোই valid, NFC একই না। broad rewrite policy-নিষিদ্ধ, তাই:

- [ ] Step 1: Maintainer-কে জিজ্ঞেস: single representation বেছে নেবে কিনা (recommendation: U+09AF+U+09BC — বেশি ব্যবহৃত + IME output)।
- [ ] Step 2: Approval পেলে শুধু `values-bn/strings.xml`-এ `য়`-normalization; deny-তে এই task skip, CHANGELOG-এ known-inconsistency note।

**Phase 1 gate:** `desktopTest --tests "*LocaleParityTest*"` PASS; `pwsh scripts/validate-locales.ps1` exit 0; commit `fix(i18n): repair bengali mojibake and enforce locale parity gate`।

---

## Phase 2 — Language pipeline (L-1, L-2, L-6 TTS, L-7 heartbeat sentinel)

### Task 2.1: `AppLanguage` → system prompt থ্রেড করা

**Files:** Modify `data/RemoteDataRepository.kt` (`getActiveSystemPrompt` ~2549), `data/ChatSystemPromptBuilder.kt`

**Interfaces:** Consumes — `AppSettings.language: AppLanguage`; Produces — `getActiveSystemPrompt(): String` যেখানে Bengali mode-এ সুস্পষ্ট Bengali-response rules departure-বিহীন inject হয় (soul-content নির্বিশেষে)।

- [ ] Step 1: Test (commonTest): `PromptLanguagePolicyTest` — fake soul `"... DORAI AI ..."` থাকলেও language=bn-তে `getActiveSystemPrompt()`-এ নিচের অংশ থাকে: `"respond in Bengali ... unless the user explicitly asks for another language"` (actual wording English constants-এ, কিন্তু test asserts presence via stable marker constant, যেমন `const val LANGUAGE_RULES_MARKER`)।
- [ ] Step 2: `getActiveSystemPrompt()`-এ `if (!baseSoul.contains("DORAI AI"))` heuristic সরাও। নতুন logic: `val languageRule = if (appSettings.language == AppLanguage.BENGALI) BengaliLanguageRules else EnglishLanguageRules` — সবসময় inject। ব্যতিক্রম: ব্যবহারকারীর explicit অন্য ভাষা request — নিয়ম বাক্যে explicitly লেখা (policy §7), code-এ detect করব না।
- [ ] Step 3: `ChatSystemPromptBuilder.kt`-এর prompt-এও একই একলাইন pointer যোগ করো (`LANGUAGE POLICY: follow the injected language rules; never switch silently`) যাতে কেউ builder আলাদাভাবে call করলেও contract হারায় না।
- [ ] Step 4: Test — English mode-এও rules inject হয় ("respond in English unless the user explicitly asks otherwise") — symmetrical behavior।
- [ ] Step 5: `./gradlew :composeApp:desktopTest --tests "*PromptLanguagePolicyTest*"` PASS।

### Task 2.2: Heartbeat / compaction / bailout prompts-এ language policy

**Files:** Modify `data/HeartbeatPromptBuilder.kt`, `data/HeartbeatManager.kt`, `data/RemoteDataRepository.kt` (bailout ~175, compaction ~2217), `data/TaskScheduler.kt` (context comment)

- [ ] Step 1: `HeartbeatPromptBuilder`-এ language line যোগ: `"Report in <active-app-language>; keep HEARTBEAT_OK sentinel exactly as ASCII 'HEARTBEAT_OK'."` (ভাষা runtime-এ resolve — hardcoded Bengali নয়)।
- [ ] Step 2: `HeartbeatManager.DEFAULT_HEARTBEAT_PROMPT`-এ sentinel ASCII রাখা হচ্ছে তা comment-এ document করো; `TaskScheduler.kt:197` check বদলাবে না (sentinel contract অক্ষত)।
- [ ] Step 3: bailout (`RemoteDataRepository.kt:175-178`) ও compaction (`:2217`) prompt-এ একই একলাইন language pointer যোগ (builder-এর marker constant reference করে)।
- [ ] Step 4: Test: heartbeat prompt builder output-এ সক্রিয় ভাষা নাম থাকে (bn → "Bengali", en → "English")।
- [ ] Step 5: `./gradlew :composeApp:desktopTest` সম্পূর্ণ suite PASS (heartbeat-related existing tests সহ)।

### Task 2.3: Android (ও platform) locale apply ঠিক করা

**Files:** Modify `jvmShared/ApplyAppLanguageJvm.kt` বা প্রযোজ্য android actual; প্রয়োজনে `App.kt` (LaunchedEffect ordering)

- [ ] Step 1: Audit confirm করেছিলাম: শুধু `Locale.setDefault()` — Compose Resources Android-এ config-driven। Fix: Android actual-এ `Resources.getSystem().configuration.setLocale(...)` + `createConfigurationContext` নয় — Compose-এর জন্য standard হলো **activity-level `Configuration` + `Activity.recreate()`** অথবা AppCompatDelegate locale। AppCompat আছে কিনা libs.versions confirm করো; নতুন dependency ছাড়া `Activity` থেকেই করো: `attachBaseContext`-এ `createConfigurationContext` — এটাই cleanest, কোনো dep লাগে না।
- [ ] Step 2: `App.kt`-এ language change-এ `LocalConfiguration`-driven recompose trigger: একটি `key(appLanguage)` wrapper অথবা recreate। Implementation detail executor ঠিক করবে, constraint: first-frame-এ ভুল ভাষা দেখা যাবে না (attachBaseContext সমাধান করলে automatic)।
- [ ] Step 3: Desktop/iOS/wasm actual-গুলোতে একই treatment সম্ভব কিনা দেখো; iOS-এ full relaunch লাগলে code-এ explicit comment + known-limitation doc entry — **PASS clamp করবে না**।
- [ ] Step 4 (verify পরে): on-device Android verify (language switch → সব প্রধান screen Bengali; restart-এ persists) — **এই step এই plan-এর gate নয়; user কে জানানো হবে verify বাকি**।
- [ ] Step 5: `./gradlew :androidApp:assembleFossDebug` PASS।

### Task 2.4: Read-aloud TTS-এ ভাষা কোড

**Files:** Modify `ui/chat/DoraiChatScreenConnected.kt:277-305`

- [ ] Step 1: `textToSpeech.say(...)`-এ `appSettings.language` থেকে languageCode pass (`bn`/`en`) — TTS wrapper-এ `languageCode` param আগে থেকে আছে (voice-mode code-এ দেখা গেছে); read-aloud path-এ wire করো।
- [ ] Step 2: Desktop/Android compile; on-device verify পরে (P2 gate নয়)।

### Task 2.5: Bengali sentence-end punctuation (TTS)

**Files:** Modify `ui/markdown/MarkdownTextRenderer.kt:34-38` (`ensureSentenceEnd`)

- [ ] Step 1: Test: Bengali-ending line-এ append punctuation `।`; ASCII-ending line-এ `.`; mixed line-এ last visible char-এর script অনুযায়ী।
- [ ] Step 2: Implement (last non-space char Bengali range U+0980–U+09FF হলে `।`)।
- [ ] Step 3: `MarkdownToSpeakableTextTest` PASS।

**Phase 2 gate:** Prompt tests PASS; desktopTest full PASS; assembleFossDebug PASS; commit `fix(i18n): thread app language into agent prompts and locale apply`।

---

## Phase 3 — Hardcoded English → resources (L-4)

প্রতিটি task একই pattern: literals → `stringResource(Res.string.*)`; নতুন key `values/strings.xml`-এ, Bengali `values-bn/strings.xml`-এ; বাকি ৫৩ locale **এই plan-এ touch হবে না** (Task 1.3 staleness warning-ই থাকবে)।

### Task 3.1: Settings chrome — সবচেয়ে বেশি user-visible
**Files:** `SettingsDetailPane.kt:205-251` (Version + N configured services + 6 tab title/desc + `"SETTINGS / "` prefix), `SettingsOverview.kt:34-63` (পুরো Overview card), `SettingsScreen.kt:359` (top bar)

- [ ] Step 1: 20+ নতুন key (`settings_version_format` `%1$s`-style নয় — `Version %1$s`, `settings_configured_services_format`, `settings_tab_*_title`, `settings_tab_*_description`, `settings_detail_section_prefix`, `settings_overview_*`, `settings_title`) — en + bn দুই জায়গায়, bn natural Bengali (`Version` → `সংস্করণ %1$s`, `SETTINGS / ` → `সেটিংস / `)।
- [ ] Step 2: Compose কোড replace; `SettingsScreen` top bar `Res.string.settings_title`।
- [ ] Step 3: desktopTest compile + Paparazzi settings golden প্রভাবিত কিনা `verifyPaparazziDebug` — diff হলে review-করে re-record।

### Task 3.2: Language picker নিজেই (GeneralSettings)
**Files:** `GeneralSettings.kt:153-159`
- [ ] Step 1: `"Language"` → existing key (`settings_language` থাকলে) না থাকলে নতুন; stale `"App display language (default: Bengali)"` → `settings_language_description` নতুন key, en+bn; bn copy: `অ্যাপের প্রদর্শন ভাষা (ডিফল্ট: বাংলা)`।

### Task 3.3: Sandbox surfaces
**Files:** `SandboxTabsContent.kt:120-243` (heading, Ready/Failed/Installing/Needs setup badges, body text, Retry setup button), `SandboxFileBrowserScreen.kt:587` (Find/Replace toggle), `SandboxFileBrowserScreen.kt:399-400` (Folder/File contentDescription)
- [ ] Step 1: ~10 নতুন key en+bn; badge labels `OperationalComponents`-এর সাথে share হবে Task 3.5-এ — order: 3.5 আগে করলে এখানে শুধু সেগুলো consume করো। **Decision:** 3.5 আগে, 3.3 পরে।

### Task 3.4: Chat tool chrome
**Files:** `ToolExecutionCard.kt:150,173,406,430,438,455,478`, `ToolMessage.kt:226`, `ConversationRail.kt:76,137`, `ChatHistorySheet.kt:238-249` (মাসের নাম), `EmptyState.kt:171-193`, `PremiumChatBackdrop.kt:151`
- [ ] Step 1: Month names: `java.text.DateFormatSymbols(Locale("bn"))` — commonMain-এ নয়, expect/actual নয় — Compose Resources-এ 12 month-abbrev key (`month_jan`…`month_dec`) বসিয়ে সহজ common সমাধান নাও।
- [ ] Step 2: বাকি literals → নতুন keys; compact `contentDescription` a11y strings সহ (`a11y_collapse`/`a11y_expand` — expand/collapse সঠিকভাবে translate; en "Collapse"/"Expand")।
- [ ] Step 3: `"Running..."` → `…` (U+2026) সহ bn `চলছে…`।
- [ ] Step 4: Desktop tests + Paparazzi chat goldens verify।

### Task 3.5: Shared status badges / components
**Files:** `OperationalComponents.kt:198-207`, `SettingsStatusBanner.kt`, `DoraiToolCard.kt:270,363,535`, `DoraiShellBlock.kt:68`, `WorkspaceDesignTokens.kt:161-165`, `DashboardScreen.kt:662`, `SettingsConfirmDialog.kt:33` (default `dismissLabel`)
- [ ] Step 1: `StatusBadge` signature: `StatusBadge(label: String, ...)` → call-site-এ resource string pass; default hardcoded labels সরাও (breaking change নয় — internal component)।
- [ ] Step 2: `WorkspaceStep.label()` → `@StringRes`/enum-key approach: enum-এ `labelKey` রাখুন, UI-তে `stringResource`।
- [ ] Step 3: `DashboardScreen.kt:662` → নতুন `dashboard_empty_no_saved_work` key।
- [ ] Step 4: `SettingsConfirmDialog` default `dismissLabel` = null → required param বানান (call-site-গুলো compile check-এ ধরা পড়বে — refactor safe)।

### Task 3.6: Settings sections ও misc
**Files:** `McpSection.kt:279-281,380`, `SkillsSection.kt:183,366-368`, `SplinterlandsComposables.kt:139,239` + `SplinterlandsViewModel.kt:141`, `ExportImportSection.kt:217,277`, `ServicesSettings.kt:370`, `SettingsDetailPane` "Version" prefix (3.1-এ done)
- [ ] Step 1: প্রতিটির literal → key; `e.message ?: "Login failed"` → `stringResource(Res.string.login_failed)` VM-এ stringResource call করা যাবে না — VM-এ error `Res.string` ID/enum return করিয়ে UI layer-এ localize (file-এর existing error pattern follow করো)।
- [ ] Step 2: `services_sponsor_subtitle` (`habibbrox2 · project maintainer`) → key; brand `habibbrox2` অপরিবর্তিত।

### Task 3.7: Parity ও verify
- [ ] Step 1: Task 1.3/1.4 আবার run — নতুন key en↔bn parity PASS।
- [ ] Step 2: `git grep -n 'Text("'` — নতুন hardcoded user-facing English অবশিষ্ট নেই (technical identifiers বাদে); ফলাফল review করে list বানান।
- [ ] Step 3: `verifyPaparazziDebug`; `desktopTest`; commit `fix(i18n): localize hardcoded UI strings across settings, chat, sandbox`।

**Phase 3 gate:** সব listed file-এ 0 unjustified literal; bn parity PASS; Paparazzi PASS; CHANGELOG entry (`### Fixed`-এ: Bengali UI hardcoded English strings localized); commit।

---

## Phase 4 — Renderer ও low-level correctness (L-6 renderer, L-7)

### Task 4.1: ToolEventGrouper — regex JSON → kotlinx.serialization
**Files:** `ui/chat/ToolEventGrouper.kt:23-49`; Test: `commonTest/.../chat/ToolEventGrouperTest.kt` (নতুন বা existing)
- [ ] Step 1: Test (RED): stdout-এ `\"` escaped quote থাকলে পুরো output থাকে; `\u0986` (Bengali আ) decode হয়ে `আ` renders; `\"success\" : false` (space সহ) detect হয়; non-JSON input-এ graceful fallback raw string।
- [ ] Step 2: `parseToolExecutionOutput`-এ `Json { ignoreUnknownKeys = true; isLenient = true }.parseToJsonElement(raw)` → `jsonObject["stdout"]?.jsonPrimitive?.content`; parse fail-এ পুরোনো regex path fallback নয় — **raw text verbatim return** (masking নয়)।
- [ ] Step 3: Tests GREEN; `desktopTest`।

### Task 4.2: Grapheme-safe truncation — `GraphemeUtils`
**Files:** Create expect/actual ফাইলগুলো (File Structure দেখো); Modify `DoraiChatScreenConnected.kt:149`, `ChatInputUtils.kt:19-29`, `TaskScheduler.kt:276-285`
- [ ] Step 1: API: `expect fun graphemeSafeTake(text: String, maxGraphemes: Int): String` + `expect fun graphemeSafeTruncateWithEllipsis(text: String, maxGraphemes: Int): String` (ellipsis U+2026 সহ, boundary কখনো cluster ভাঙে না)।
- [ ] Step 2: Test corpus (commonTest): Bengali `আমি বাংলায় লিখছি` (combining marks), `ক্ষ/স্ত্র/র্য` conjuncts, `বাংলা 😀 👩‍💻`, ZWNJ sequence, mixed `বাংলা API v2.1`; 90-boundary জবরদস্তি হলেও half-cluster নেই; ellipsis এক grapheme হিসেবে count।
- [ ] Step 3: Android actual: `BreakIterator.getCharacterInstance()`; Desktop: `java.text.BreakIterator.getCharacterInstance()`; iOS: `NSString enumerateSubstrings...ComposedCharacterSequences`; Wasm: codePoint scan + documented limitation।
- [ ] Step 4: তিনটি call site replace (behavior: same visible length intent, correct boundary)।
- [ ] Step 5: `commonTest` + `desktopTest` GREEN; Android compile।

### Task 4.3: Markdown link scheme allow-list
**Files:** `ui/markdown/MarkdownInlineRenderer.kt:65-75`
- [ ] Step 1: Test: `https://…`, `http://…`, `mailto:…` → clickable; `javascript:`, `intent:`, `data:`, `file:` → link text plain (no Url annotation); Bengali path সহ `https://…` intact; scheme case-insensitive।
- [ ] Step 2: Implement: href lowercase-scheme check; allow-list match না করলে `LinkAnnotation.Url` দেবেন না — text style plain + semantics-এ role রাখুন (যদি `Role.Link` দেওয়া যায়) না হলে plain text। Unsafe হলে user-facing error নয় — শুধু non-clickable।
- [ ] Step 3: Tests PASS।

### Task 4.4: Underscore emphasis — Bengali letters word-char
**Files:** `ui/markdown/InlineTokenizer.kt:43-46`
- [ ] Step 1: Test: `ফাইল_১ … ফাইল_২` (underscore বাংলা শব্দের মাঝে) emphasis নয়; `_গাঢ়_` emphasis; English intra-word `snake_case_var` emphasis নয় (আগের behavior)।
- [ ] Step 2: Regex-এর `[A-Za-z0-9_]` → `[\p{L}\p{N}_]` (Unicode letter/number) — Java/Kotlin regex-এ সমর্থিত; inline comment-এ কারণ লিখুন (Bengali letters word constituent)।
- [ ] Step 3: StreamingToleranceTest ও InlineParsingTest পুরোনোগুলো GREEN (regression)।

### Task 4.5: Bengali corpus renderer test — `BengaliCorpusTest.kt`
**Files:** Create `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/markdown/BengaliCorpusTest.kt`
- [ ] Step 1: Cases: (a) conjuncts `ক্ষ জ্ঞ ন্ত্র স্ত্র র্য` inline; (b) `কী, নীড়, শ্রেণি, প্রয়োজনীয়`; (c) mixed `বাংলা (Bangla) — API v2.1: /path/to/file`; (d) emoji `বাংলা 😀 👩‍💻 ❤️`; (e) ZWJ/ZWNJ (`👩‍💻` intact round-trip); (f) nested Markdown + Bengali link label + Bengali table; (g) partial-stream simulation: `StreamingToleranceTest`-এর pattern-এ Bengali chunks split at arbitrary byte/char boundaries — parser exception ছাড়া re-parse stable; content never dropped/duplicated; (h) fenced code with Bengali comment verbatim।
- [ ] Step 2: সব test GREEN — ব্যর্থ হলে renderer-এ real bug পাওয়া গেছে → bug fix একই task-এ (scope-creep ছাড়া, minimal)।

**Phase 4 gate:** `desktopTest` + `commonTest` GREEN; commit `fix(markdown): grapheme-safe truncation, link scheme allow-list, unicode-aware emphasis`।

---

## Phase 5 — Accessibility ও interaction (U-1, Critical C-3 snackbar, U-5 touch targets)

### Task 5.1: DoraiSnackbar — TalkBack-যোগ্য
**Files:** `ui/components/DoraiSnackbar.kt:74-124`; Test: commonTest/desktopTest semantics test (repo-তে Compose UI-test infra থাকলে; না থাকলে Paparazzi + manual checklist)
- [ ] Step 1: `Text("✕")` → `Icon(Icons…close)` + `stringResource(Res.string.a11y_dismiss)`; `semantics { liveRegion = LiveRegionMode.Polite }` snackbar container-এ; variant colors `LocalDesignColorTokens.current.success/warning/error/info` (Theme.kt globals নয়)।
- [ ] Step 2: Light-theme contrast: text সব variante-এ token-based on* রঙ; inspect by hand + screenshot。
- [ ] Step 3: `verifyPaparazziDebug` (snackbar goldens মিলিয়ে re-record যদি intentional)।

### Task 5.2: Selection semantics — `selectable` migration
**Files:** `DoraiModeSelector.kt:103`, `DoraiStructureComponents.kt:79,116`, `DoraiDynamicUiBlock.kt:135`, `adaptive/DoraiAdaptiveScaffold.kt:226,351,492`
- [ ] Step 1: `clickable(role = Role.RadioButton)` → `Modifier.selectable(selected = …, onClick = …, role = Role.RadioButton)`; Tab-গুলো → `selectable(..., role = Role.Tab)`; AdaptiveScaffold bottom/rail/drawer-এ `selected = currentDestination == item` যোগ; `indication = null` সরাও (ripple রাখো)।
- [ ] Step 2: TalkBack check behavior: প্রতিটি site-এ selected state announce হয় (manual বা ui-test)।

### Task 5.3: Role-less clickables ও headings
**Files:** `SettingsListItem.kt:43`, `DoraiHomeScreen.kt:598,211-216`, `WorkspaceScreen.kt:1220`, `WorkspaceProjectsExplorer.kt:426`, `WorkspaceGitPanel.kt:265`, `SandboxFileBrowserScreen.kt:392`, `ChatCommandPalette.kt:239`, `DoraiUiRenderer.kt:1598,1648`, `DoraiChatScreen.kt:704-729`, `ui/adaptive/*` titles
- [ ] Step 1: প্রতিটি clickable row-এ `role = Role.Button` (বা Context অনুযায়ী) + merged semantics; list/detail rows-এ `Role.ListItem` যদি উপযুক্ত।
- [ ] Step 2: Screen titles (Home, Settings category, Dashboard, Chat welcome, Sandbox heading)-এ `semantics { heading() }` — pattern `SandboxTabsContent.kt:120` থেকে নাও।
- [ ] Step 3: `ChatCommandPalette`: selected index-এ `semantics { selected = true }` + list items `Role.Button`।
- [ ] Step 4: `DoraiChatScreen.kt:1570,1785` — empty-string contentDescription fallback → null-safe (`sentAtCd` null হলে semantics নেই)।

### Task 5.4: 48dp touch targets
**Files:** `DoraiHomeScreen.kt:475` (chip 36→48), `DoraiChatScreen.kt:618` (New Chat 44→48), `DoraiUiRenderer.kt:681` (checkbox 24 visual → 48 touch `minimumInteractiveComponentSize`), `:1598` (tab pill 32→min 48), `DoraiSlider.kt:85` (thumb touch area)
- [ ] Step 1: প্রতিটি site-এ `defaultMinSize(48.dp)` / minimum interactive size; visual আকার অপরিবর্তিত রাখতে হলে transparent padding দিয়ে touch area বাড়াও।

### Task 5.5: Icon-only buttons — label বাধ্যতামূলক
**Files:** `DoraiSearchField.kt:62`, `SettingsListItem.kt` (deleteContentDescription nullable → required), `RefreshIconButton.kt:29-33`, `DoraiAvatar.kt:55` (description param), `DoraiLoadingSpinner.kt` (`semantics { contentDescription = stringResource(Res.string.a11y_loading) }`), `MessageActionRow.kt` thumbs (selected semantics)
- [ ] Step 1: Signature/default পরিবর্তন → compile দিয়ে সব call-site catch; missing label → compile error (intended)।

### Task 5.6: Settings nav consistency + approval card theming
**Files:** `DoraiSettingsScreen.kt:362-368` (category rail rows: `selectable` + selected semantics — `SettingsNavigation.kt`-এর pattern copy), `ToolConfirmationCard.kt` (fixed-dark tokens → LocalDesignColorTokens)

**Phase 5 gate:** ui tests (যেগুলো আছে) PASS; Paparazzi PASS; manual TalkBack checklist note (Android device-এ ৫টি flow — verify পরে); commit `fix(a11y): selection semantics, headings, 48dp targets, labeled icon buttons`।

---

## Phase 6 — Theme/tokens, light-theme leaks, dead code, fake data (U-2, U-3, C-2)

### Task 6.1: Fake state removal (Critical C-2)
**Files:** `App.kt:215-222`, `home/DoraiHomeScreen.kt:99-109`, `DoraiChatScreenConnected.kt:165`
- [ ] Step 1: `HomeUiState` defaults: `isWorkspaceSetupDone = true` / `isSandboxRunning = true` / `"1.2 / 4 GB"` / `"Free FAST"` → real sources: sandbox status (existing sandbox state holder), storage (real metrics যদি থাকে; না থাকলে **hidden**, fake নয়), model name (configured service-এর actual name; unconfigured → "Not configured" string, localized)।
- [ ] Step 2: `DoraiChatScreenConnected.kt:165` — unconfigured হলে "Not configured" (localized `model_not_configured`) — connected layer hide করবে না।
- [ ] Step 3: Home empty states: sandbox/storage/unconfigured-এ proper empty/error UI (sandbox নেই → "Set up sandbox" CTA — SandboxTabsContent-এর existing setup flow-তে link)।
- [ ] Step 4: `App.kt`-এ hardcoded state feed সরানো; ViewModel/state holder-এ আসল flows।

### Task 6.2: Status color unification
**Files:** `SettingsStatusColors` (SettingsDesignTokens.kt:36-41), `SettingsStatusBanner.kt`, `OperationalComponents.kt` (Success/Warning/Error/Info usages), `SettingsStatusBanner` colors → সব `DoraiSemanticColors`/`LocalDesignColorTokens`-এ route।
- [ ] Step 1: `SettingsStatusColors` object → thin wrapper যা `LocalDesignColorTokens.current` থেকে read করে (dark fixed hex সরাও); call-site API পরিবর্তন নয়।
- [ ] Step 2: `ToolConfirmationCard.kt`, `WorkspaceProjectsExplorer.kt:406-458`, `DashboardScreen.kt:58-66`, `WorkspaceGitPanel.kt:265`-adjacent color sites — fixed `DesignTokens.*` → LocalDesignColorTokens।
- [ ] Step 3: Paparazzi — dark theme goldens ঠিক মিলবে কিনা; light theme goldens (যদি থাকে) diff review।

### Task 6.3: Typography/spacing consolidation (scope-limited)
**Files:** `ui/Theme.kt` (DoraiTypography lineHeights: bodySmall/labelSmall/labelMedium 16sp → 1.5× বা DoraiType থেকে), token files (`ChatSpacing`, `SettingsSpacing`, `WorkspaceSpacing`)
- [ ] Step 1: `DoraiTypography` chat scale-এ Bengali-friendly lineHeight (≥1.5): bodySmall 12sp→18sp, labelSmall 11sp→16sp, labelMedium 12sp→18sp।
- [ ] Step 2: Spacing scales: `SettingsSpacing`/`ChatSpacing`/`WorkspaceSpacing`-এ xl mismatch (20 vs 24dp) — এক canonical value (DesignTokens `xl`) দিয়ে বাকিরা delegate (deprecated typealias নয় — সরাসরি reference)।
- [ ] Step 3: Paparazzi chat/settings goldens diff review → intentional হলে re-record।

### Task 6.4: Chip/slider/avatar/badge theming + touch
**Files:** `DoraiChip.kt` (accent → LocalDesignColorTokens primary; doc comment "38dp" → reality 48dp), `DoraiSlider.kt` (thumb color token), `DoraiAvatar.kt` (primary token), `DoraiBadge.kt` (border/clip order fix)

### Task 6.5: Dead code removal (grep-verified)
**Files:** Delete `ui/components/DoraiDialog.kt`, `chat/composables/SmallIconButton.kt`, `components/v2/DoraiComponentsPreview.kt`
- [ ] Step 1: প্রতিটির জন্য: `git grep -n "DoraiDialog|SmallIconButton|DoraiComponentsPreview"` →শুধু declarations; call না থাকলে delete। `DoraiTokenPreview.kt` — delete **নয়** (design-system reference; maintainer-কে note মাত্র)।
- [ ] Step 2: Compile + tests。

### Task 6.6: Breakpoint unification
**Files:** `WorkspaceDesignTokens.kt` (`WorkspaceTier` 600/1000 → 600/840/1200), `dashboardColumnsFor` (600/1000 → 600/840/1200)

**Phase 6 gate:** `verifyPaparazziDebug` PASS (বা reviewed re-record); `desktopTest` PASS; commit `fix(ui): remove fabricated home state, unify theme tokens and status colors`।

---

## Phase 7 — Responsive/overflow ও ছোট UX defects (U-4 + minor leftovers)

- [x] Task 7.1: `maxLines = N` ছাড়া `overflow` নেই — 32 site: প্রতিটিতে `overflow = TextOverflow.Ellipsis` যোগ করা হয়েছে (সব user-facing `Text(maxLines = …)` site; approval buttons compact-এ 2-line wrap; hardcoded `13.sp` সরানো হয়ে `DoraiTypography.labelMedium`)। TextField-site গুলো স্বাভাবিকভাবেই বাদ।
- [x] Task 7.2: `DoraiApprovalActions.kt` → `weight(1f)` রেখে 2-line wrap (`maxLines = 2`) + centered overflow ellipsis + token font;লম্বা Bengali/German label ছোট screen-এ ২ লাইনে wrap হওয়ার design নিশ্চিত। (Manually check static review; on-device দেখনা pending।)
- [x] Task 7.3: `DoraiWorkspaceScreen.kt:610` fixed 300.dp → `widthIn(max = 300.dp)` + `fillMaxWidth` (compact ≤320dp fallback)।
- [x] Task 7.4: `WorkspaceScreen.kt` git status badge: `9.sp` hardcode সরিয়ে `DoraiTypography.labelSmall` + localized full label (`TextOverflow.Ellipsis`) + compact chip (`heightIn(22.dp)`/`defaultMinSize(22.dp)`) — `.take(1)` একক অক্ষর localized label বাদ। `:1241` path-এ ellipsis যোগ।
- [x] Task 7.5: `DoraiDialog` code ref-মুক্ত; `PremiumChatBackdrop.kt:151` contentDescription → `Res.string.premium_backdrop_logo_description` (docs/superpowers/plans-এ শুধু উল্লেখ আছে, app code-এ নেই)।
- [x] Task 7.6: `DoraiAgentTimeline.kt:98,171` — expandable-এ `Role.Button` + `stateDescription` (একটিমাত্র expandable হওয়ায় "Expanded"/"Collapsed" stringResource) যোগ।
- [ ] Task 7.7: `DoraiUiRenderer.kt` — widget-level try/catch **Compose-এ সম্ভব নয়** (composable invocation try/catch illegal)। নিরাপদ বিকল্প হিসেবে রাশি-ভিত্তিক defensive clamp implement করা হয়েছে (image/slider/progress/icon/avatar/countdown; NaN/Infinity/out-of-range মান) — bad node-এ widget crash না হয়ে fallback মান ব্যবহার হবে। Inline per-widget error placeholder ও পূর্ণ error boundary এখনো বাকি (Compose public error boundary নেই; অন্য level-এ সমাধান দরকার)।
- [x] Task 7.8: `TerminalInputView.kt` `deleteSurroundingText` → grapheme-aware delete (`TerminalImeTextBuffer` committed-tail track; `graphemeSafeDeleteCount`/`graphemeClusterCount`; code-unit counts-এর পরিবর্তে cluster-aligned backspace count)। **On-device Bengali IME verify pending।**
- [x] Task 7.9: `DoraiChatScreen.kt` — `SelectionContainer` সরানো হয়েছে Markdown অংশ থেকে; এখন `MarkdownContent`-এর ভেতরে শুধু text-run segment-গুলোর চারপাশে `SelectionContainer` (DORAI UI block গুলো নির্বাচন-বহির্ভূত scope-এ)। Paparazzi verify পুনঃচলুনীতে render diff চেক করা হয়েছে।

**Phase 7 gate:** Paparazzi + desktopTest PASS; commit `fix(ui): ellipsis/overflow sweep, adaptive width fixes`।

---

## Phase 8 — Docs, changelog ও final gates

- [ ] Task 8.1: `CHANGELOG.md` — unreleased section-এ user-facing fixes: Bengali strings repaired, UI localized, home fake state removed, a11y improvements, link safety।
- [ ] Task 8.2: `docs/features/chat.md`, `docs/features/settings-export-import.md` (যদি touch হয়) — "Last verified" আপডেট + পরিবর্তিত behavior (Link allow-list, fake state removal) document।
- [ ] Task 8.3: `agents/resources-and-localization.md` — parity gate (`desktopTest:LocaleParityTest`, `scripts/validate-locales.ps1`) উল্লেখ; future locale addition-এর নিয়ম।
- [ ] Task 8.4: Final: `./gradlew spotlessCheck` → `:composeApp:desktopTest` → `:screenshotTests:verifyPaparazziDebug` → `:androidApp:assembleFossDebug` → `pwsh scripts/validate-locales.ps1` — সব PASS না হলে কোথাও PASS দাবি নয়; Bengali policy §13 DoD checklist run করে output-এ যোগ।
- [ ] Task 8.5: PR description-এ: audit summary link (chat), phase-by-phase verification তালিকা, known limitations (53 stale locales, Android device verify pending, iOS locale relaunch, wasm grapheme fallback)।

---

## Execution Handoff

সব phase শেষে: `fix/ui-bangla-remediation` → `main` PR (squash নয় — phase commits ইতিহাস রাখতে merge commit বেছে নাও যদি repo convention Allow করে; না হলে squash-এ phase bullet-গুলো PR body-তে)।


## Phase 9 — Verification follow-up (post-merge)

- [x] Task 9.1: Bengali resource canonical form — `U+09DF` বনাম `U+09AF U+09BC` অসামঞ্জস্য (১৭টি string) scope-সীমিত NFC normalization দিয়ে canonical spelling-এ আনা হয়েছে; `BengaliCanonicalFormTest` (scope/equivalence/round-trip) CI gate হিসেবে যোগ।
- [x] Task 9.2: Stale-locale backlog — `LocaleCoverageBaselineTest` (কোনো locale ইতিমধ্যে পাওয়া key হারালে fail) + `scripts/locale-coverage.ps1` (coverage report, per-locale missing-key worklist, `-FailOnRegression`) + baseline (`scripts/locale-coverage-baseline.json`)।
- [x] Task 9.3: JVM-এ যাচাইযোগ্য অংশ — `AppLanguageSwitchTest` (platform locale switch), `TerminalImeTextBufferTest` (grapheme-aligned IME backspace count) — CI-তে চলছে।
- [x] Task 9.4: ডিভাইস checklist — `docs/development/bangla-device-verification-checklist.md` (Android/iOS locale switch, Bengali IME composition, terminal backspace, dynamic UI)।
- [ ] Task 9.5: **ডিভাইসে চালিয়ে যাচাই** — এই environment-এ `adb`/`emulator` নেই, তাই on-device item (Android/iOS locale switch, বাংলা IME composition/backspace, real-device UX) এখনও **pending**; checklist অনুযায়ী হাতে ডিভাইসে চালিয়ে ফলাফল (pass/fail/blocked) রিপোর্ট করতে হবে।