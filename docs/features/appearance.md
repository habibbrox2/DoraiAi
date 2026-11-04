# Appearance

**Last verified:** 2026-11-04

DORAI AI ships a 2027 premium dynamic theme engine that runs identically on Android, iOS, Desktop (JVM) and Web (Wasm/JS). Appearance is configured from **Settings → Appearance** with a grid of live preview swatches; changes apply immediately — no restart, no Gradle rebuild.

## Built-in presets

The engine ships thirteen built-in presets, each defining `id`, `displayName`, `isDark`, `background`, `surface`, `primary`/`accent` and semantic text/outline tiers:

- **Dark** — premium obsidian default: background `#050A1C`, surface `#08132B`, cyan primary `#25CDE2`.
- **Light** — background `#FFFFFF`, blue primary `#245FD8`.
- **OG Shine** (new) — deep plum `#14082B` with shine-magenta `#FF2E9A` and gold `#FFC24B`.
- **Peach** (new) — warm off-white with peach `#FF7A45`.
- **Midnight** — brand navy `#06152E` with cyan accent (per the former appearance contract).
- **Lilac**, **Dusk**, **Seafoam**, **Graphite**, **Daybreak**, **Sherbet**, **Paper**, **Carbon** — promoted from the legacy `Theme.kt` schemes to full surface/text/outline tiers.

User-created custom themes sit alongside the presets in the same grid and are marked with a remove badge.

## Match Device (system sync)

The **Match Device** switch synchronizes dark/light modes with the OS setting (`isSystemInDarkTheme()`) while retaining the selected accent. When the selected palette belongs to a different luminance class than the OS-chosen base, the base pair's own accent is used instead — a dark accent on a light canvas (or the reverse) is a contrast failure.

## Surface style

A **Flat / 3D** toggle controls premium surfaces via `Modifier.doraiSurfaceStyle(style, shape)`:

- **Flat** — filled surface tier with a 1dp hairline outline.
- **3D** — neumorphic depth: dual-toned outer shadow, vertical light gradient across the fill and a faint white top-edge highlight. Suitable for Web (Wasm), Desktop, Android and iOS.

## User-created themes

The **Create theme** builder takes a name plus three opaque HEX values (`background`, `surface`, `primary`). Each field shows a live RGB readout; Save stays disabled until all three parse. Custom palettes are stored locally under `custom_themes_json` and selected immediately (Match Device turns off).

## Persistence

Engine state lives in `multiplatform-settings` (same store as the rest of app settings):

| Key | Meaning |
|---|---|
| `selected_theme_id` | Preset id (e.g. `dark`, `midnight`) or a `custom_*` id |
| `is_system_theme` | Match Device switch |
| `theme_style` | `Flat` / `ThreeD` |
| `custom_themes_json` | JSON array of custom palettes |

An existing legacy `theme_mode` (`System` / `Light` / `Dark`) migrates one-time into the new keys on first launch after the upgrade. The engine also writes `theme_mode` in tandem so Android system-bar icons keep their correct style — dark selection → dark bars, light selection → light bars, Match Device → system-driven.

## Behavior

- **Reactivity** — selections recompose the whole app immediately; no app restart.
- **System bars (Android)** — edge-to-edge; icon style follows the resolved theme as before.
- **Material You** — dynamic wallpaper colors are **no longer applied**. The selected preset or custom theme is the app's identity on every platform. This is a deliberate change; if wallpaper-derived colors are wanted in Match Device on Android 12+, that needs an explicit product decision.
- **KaiAppTheme** — removed. The engine root is `DoraiTheme` (`DoraiThemeEngine.kt`).

## Key Files

| File | Purpose |
|------|---------|
| `composeApp/.../theme/data/ThemeModels.kt` | `ThemePalette`, `ThemeStyle`, `ThemeState`, `resolve()`, hex utilities |
| `composeApp/.../theme/data/ThemePresets.kt` | The thirteen built-in presets |
| `composeApp/.../theme/data/ThemeSettingsRepository.kt` | Persistence (`selected_theme_id`, `is_system_theme`, `theme_style`, `custom_themes_json`), legacy `theme_mode` migration + tandem sync |
| `composeApp/.../theme/engine/DoraiThemeEngine.kt` | `DoraiTheme` root; maps palette → Material 3 `ColorScheme`, provides `LocalDoraiPalette`/`LocalDoraiThemeState`/`LocalDoraiThemeStyle` and `LocalDesignColorTokens` |
| `composeApp/.../theme/engine/ElevationStyles.kt` | `Modifier.doraiSurfaceStyle(style, shape)` — Flat vs 3D surfaces |
| `composeApp/.../theme/ThemeViewModel.kt` | `StateFlow<ThemeState>` holder for the Appearance UI |
| `composeApp/.../theme/ui/AppearanceSettingsScreen.kt` | Match Device, Flat/3D, preset grid, Create theme entry |
| `composeApp/.../theme/ui/components/ThemeSwatchCard.kt` | Preview swatch with mockup bars, accent dots, accent pill, spring selection ring |
| `composeApp/.../theme/ui/components/CreateThemeDialog.kt` | Hex/RGB theme builder dialog |
| `composeApp/.../App.kt` | Root `DoraiTheme` wrapper (was `Theme(colorScheme = ...)`) |
| `composeApp/.../ui/settings/AppearanceSettings.kt` | Appearance tab adapter |
| `composeApp/.../AppModule.kt` | Koin registrations (`ThemeSettingsRepository` singleton, `ThemeViewModel`) |
