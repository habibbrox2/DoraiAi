# Appearance

**Last verified:** 2026-11-05

DORAI AI ships a 2027 premium dynamic theme engine that runs identically on Android, iOS, Desktop (JVM) and Web (Wasm/JS). Appearance is configured from **Settings → Appearance** with a grid of live preview swatches; changes apply immediately — no restart, no Gradle rebuild.

## Canonical Hexacore Brand Colors

The visual identity is anchored on the canonical hexacore palette and brand gradient:

- **Obsidian Dark Canvas**: `#050A1C` — root workspace background.
- **Brand Cyan**: `#25E2F5` — primary brand highlight and focus state.
- **Brand Blue**: `#377CFF` — mid-spectrum gradient anchor.
- **Brand Violet**: `#6F5BFF` — deep agent gradient terminal and cognitive accents.
- **Brand Gradient**: `#25E2F5` (0%) → `#377CFF` (48%) → `#6F5BFF` (100%).

## Built-in Presets

The engine ships thirteen built-in presets in `ThemePresets.kt`, each defining `id`, `displayName`, `isDark`, `background`, `surface`, `surfaceElevated`, `surfaceRaised`, `primary`/`accent`, and semantic text/outline tiers:

1. **Dark (`dark`)** — premium obsidian default: background `#050A1C`, surface `#08132B`, primary cyan `#25CDE2`, accent violet `#8B78FF`.
2. **Light (`light`)** — clean light neutral: background `#FFFFFF`, surface `#FFFFFF`, primary blue `#245FD8`, accent `#377CFF`.
3. **OG Shine (`og_shine`)** — deep plum obsidian: background `#14082B`, surface `#1E1038`, shine-magenta primary `#FF2E9A`, gold accent `#FFC24B`.
4. **Peach (`peach`)** — warm soft light: background `#FFF4EC`, surface `#FFFFFF`, peach primary `#D9531E`, amber accent `#B45309`.
5. **Midnight (`midnight`)** — deep brand navy: background `#06152E`, surface `#0E2A4D`, cyan primary `#25E2F5`, blue accent `#377CFF`.
6. **Lilac (`lilac`)** — soft purple light: background `#FAF7FF`, surface `#F3F0FF`, violet primary `#6B46C1`, accent `#8B5CF6`.
7. **Dusk (`dusk`)** — sunset dark: background `#0F0A19`, surface `#1E1625`, amber primary `#F59E0B`, purple accent `#A855F7`.
8. **Seafoam (`seafoam`)** — mint fresh light: background `#F0FDF4`, surface `#ECFDF5`, emerald primary `#047857`, green accent `#10B981`.
9. **Graphite (`graphite`)** — neutral dark slate: background `#0F0F0F`, surface `#1A1A1A`, slate primary `#94A3B8`, accent `#64748B`.
10. **Daybreak (`daybreak`)** — morning sky light: background `#F8FAFF`, surface `#F1F5FF`, royal blue primary `#2563EB`, accent `#3B82F6`.
11. **Sherbet (`sherbet`)** — berry pastel light: background `#FFF7FB`, surface `#FEF2F8`, raspberry primary `#D91C78`, pink accent `#F472B6`.
12. **Paper (`paper`)** — warm editorial light: background `#FAF7F2`, surface `#F5F3F0`, warm brown primary `#92400E`, terra-cotta accent `#D97757`.
13. **Carbon (`carbon`)** — OLED true-black: zero-black base background `#000000`, surface `#0A0A0A`, luminous blue primary `#60A5FA`, accent `#3B82F6`.

User-created custom themes sit alongside the presets in the same grid and are marked with a remove badge.

## Match Device (System Sync) & WCAG AA Contrast Guard

The **Match Device** switch synchronizes dark/light modes with the OS setting (`isSystemInDarkTheme()`) while retaining the selected accent.

### WCAG 2.1 Level AA Rules

- **Contrast Requirement**: Standard text requires at least **4.5:1** contrast against its background; large text and interactive UI elements require at least **3.0:1**.
- **Luminance Guard in `resolve()`**: When Match Device is active, the engine ensures that a user's chosen accent is only carried over if `selected.isDark == base.isDark`. If there is a luminance mismatch (for instance, applying a dark theme's accent onto a light background, or vice versa), the base pair's own accent is used instead. This mathematical guard prevents unreadable low-contrast combinations.
- **Formulas**: Contrast is computed in `ThemeModels.kt` via standard WCAG 2.1 relative luminance and contrast ratio formulas.

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
| `selected_theme_id` | Preset id (e.g. `dark`, `midnight`, `carbon`) or a `custom_*` id |
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
| `docs/design-system/theme-contract.md` | Source-of-truth theme contract and token definitions |
| `composeApp/.../theme/data/ThemeModels.kt` | `ThemePalette`, `ThemeStyle`, `ThemeState`, `resolve()`, WCAG luminance & contrast calculations, hex utilities |
| `composeApp/.../theme/data/ThemePresets.kt` | The thirteen built-in presets (`dark`, `light`, `og_shine`, `peach`, `midnight`, `lilac`, `dusk`, `seafoam`, `graphite`, `daybreak`, `sherbet`, `paper`, `carbon`) |
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
| `composeApp/.../ui/theme/DesignTokens.kt` | AURORA brand tokens (`BrandCyan`, `BrandBlue`, `BrandViolet`, etc.) |
| `composeApp/.../theme/ThemeEngineTest.kt` | Tests preset resolution, Match Device luminance guard, persistence |
| `composeApp/.../theme/PresetContrastTest.kt` | WCAG 2.1 AA contrast ratio tests for all 13 presets |
