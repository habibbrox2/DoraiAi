# Phase B1 — Theme-scoped immutable color tokens

## Change
- Added immutable `DesignColorTokens` roles for text, surfaces, cards and borders.
- Added `LocalDesignColorTokens`, whose default preserves the existing dark design fallback.
- The root `Theme(colorScheme)` now derives and provides this palette from the exact Material 3 `ColorScheme` supplied to it.
- Added behavior assertions for Light and Dark scheme mapping in `ThemeColorSchemeTest`.

## Compatibility boundaries
- Existing `DesignTokens` spacing, radii, brand accents, semantic status colors and terminal background remain unchanged.
- Existing `LocalDesignTokens` and `KaiThemeColors` APIs remain in place for this incremental migration.
- Persisted `ThemeMode` values remain System/Light/Dark; no serialized preference or settings UI was changed.
- This commit does not yet migrate feature consumers to `LocalDesignColorTokens`, nor remove the mutable compatibility singleton. Those require a separately audited, feature-batched migration.

## Validation
- Required: common unit tests, Desktop tests, Android FOSS compile/unit tests, Quality/Web Assets, Screenshot Regression.
- Do not treat this change as complete until all jobs pass. Fix any compile/test regressions before moving on.
