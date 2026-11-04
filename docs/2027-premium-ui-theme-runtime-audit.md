# 2027 Premium UI — Theme Runtime Audit Addendum

**Branch:** `feature/2027-premium-agent-ui-migration`
**Audited revision:** `04438de6f94fcd5c1a646a78a5f9522872d5209e`
**Scope:** Read-only verification of theme mode persistence, runtime composition, legacy scheme references, and token consumers.
**Status:** Audit correction; no application source changed.

## 1. Verified runtime contract (source takes precedence over stale docs)

The current implementation exposes exactly three persisted modes:

- `composeApp/src/commonMain/kotlin/com/dorai/ai/data/AppSettings.kt`: `ThemeMode = System, Light, Dark`.
- Persistence key is `theme_mode`; `setThemeMode` stores `mode.name` and updates `_themeModeFlow`.
- `loadInitialThemeMode()` parses the stored enum name and falls back to `System` for missing or invalid values.
- `composeApp/src/commonMain/kotlin/com/dorai/ai/App.kt`: `AppContent` observes `themeModeFlow`; System resolves against `isSystemInDarkTheme()`, Light selects the supplied light scheme, Dark selects the supplied dark scheme. The resulting scheme is passed to `Theme(colorScheme = effectiveColorScheme)`.
- `androidApp/src/main/kotlin/com/dorai/ai/MainActivity.kt`: system-bar icon/style calculation currently maps System and Light through system appearance, and Dark to dark UI. This is a separate platform edge-to-edge calculation and must be kept consistent with the actual selected scheme.
- `GeneralSettings.kt`: picker currently presents only System, Light and Dark.
- `SettingsUiState.kt`, `SettingsActions.kt`, `SettingsViewModel.kt`, `DataRepository.kt`, `RemoteDataRepository.kt`, and `FakeDataRepository.kt` carry the same enum through settings state and persistence.

**Correction:** `docs/design-system/theme-contract.md` and `docs/features/appearance.md` describe Midnight and OLED Black, but these modes are not represented in the current `ThemeMode` enum, settings picker, App runtime switch, or Android system-bar switch. Do not implement those modes or assume they are persisted without an explicit product decision and a migration plan. Current runtime truth is System/Light/Dark.

## 2. Theme composition and token divergence

- `ui/Theme.kt` defines `KaiColorPalette`, `LocalKaiColors`, and mutable singleton `KaiThemeColors`.
- `Theme(colorScheme)` infers light-vs-dark from `colorScheme.background.luminance()`, constructs a `KaiColorPalette`, copies every field into mutable `KaiThemeColors`, then provides `LocalKaiColors` and `LocalDesignTokens`.
- The `LocalDesignTokens` provider currently supplies the static singleton `DesignTokens`, independent of the selected Material scheme.
- `ui/KaiAppTheme.kt` also provides static `DesignTokens`. Its `useOLED` argument is currently unused. The wrapper is not the root persisted-mode adapter used by `App.kt`.
- `LocalKaiColors` has no discovered consumer outside its definition/provider in `Theme.kt`; the global compatibility getters (`TextPrimary`, `TextSecondary`, `TextTertiary`, `TextMuted`, and theme-suffixed surface/border getters) are used broadly.
- `KaiTypography`, `KaiShapes`, and `KaiBorders` are defined in `Theme.kt`; they are independent shared primitives and must not be deleted as part of the color-state migration.

## 3. Search-backed consumer inventory

GitHub code search on the audited branch returned these consumer groups:

- `TextPrimary`: at least 50 matching files in the result window, spanning shared components, chat, settings, build, dashboard, sandbox, Markdown, and dynamic UI. This is a broad compatibility surface; migrate by bounded feature batches, not a blind replacement.
- `TextSecondary`: at least 48 matching files across the same feature areas.
- `TextTertiary`: 6 matching files.
- `TextMuted`: 3 matching files.
- `DesignTokens`: at least 33 matching files, including shared components, chat, settings, build, sandbox, dashboard and tests.
- `KaiShapes`: at least 27 matching files.
- `KaiBorders`: 5 matching files.
- `LocalDesignTokens`: direct consumers include `KaiSearchField.kt`, `ConversationRail.kt`, `AppSidebar.kt`, and `ChatScreen.kt`; provider/default and tests also reference it.
- No direct `LocalKaiColors.current` consumer was returned by code search; it is still part of the theme provider API and should be checked by a complete branch search before removal.
- Theme-suffixed getters such as `SurfaceElevatedTheme`, `SurfaceRaisedTheme`, `CardFillTheme`, `CardSurfaceTheme`, and border variants currently resolve through `KaiThemeColors` and are only found in `Theme.kt` in the search result. Confirm via repository-wide search before removing them.

The search result count is a bounded search result window, not a mathematically complete total of symbol occurrences. The file paths are a practical migration inventory; verify exact references again immediately before deleting compatibility APIs.

## 4. Scheme definitions vs active runtime

`Theme.kt` defines additional named palettes including Lilac, Dusk, Seafoam, Graphite, Daybreak and Carbon. Search found no corresponding persisted enum values or settings picker options. Some are imported into `App.kt`; before deleting or retaining them as supported themes, inspect all imports, call sites, previews and screenshot fixtures. Their existence alone does not establish user-selectable runtime support.

## 5. Test gap and required implementation tests

Current `ThemeColorSchemeTest.kt` contains tautological Light/Dark assertions (each compares a literal color with itself), checks static token constants, and verifies only that the CompositionLocal object exists. It does not verify runtime selection, token/scheme coherence, persistence round-trips, or Android system-bar mode mapping.

Before removing compatibility state, add behavior tests for:
1. System mode resolves to the supplied platform scheme under both system-light and system-dark.
2. Light and Dark resolve to the correct supplied schemes.
3. The CompositionLocal token palette matches the selected scheme and preserves semantic text/status/border roles.
4. Invalid/missing persisted enum values safely fall back to System; existing stored System/Light/Dark values remain readable.
5. Parallel compositions/previews cannot leak palette values through global mutable state.
6. `KaiAppTheme(useOLED = ...)` call sites are inventoried; preserve source compatibility or explicitly deprecate the unused argument only after proving no external API requirement.
7. Android system-bar style remains correct for all supported modes.

## 6. Implementation guardrails

- Do not add Midnight/OLED modes as a side effect of the token refactor; first reconcile the stale docs with the intended product contract.
- Preserve the three current serialized enum names and the `theme_mode` key.
- Derive the immutable token palette from the same effective `ColorScheme` used by `MaterialTheme`.
- Replace global getters with composition-local reads in controlled batches; do not remove them until all source, tests, previews and platform source sets have zero references.
- Keep semantic status, code editor, diff, Markdown and terminal/ANSI palettes distinct from brand/surface tokens.
- Keep `KaiTypography`, shape semantics, focus, selected/disabled states, touch targets, navigation and all business behavior unchanged.
- The `useOLED` parameter is presently unused; do not infer that an OLED theme exists in runtime.
