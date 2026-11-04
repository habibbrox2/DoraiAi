# Keyless Auto Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route the built-in Free tier through a hidden keyless Kilo/OpenCode fallback exposed as the `Auto` model alias when no configured provider can answer.

**Architecture:** Add a focused common-code fallback component that owns the `auto` alias and ordered keyless provider/model targets. Wire normal chat to try configured services first and always use the hidden fallback after failures; show one `Auto` Free entry in chat and exclude Kilo and OpenCode from user-facing service selection while retaining provider metadata for internal requests. Update the multi-service behavior spec and add focused common tests.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, kotlinx.coroutines, commonTest.

**Spec:** User request in this conversation; repository behavior reference: `docs/features/multi-service.md`.

## Global Constraints

- Keep all shared fallback behavior in `composeApp/src/commonMain/`; do not use JVM-only APIs.
- Preserve the configured provider ordering and existing provider request behavior.
- Keep Kilo and OpenCode available to internal request dispatch but hide them from user-facing service selection; hide legacy entries without deleting persisted settings.
- Do not add dependencies or change provider credentials; keyless targets must send no API key.
- Auto fallback is unconditional after configured-provider failures; the previous Free fallback preference does not disable it.
- Update the relevant feature spec and its `Last verified` date.

## Review Focus

- No configured providers: the built-in Free path must use the `Auto` alias and try the internal free targets.
- Configured providers succeed: return their response without invoking hidden targets.
- Configured providers fail: always continue to Auto, without exposing Kilo/OpenCode in UI/status.
- All keyless targets fail: surface the final provider error rather than an empty/success-shaped response.
- A provider failure after tool side effects must not replay the tool loop and duplicate those effects.
- Legacy saved Kilo/OpenCode instances must not reappear in user-facing service selection.

---

### Task 1: Define the hidden keyless Auto router

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/data/KeylessFreeTierFallback.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/data/KeylessFreeTierFallbackTest.kt`

**Interfaces:**
- Produces `internal const val AUTO_MODEL_ID = "auto"` and an ordered, typed list of internal targets, each containing a `Service` and its actual upstream free model ID.
- The first target uses Kilo's `kilo-auto/free`; OpenCode targets use only the free model IDs already declared for OpenCode in `Service.kt`.
- The helper does not expose targets as configured service instances and does not read or persist API keys.

- [ ] **Step 1: Write tests for the alias and ordered free targets**

Assert the alias is exactly `auto`, Kilo is first with model `kilo-auto/free`, every subsequent OpenCode model is in its declared free-model set, and no paid model appears.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `.\gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.data.KeylessFreeTierFallbackTest"`
Expected: FAIL because the router and target list do not exist yet.

- [ ] **Step 3: Implement the internal target list**

Create the focused common-code component and keep the provider/model mapping in this new file, as requested.

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `.\gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.data.KeylessFreeTierFallbackTest"`
Expected: `BUILD SUCCESSFUL`.

### Task 2: Route Free chat through Auto and fallback on provider failure

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/data/RemoteDataRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ChatViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/ServicesSettings.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/data/KeylessFreeTierFallbackTest.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/chat/ChatViewModelExtendedTest.kt`

**Interfaces:**
- Consumes the ordered targets and `AUTO_MODEL_ID` from Task 1.
- Keep provider dispatch on the existing `askWithService`/`plainChat` path; allow the internal target's upstream model ID to override settings-derived credentials without changing saved user settings.
- Use `FallbackStatus` with a public Free/Auto label only; never report internal target names.

- [ ] **Step 1: Add failing fallback-flow tests**

Cover: no configured provider invokes Kilo with `kilo-auto/free`; Kilo failure tries OpenCode free targets in order; a configured provider success does not invoke fallback; fallback exhaustion propagates the final error; no API key is supplied to hidden targets.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `.\gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.data.KeylessFreeTierFallbackTest"`
Expected: FAIL because chat does not dispatch through the hidden router.

- [ ] **Step 3: Wire the router into normal chat**

When there are no configured services, use the internal Auto fallback as the Free path. After configured services fail, invoke the hidden router regardless of the previous Free fallback preference. Keep the existing fallback toggle out of the UI because it no longer controls this required behavior. Preserve retry behavior, context validation, visible errors, and the no-replay guarantee for tool side effects.

- [ ] **Step 4: Run focused fallback and existing chat/data tests**

Run: `.\gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.data.KeylessFreeTierFallbackTest"`
Expected: `BUILD SUCCESSFUL`; all routing and failure assertions pass.

### Task 3: Hide internal providers and document behavior

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/data/RemoteDataRepository.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsViewModelTest.kt`
- Modify: `docs/features/multi-service.md`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/data/KeylessFreeTierFallbackTest.kt`

**Interfaces:**
- Preserve `Service.fromId` and internal provider metadata for request dispatch.
- Exclude `Service.Kilo` and `Service.OpenCode` from available/configured user-facing services, including legacy saved entries, without deleting their persisted settings.

- [ ] **Step 1: Add failing service-visibility assertions**

Assert Kilo and OpenCode are absent from services users can add or select, while `Service.fromId("kilo")` and `Service.fromId("opencode")` still resolve for internal dispatch.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `.\gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.data.KeylessFreeTierFallbackTest"`
Expected: FAIL because both providers are currently included in user-facing service choices.

- [ ] **Step 3: Hide internal entries and update the feature spec**

Keep the provider objects available internally, filter legacy instances from active user-facing service state, and document the Auto fallback, its failure order, and provider invisibility. Correct the existing default-provider description and refresh `Last verified`.

- [ ] **Step 4: Run focused tests and formatting validation**

Run: `.\gradlew.bat :composeApp:desktopTest --tests "com.dorai.ai.data.KeylessFreeTierFallbackTest"`
Expected: `BUILD SUCCESSFUL`.

Run: `.\gradlew.bat spotlessCheck`
Expected: `BUILD SUCCESSFUL`.
