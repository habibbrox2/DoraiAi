# Premium Dark Home Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the compact empty-chat screen visually match the supplied premium dark DORAI AI reference while preserving the existing Kotlin Multiplatform chat runtime and capability gates.

**Architecture:** Keep `ChatScreen` as the owner of UI state, navigation, draft persistence, and side-panel behavior. Build the premium dark presentation from Compose-native backdrop, semantic colors, and existing header/empty-state/composer composables; retain the existing `ChatActions`, `QuestionInput`, drawer, sandbox, and service/model selection integrations rather than creating duplicate state or mock workflows.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, Compose Resources, Paparazzi screenshot tests, JUnit.

**Spec:** Supplied `c45f6a59-96a8-47c7-957c-3fe8245f96b9.png` dark reference; `docs/chat-ui-upgrade-plan.md`; `docs/ui-upgrade-plan-7-premium-chat.md`; `docs/features/dynamic-ui.md`; `docs/features/reasoning.md`.

## Visual Comparison Report

| Area | Current `mobile-1.png` | Target reference | Implementation decision |
|---|---|---|---|
| Canvas | Flat grey-to-dark gradient | Obsidian canvas with deep cyan/violet waves and controlled neon edges | Compose-native dark ambient backdrop; no bitmap/CSS dependency. |
| Header | Compact dark HUD with many small controls | Floating navy glass bar, clear clusters, status label, terminal and settings actions | Restyle `AgentHud`; retain all existing callbacks and hide non-fitting optional controls behind existing capability gates. |
| Hero | Dark square BK monogram | Cyan/violet circular orb with orbital light trails and prominent welcome hierarchy | Create a lightweight animated orb with a reduced-motion static fallback; derive state from real runtime flags. |
| Actions | Wide dark cards with two-line Bengali copy | One-line, horizontally scrollable illuminated glass chips | Keep only supported prompt/capability actions and retain `onQuickStart`; do not invent tools. |
| Composer | Dark static input | Floating dark-glass action bar with attachment, slash/voice, and cyan send affordances | Re-skin existing `QuestionInput`; preserve its attachments, slash completion, draft, keyboard inset, send/cancel semantics. |
| Continuation / bottom navigation | Not present in current empty-chat route | Reference contains fictional RPG metrics and five destinations | Do not render fake progress or cosmetic navigation. Add only after verified saved-game data and application navigation contracts are supplied. |

## Global Constraints

- The supplied premium dark image is the visual source of truth; the existing dark/OLED plans remain behavioral and security constraints.
- Keep portable UI in `commonMain`; do not introduce JVM, Android, CSS, or new dependency requirements.
- Preserve `ChatUiState`/`ChatActions` ownership, attachment handling, slash command sourcing, provider selection, sandbox approval gates, dynamic-UI safety, and reasoning privacy behavior.
- Status, task history, tools, and navigation may be shown only when backed by actual application state; never fabricate RPG metrics, “all systems ready”, tools, or activity.
- Respect `prefersReducedMotion()`, Material accessibility semantics, 48dp touch targets, font scaling, contrast, and compact/medium/expanded `ChatAdaptivePolicy` behavior.
- Preserve the pre-existing unrelated edit in `iosApp/Configuration/Config.xcconfig`.
- Do not run the write-capable screenshot-update workflow; use focused record/verify commands only after inspecting available Gradle tasks.

## Review Focus

- Dark theme: all home-screen foreground text, borders, and icons remain readable against the new ambient backdrop.
- Compact width: header clusters never overlap or clip at the Pixel 9a screenshot width and action chips remain reachable by horizontal scrolling.
- Runtime state: HUD/orb color and label describe only actual idle/generating/speaking/sandbox states.
- Composer: draft text, file attachments, slash suggestions, keyboard insets, and streaming cancellation survive the visual refactor.
- Reduced motion: the orb/status effects render a stable, useful fallback when motion is disabled.

## File Structure

- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ChatDesignTokens.kt` — define premium-dark chat surface/color/shape accessors without changing unrelated theme contracts.
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/PremiumChatBackdrop.kt` — reusable dark ambient background and motion-safe orb primitives.
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/AgentHud.kt` — reference-aligned HUD layout and actual status copy.
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/EmptyState.kt` — reference-aligned hero and capability-backed quick-action chips.
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/QuestionInput.kt` — premium-dark glass composer treatment only; preserve its existing event contract.
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ChatScreen.kt` — host the backdrop and pass real presentation flags to child composables without changing action ownership.
- Modify: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/ScreenshotTest.kt` — cover compact dark empty state and the reduced-motion/static presentation path supported by the test harness.
- Modify if behavior documentation changes: `docs/ui-upgrade-plan-7-premium-chat.md` — correct visual theme guidance and update its verification metadata after visual validation.

### Task 1: Establish the Dark Reference Test Boundary

**Files:**
- Modify: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/ScreenshotTest.kt`
- Read: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/ScreenshotTestData.kt`
- Read: `screenshotTests/build.gradle.kts`

**Interfaces:**
- Consumes: `ChatScreenContent(uiState, textToSpeech, ...)` and `DarkColorScheme`.
- Produces: named Paparazzi coverage for the compact, dark empty-chat reference state.

- [ ] **Step 1: Discover the exact focused screenshot tasks and record the existing compact dark baseline without updating copied marketing screenshots**

Run: `gradlew.bat tasks --all` and the existing focused Paparazzi test command discovered from that output.

Expected: the task name and an unmodified baseline are recorded; no `updateScreenshots` task is invoked.

- [ ] **Step 2: Add a focused `chatEmptyState_premiumDark` screenshot fixture whose `ChatUiState` has real available-service and capability inputs only**

The fixture must not add fake history, tool activity, RPG values, or synthetic navigation destinations.

- [ ] **Step 3: Run the focused screenshot test to verify the pre-implementation visual assertion fails or records the intended mismatch**

Run the exact discovered Paparazzi test selector.

Expected: a controlled visual mismatch against the old snapshot, establishing that the target screen is exercised.

### Task 2: Normalize Scoped Premium-Dark Presentation Tokens

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ChatDesignTokens.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/ThemeColorSchemeTest.kt` (extend only if public theme values change)

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme` and the existing `ChatSpacing`/`ChatShapes` API.
- Produces: composable surface, border, text, and accent values for the premium dark home screen.

- [ ] **Step 1: Write a focused test for every newly public semantic token used by the compact premium-dark screen**

Assert the agreed dark surface, border, text, and cyan/violet accent roles—not implementation-specific animation values.

- [ ] **Step 2: Run the focused common test and verify it fails because the premium-dark semantic accessors do not exist**

Run the discovered common-test task with the focused selector.

Expected: FAIL with the missing premium-dark token API.

- [ ] **Step 3: Implement premium-dark semantic accessors in `ChatDesignTokens.kt` using the target palette (`#030B1A` base, cyan `#00D7E8`, violet `#8A63F6`, high-contrast white text)**

Keep the existing `DesignTokens` API stable for unrelated screens. Use 16dp cards, a 24dp HUD, and pill actions to mirror the reference hierarchy.

- [ ] **Step 4: Run the focused token test**

Expected: PASS in premium-dark screenshot and unit-test coverage.

### Task 3: Build the Motion-Safe Dark Backdrop and Agent Orb

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/PremiumChatBackdrop.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ChatScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/EmptyState.kt`
- Test: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/ScreenshotTest.kt`

**Interfaces:**
- Consumes: actual `isLoading`, `isSpeaking`, and sandbox execution flags already available in the chat screen.
- Produces: `PremiumChatBackdrop(content)` and `AgentOrb(presentationState, reducedMotion)` composables without runtime-state ownership.

- [ ] **Step 1: Add the compact dark screenshot assertion for the ambient backdrop and static/reduced-motion orb path**

The expected image must show controlled cyan/violet illumination, orbital trails, and legible text/controls without excessive bloom.

- [ ] **Step 2: Implement `PremiumChatBackdrop` with Compose `Brush`/`Canvas` deep-navy wave layers and no remote or generated bitmap assets**

Place it as a screen background in `ChatScreen`, behind the existing shell. Ensure it fills the viewport without intercepting input.

- [ ] **Step 3: Replace the empty-state square logo treatment with `AgentOrb`, mapping idle/loading/speaking/executing colors only from passed runtime state and using `prefersReducedMotion()` for a static fallback**

Do not add a “listening” or “completed” state unless the runtime exposes it.

- [ ] **Step 4: Run the focused dark screenshot and the existing empty-state screenshot suite**

Expected: PASS; no content regression in dark or dynamic-UI screenshots.

### Task 4: Restyle the HUD Without Changing Its Behavior

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/AgentHud.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/TopBar.kt` only if a display-only status argument is needed
- Test: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/ScreenshotTest.kt`

**Interfaces:**
- Consumes: the existing `AgentHud` callbacks, available service/model lists, `isGenerating`, `isShellExecuting`, and speech flags.
- Produces: a compact floating header retaining drawer, service selection, sandbox, TTS, interactive mode, back, and settings behavior.

- [ ] **Step 1: Add a compact-width screenshot assertion covering the HUD with its real optional controls absent and with sandbox available**

Verify no cluster overlaps at phone width and each shown icon has a content description.

- [ ] **Step 2: Replace the flat rectangular HUD surface with a translucent navy glass container, cyan/violet edge treatment, a visual divider before the real status label, and circular terminal/settings controls**

Keep the service selector functional; if its label cannot fit at compact width, retain an accessible selector affordance rather than clipping model text.

- [ ] **Step 3: Derive the visible status label from existing booleans in priority order: executing, generating, speaking, then idle**

Do not claim MCP calls, approval waits, connectivity, or completion without typed state.

- [ ] **Step 4: Run the focused HUD screenshot test and `ChatAdaptivePolicyTest`**

Expected: PASS; 600dp and 840dp policies are unchanged.

### Task 5: Align the Hero and Quick Actions to the Reference

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/EmptyState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/ChatScreen.kt` only to pass existing capability flags
- Test: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/ScreenshotTest.kt`

**Interfaces:**
- Consumes: existing `onQuickStart(prompt)` and available runtime capability flags.
- Produces: horizontally scrollable, one-line compact action chips that call the same `ChatActions.ask` path.

- [ ] **Step 1: Update the focused screenshot to assert the centered title/subtitle hierarchy and at least one chip row at compact width**

Use the product name exactly as `DORAI AI`; do not introduce hard-coded game state or tools.

- [ ] **Step 2: Refactor the quick-action item model to accept a capability predicate and render only supported actions**

Retain New Chat/Ask Question and other existing prompt-backed actions; make Sandbox Check conditional on the real sandbox availability passed from `ChatScreen`.

- [ ] **Step 3: Replace the wide dark cards with compact dark-glass chips matching the reference’s icon-plus-label layout and horizontal scrolling**

Use 48dp minimum interaction targets, stable content descriptions, and readable localized labels. Do not replace the current prompt/callback contract.

- [ ] **Step 4: Run focused screenshot coverage and any affected chat composable unit tests**

Expected: PASS with no fake capability displayed.

### Task 6: Re-skin the Existing Universal Composer

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/chat/composables/QuestionInput.kt`
- Test: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/chat/composables/DetectSlashQueryTest.kt`
- Test: `screenshotTests/src/test/kotlin/com/dorai/ai/screenshots/ScreenshotTest.kt`

**Interfaces:**
- Consumes: existing `QuestionInput` parameters, draft `TextFieldValue`, attachment callbacks, slash-result provider, and send/cancel callbacks.
- Produces: the reference-aligned premium-dark composer surface with the same interactions.

- [ ] **Step 1: Run `DetectSlashQueryTest` before editing and record its passing behavior**

Expected: PASS, proving current slash matching behavior before a visual-only change.

- [ ] **Step 2: Add a dark screenshot fixture containing a non-empty draft and enabled attachment/slash/voice/send affordances supported by the current UI state**

Avoid an action that requires ungranted runtime permission in a screenshot.

- [ ] **Step 3: Apply the dark floating action-bar surface, inner separators, cyan/violet edge treatment, and cyan send affordance while retaining every existing callback, enabled state, IME inset, attachment preview, and streaming stop branch**

Do not add a second text field or alternate state holder.

- [ ] **Step 4: Run the slash unit test and focused Paparazzi composer screenshot**

Expected: both PASS.

### Task 7: Validate and Document the Accepted Scope

**Files:**
- Modify if required by accepted behavioral/documentation change: `docs/ui-upgrade-plan-7-premium-chat.md`
- Inspect: `docs/features/dynamic-ui.md`, `docs/features/reasoning.md`

**Interfaces:**
- Consumes: final screenshots and existing test/build task inventory.
- Produces: evidence-backed acceptance report and updated visual guidance, without changing dynamic UI or reasoning behavior.

- [ ] **Step 1: Run the focused screenshot suite, `ChatAdaptivePolicyTest`, `DetectSlashQueryTest`, and relevant theme test**

Expected: PASS with recorded commands and exact results.

- [ ] **Step 2: Run the narrowest Android compile task and, after it passes, the relevant Desktop and Wasm compile/distribution tasks discovered from `gradlew.bat tasks --all`**

Expected: each claimed target completes successfully; report skipped targets explicitly.

- [ ] **Step 3: Compare the new compact dark screenshot to the supplied target for header spacing, hero placement, chip row, composer, contrast, and absence of fake UI**

Record remaining differences, including deliberately deferred continuation-card and bottom-navigation features.

- [ ] **Step 4: Update the chat visual plan only if it must now describe the new premium-dark reference direction, then inspect `git diff --check` and `git diff --stat`**

Expected: no whitespace errors; unrelated `iosApp/Configuration/Config.xcconfig` remains untouched.

## Self-Review

- **Spec coverage:** The plan covers the reference’s ambient dark canvas, HUD, orb/title, quick actions, and composer. It deliberately excludes fake continuation metrics and bottom navigation because no verified state or navigation contract exists.
- **Step scan:** Each task has a focused test/baseline, implementation action, and validation command; no task introduces a duplicate state owner.
- **Type consistency:** `ChatScreenContent` and `QuestionInput` retain their existing contracts; new backdrop/orb APIs are presentation-only and receive real booleans from `ChatScreen`.
- **Review focus:** Compact layout, dark contrast, runtime-status truthfulness, composer preservation, and reduced motion are assigned to screenshot/unit validation in Tasks 1–6.
- **Proportion:** This plan limits the first delivery to the supplied empty-chat mobile reference; broader dashboard/navigation/game functionality requires a separate product decision and plan.

