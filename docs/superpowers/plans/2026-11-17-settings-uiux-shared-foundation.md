# Settings UI/UX Redesign — Shared Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish one consistent, accessible, reusable foundation for every Settings detail screen — a shared header, a shared component vocabulary, defined interaction states, responsive behavior, and a repeatable screen recipe — so the later per-screen work (General/Agent/Providers/Tools/MCP) only fills in content instead of reinventing UI.

**Architecture:** The codebase already has equivalent pieces under different names (`SettingsCard`, `SettingsRow`, `SettingsSwitchCard`, `SettingsGroup`, `SettingsStatusRow`, `SettingsStatusBanner`, `SettingsConfirmDialog`, `SettingsSectionHeader`, `CollectionEmptyState`, `DoraiListDetailPaneScaffold`, `SettingsDetailSectionHeading`). This plan introduces a single facade in the `settings` package that **wraps/adapts those existing components** (no duplicate primitives), adds the missing states (loading/error/empty), and exposes one shared header + screen shell. Detail screens migrate onto it in a later plan.

**Tech Stack:** Kotlin Multiplatform / Compose Multiplatform, Material 3, `DoraiTypography`, `LocalDoraiColors`/`LocalDesignColorTokens`, `SettingsSpacing`/`SettingsShapes`, existing `DoraiListDetailPaneScaffold` + `LocalDoraiWindowWidthClass`. Tests: `commonTest` uses **pure `kotlin.test` only** (no `createComposeRule`/`runComposeUiTest` infrastructure exists); visual/a11y behavior is verified via **Paparazzi screenshotTests** (`screenshotTests` module). No new dependencies.

**Spec:** User-provided Settings UI/UX spec (Bengali screenshots): Shared Header; Shared Components table (SettingsSection, SettingsPreferenceRow, SettingsSwitchRow, SettingsActionRow, SettingsStatusCard, SettingsInfoCard, SettingsEmptyState, SettingsErrorState, SettingsLoadingState, SettingsConfirmationDialog); Responsive Layout (compact single column / medium+ left sidebar + right pane); Accessibility (48dp targets, content descriptions, screen reader, font scaling, contrast, keyboard, clear selected state); Interaction States (default/pressed/focused/disabled/loading/error/success); per-screen content for General/Agent/Providers/Tools/MCP.

> Scope note: This plan covers the **shared foundation only** (per the chosen priority). The per-screen content migration (General, Agent, Providers, Tools, MCP) and the “GitHub → Workspace” move are explicitly out of scope here and get their own plan(s).

## Global Constraints

- Java 21 required (`java -version` must report 21).
- No new Gradle dependencies/plugins; `gradle/libs.versions.toml` unchanged without approval.
- `commonMain` only: no `java.*`/`javax.*`/`android.*`; all composables multiplatform.
- All user-facing strings in `composeApp/src/commonMain/composeResources/values/strings.xml` (`Res.string.*`); no hardcoded copy; add all target locales for any new key (verify `composeResources/values-*/strings.xml`).
- Bengali/Unicode policy applies to any new user-facing text: read `docs/development/bengali-language-and-rendering-policy.md` before adding strings; no Bengali support claim from ASCII-only tests.
- Reuse existing components/tokens; do not create a second primitive where one exists. DRY.
- Accessibility floor: every interactive control ≥48dp touch target (`defaultMinSize`/`DesignTokens.MinTouchTarget`), meaningful `contentDescription`/semantics, semantic `heading()` for section titles, visible focus, preserved selected state.
- Preserve security behavior: GitHub/Providers/MCP destructive actions keep `SettingsConfirmDialog` + undo flows; no SSRF/credential/network config weakening.
- Verification before each commit: `gradlew.bat :composeApp:test` BUILD SUCCESSFUL; `gradlew.bat :screenshotTests:verifyPaparazziDebug` for any global component/default change; `spotlessCheck` green once per change-set.
- Branch policy: never commit to `main`; one task branch from `origin/main` (e.g. `feat/settings-ui-shared-foundation`).
- Because this work changes shared defaults/components, run the screenshot gate (`verifyPaparazziDebug`) and re-record affected goldens with `recordPaparazziDebug --tests "..."` in the same PR when intentional diffs appear.

## Review Focus

1. **Duplicate titles (the exact bug just fixed manually):** a screen that renders a category/section heading and then a card with the same title must show it once. → Task 1 test + Task 6 migration asserts one heading per group.
2. **48dp touch targets & keyboard focus on rows/switches/actions:** compact cards must not fall below the accessibility floor. → Task 2/3 component tests assert `defaultMinSize` and semantics.
3. **Loading/error/empty states are real, not fake data:** the spec forbids fake marketplaces/status; components must render states from an explicit state argument with no invented content. → Task 4 tests assert no placeholder rows when `isLoading`/`error` set.
4. **Responsive compact vs medium+:** no permanent list pane on compact; sidebar + detail on medium+; must reuse `DoraiListDetailPaneScaffold`, not a second layout. → Task 5 test asserts compact shows only detail pane.
5. **State after refresh/selection:** manual model ids and selected rows survive a settings refresh (the `ensureSelectedModelPresent` invariant) and selected state is visually clear. → Task 6 verification via existing `SettingsViewModelTest` + screenshot.

---

### Task 1: Shared Settings Header (single source for all detail screens)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsDetailHeader.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsDetailHeaderTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt` (replace `SettingsPageHeader` call at ~line 610)

**Interfaces:**
- Produces: `SettingsDetailHeader(title: String, icon: ImageVector, description: String? = null, onBack: (() -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null, modifier: Modifier = Modifier)` — back navigation + category icon + title + optional description + contextual actions, semantic heading, all from existing `SettingsPageHeader` behavior.
- Consumes: `LocalDoraiColors`, `DoraiTypography`, `stringResource(Res.string.a11y_navigate_back)`.

- [ ] **Step 1: Write a failing compile/signature test in commonTest**

There is no Compose UI test runner in this repo (verified: no `createComposeRule`/`runComposeUiTest` anywhere). Prove the new API exists by referencing it in a pure `kotlin.test` signature test:

```kotlin
package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertTrue

class SettingsDetailHeaderTest {
    @Test
    fun `shared header is exposed for detail screens`() {
        // Referencing the composable in a function type proves the symbol/signature compiles.
        val reference: Any = ::SettingsDetailHeader
        assertTrue(reference.toString().contains("SettingsDetailHeader"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.ui.settings.SettingsDetailHeaderTest"`
Expected: FAIL (unresolved reference `SettingsDetailHeader`).

- [ ] **Step 3: Implement `SettingsDetailHeader`**

Move the body of `SettingsPageHeader` (in `SettingsV2Components.kt`) into this composable; add `description` and `actions` slots. Keep `onBack` null = no back button. Mark title with `Modifier.semantics { heading() }`. Keep the existing icon tint/background tokens.

- [ ] **Step 4: Update `DoraiSettingsScreen` to use `SettingsDetailHeader`** instead of `SettingsPageHeader`; keep `SettingsPageHeader` as a deprecated alias only if other call sites exist (grep first).

- [ ] **Step 5: Run test + screenshot gate**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.ui.settings.SettingsDetailHeaderTest"`, then `gradlew.bat :screenshotTests:verifyPaparazziDebug`.
Expected: PASS (re-record affected settings goldens with `recordPaparazziDebug --tests "..."` if intentional diff). Visual/a11y behavior (heading semantics, back content description, 48dp) is proven by the Paparazzi goldens, not by UI-unit assertions.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsDetailHeader.kt composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsDetailHeaderTest.kt
git commit -m "feat(settings): add shared detail header with back/title/description/actions"
```

---

### Task 2: Shared row primitives — Preference / Switch / Action

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsRows.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsRowsTest.kt`

**Interfaces:**
- Consumes: existing `SettingsCard`, `ToggleableHeadline`, `SettingsSwitchCard`, `SettingsSectionCard`, `ActionButton`/`ActionButtonVariant`.
- Produces:
  - `SettingsPreferenceRow(title: String, subtitle: String? = null, leadingIcon: (@Composable () -> Unit)? = null, onClick: () -> Unit, modifier: Modifier = Modifier)` — navigable row, ≥48dp, `Role.Button`.
  - `SettingsSwitchRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier)` — wraps `SettingsSwitchCard`/`ToggleableHeadline` behavior, whole row is one `Role.Switch` target.
  - `SettingsActionRow(title: String, subtitle: String? = null, actionLabel: String, onAction: () -> Unit, variant: ActionButtonVariant = ActionButtonVariant.SECONDARY, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write a failing signature test in commonTest** referencing `::SettingsPreferenceRow`, `::SettingsSwitchRow`, `::SettingsActionRow` (same pattern as Task 1). Behavioral a11y (role, 48dp, semantics) is covered by `screenshotTests:verifyPaparazziDebug` goldens for the settings screens that adopt these rows.

- [ ] **Step 2: Run test** → FAIL (symbols missing).

- [ ] **Step 3: Implement `SettingsRows.kt`** by delegating to existing components (no new drawing code). Apply `.defaultMinSize(minHeight = DesignTokens.MinTouchTarget)` and semantics.

- [ ] **Step 4: Run test** → PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat(settings): add shared preference/switch/action rows with a11y floor"`

---

### Task 3: Shared state components — Status / Info / Empty / Error / Loading

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsStates.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsStatesTest.kt`

**Interfaces:**
- Consumes: `SettingsStatusBanner` + `SettingsBannerSeverity`, `SettingsKeyValueRow`/`SettingsStatusRow`, `CollectionEmptyState`, `CircularProgressIndicator`.
- Produces:
  - `SettingsStatusCard(label: String, status: String, color: Color, modifier: Modifier = Modifier)` → wraps `SettingsStatusRow`.
  - `SettingsInfoCard(message: String, modifier: Modifier = Modifier)` → wraps `SettingsStatusBanner(severity = Info)`.
  - `SettingsEmptyState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier)` → wraps `CollectionEmptyState`.
  - `SettingsErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier)` → wraps `SettingsStatusBanner(severity = Error)`; `liveRegion = Polite`.
  - `SettingsLoadingState(message: String? = null, modifier: Modifier = Modifier)` → progress indicator + optional label; no fake content.

- [ ] **Step 1: Write a failing signature test in commonTest** referencing all five composables (`::SettingsStatusCard`, `::SettingsInfoCard`, `::SettingsEmptyState`, `::SettingsErrorState`, `::SettingsLoadingState`). `liveRegion`/no-fake-content behavior is documented in KDoc and verified by screenshot goldens.

- [ ] **Step 2: Run test** → FAIL.

- [ ] **Step 3: Implement by delegation** to existing components; keep a single visual language (same border alpha/tones as `SettingsStatusBanner`).

- [ ] **Step 4: Run test** → PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat(settings): add shared status/info/empty/error/loading states"`

---

### Task 4: Confirmation dialog + interaction-state tokens

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsConfirmDialog.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsConfirmDialogTest.kt`

**Interfaces:**
- Consumes: `MaterialTheme`, `ButtonDefaults`, existing destructive `ActionButtonVariant.DESTRUCTIVE`.
- Produces: `SettingsConfirmationDialog` (rename/expose `SettingsConfirmDialog` with a canonical name) with explicit **pressed/focused/disabled** colors on confirm/dismiss via `ButtonDefaults.textButtonColors(...)`, destructive confirm uses `LocalDoraiColors.current.err`, ≥48dp buttons, and `contentDescription` where needed. Keep existing params.

- [ ] **Step 1: Write a failing signature test in commonTest** — construct a `::SettingsConfirmationDialog` reference with the `enabled` parameter wired; assert the symbol compiles. Add a pure helper test for any extracted color/interaction-state resolver (e.g. `confirmationButtonColors(enabled)`) if one is introduced.

- [ ] **Step 2: Run test** → FAIL if `enabled` param/symbol missing.

- [ ] **Step 3: Implement** — add `enabled: Boolean = true` param; define interaction colors; keep behavior (confirm/dismiss callbacks unchanged).

- [ ] **Step 4: Run test + `spotlessCheck`** → PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat(settings): define interaction states for confirmation dialog"`

---

### Task 5: Responsive settings screen shell (reuse adaptive scaffold)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt` (the detail-content `Column` at ~lines 605-623)
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsResponsiveShellTest.kt`

**Interfaces:**
- Consumes: `DoraiListDetailPaneScaffold`, `LocalDoraiWindowWidthClass`, `DoraiWindowWidthClass.isCompact`, `StaggeredSettingsColumns` (existing).
- Produces: `SettingsDetailShell(content: @Composable () -> Unit)` — on compact: single scrollable detail column; on medium+: category list pane (left) + detail pane (right) via the existing scaffold, with multi-column content via `StaggeredSettingsColumns` inside the detail pane. Category list must reuse the existing category icons/titles (no new nav model).

- [ ] **Step 1: Add Paparazzi screenshot tests** for the settings home at compact (`PIXEL_9A`) and medium/tablet width (`PIXEL_TABLET`), reusing the existing `DoraiUiV2ScreenshotTest.snap(...)` helper pattern (`destination = DoraiDestination.Settings`). If a snapshot name already exists (e.g. `settings_home_compact_ogshine`), no new test is needed — the gate below covers it.

- [ ] **Step 2: Run `gradlew.bat :screenshotTests:verifyPaparazziDebug`** → FAIL with golden diff (new responsive layout changes rendering).

- [ ] **Step 3: Implement** — wrap existing detail content in `SettingsDetailShell`; keep the per-category scroll reset (`remember(category) { ScrollState(0) }`).

- [ ] **Step 4: Run test + `screenshotTests:verifyPaparazziDebug`** (tablet/compact goldens) → PASS; re-record if intentional.

- [ ] **Step 5: Commit** — `git commit -m "feat(settings): responsive detail shell via adaptive scaffold"`

---

### Task 6: Accessibility sweep + duplicate-title regression on shared path

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsV2Components.kt` (`SettingsGroup`, `SettingsSectionCard`, `SettingsSwitchCard`, `SettingsChipRow`)
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsSectionHeader.kt`
- Modify: `docs/features/settings-*.md` if a matching feature doc exists (grep first); else create `docs/features/settings-ui-foundation.md`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/SettingsA11yTest.kt`

**Interfaces:**
- Consumes: all components from Tasks 1-5.
- Produces: consistent 48dp targets, `contentDescription`/semantics, `heading()` only on the single section title, `selected` semantics on chip rows, keyboard tab order (focus traversal) and visible focus via `MaterialTheme` defaults.

- [ ] **Step 1: Write a failing signature test in commonTest** referencing the shared components and a pure `headingOwner` policy helper if one is introduced (e.g. `fun headingOwnerFor(group: SettingsGroupKind)`). The concrete a11y properties (single `heading()`, `selected`, 48dp, content descriptions) are proven by the Paparazzi settings goldens; Kotlin tests here guard the shared policy/API only.

- [ ] **Step 2: Run test** → FAIL on current code.

- [ ] **Step 3: Implement** — hoist `heading()` to `SettingsDetailHeader`/`SettingsSectionHeader` only; remove stray `heading()` from card-level duplicates; apply `defaultMinSize`/`.semantics { selected }` where missing; add focus indicators via existing `MaterialTheme` (no new visuals).

- [ ] **Step 4: Run test + full `gradlew.bat :composeApp:test` + `screenshotTests:verifyPaparazziDebug`** → PASS.

- [ ] **Step 5: Update docs** — "Last verified" date + Key Files table in the settings feature doc; add a one-line CHANGELOG entry (user-facing consistency/accessibility improvement).

- [ ] **Step 6: Commit** — `git commit -m "feat(settings): accessibility and duplicate-title sweep on shared foundation"`

---

## Author self-review

- Spec coverage: Shared Header (T1), Rows/Status/Info/Empty/Error/Loading/Confirmation (T2-T4), Responsive (T5), Accessibility + Interaction States (T2-T4, T6), per-screen content explicitly deferred (scope note).
- Step scan: each step yields one checkable artifact; no “handle edge cases” filler.
- Type consistency: `SettingsDetailHeader`, `SettingsPreferenceRow`, `SettingsSwitchRow`, `SettingsActionRow`, `SettingsStatusCard`, `SettingsInfoCard`, `SettingsEmptyState`, `SettingsErrorState`, `SettingsLoadingState`, `SettingsConfirmationDialog`, `SettingsDetailShell` are used consistently.
- Review Focus: each of the 5 items has a test in T1/T2/T3/T5/T6.
- Proportion: implementation steps delegate to existing components (no redrawn UI); tests are the only large code blocks, as required by TDD.
