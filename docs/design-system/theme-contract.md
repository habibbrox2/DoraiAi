# DORAI AI Theme Contract

This document is the source-of-truth contract for DORAI AI's 2027 live theme engine across Compose Multiplatform (Android, iOS, Desktop JVM, and Web/Wasm).

## Canonical Hexacore Brand Identity

The core visual identity of DORAI AI is established by four canonical hexacore colors and an authoritative brand gradient:

| Color Name | Hex Code | Purpose & Semantic Role |
|---|---|---|
| **Obsidian Base** | `#050A1C` | Canvas/page background for standard dark workspace |
| **Brand Cyan** | `#25E2F5` | Primary brand accent, glow highlight, and active focus state |
| **Brand Blue** | `#377CFF` | Mid-spectrum gradient anchor and secondary interactive tone |
| **Brand Violet** | `#6F5BFF` | Deep agent gradient terminal and cognitive highlights |

### Brand Gradient

The canonical D-mark and brand artwork employ a linear multi-stop gradient:

$$\text{Gradient: } \#25E2F5 \ (0\%) \longrightarrow \#377CFF \ (48\%) \longrightarrow \#6F5BFF \ (100\%)$$

### Obsidian Dark Workspace Surface Tiers

In addition to the core brand colors, the default obsidian workspace (`Dark` preset and `DesignTokens`) uses a structured elevation hierarchy:

| Surface Tier | Token | Hex Code | Visual Hierarchy |
|---|---|---|---|
| Canvas Background | `DesignTokens.Background` | `#050A1C` | App canvas and root background |
| OLED Background | `DesignTokens.BackgroundOled` | `#000000` | True zero-black base for OLED/AMOLED panels |
| Terminal Background | `DesignTokens.TerminalBackground` | `#070D20` | Monospace code/terminal wells |
| Base Surface | `DesignTokens.SurfaceDark` | `#08132B` | Standard cards, panels, sidebars |
| Elevated Surface | `DesignTokens.SurfaceElevated` | `#0D1C38` | Floating cards, modal sheets, popovers |
| Raised Surface | `DesignTokens.SurfaceRaised` | `#13284A` | Interactive pills, inputs, focus wells |
| Primary Accent | `DesignTokens.PrimaryCyan` | `#25CDE2` | Interactive actions, active selection |
| Agent Accent | `DesignTokens.AgentViolet` | `#8B78FF` | Agent activity, streaming indicators |

---

## The 13 Built-in Presets

The 2027 theme engine ships thirteen built-in presets in `ThemePresets.kt`. Each preset defines complete surface, background, primary/accent, text, and outline tiers:

| Preset ID | Display Name | Luminance Class | Background | Surface | Surface Elevated | Surface Raised | Primary Accent | Secondary Accent |
|---|---|---|---|---|---|---|---|---|
| `dark` | Dark (Obsidian Default) | Dark | `#050A1C` | `#08132B` | `#0D1C38` | `#13284A` | `#25CDE2` | `#8B78FF` |
| `light` | Light | Light | `#FFFFFF` | `#FFFFFF` | `#EBF1F7` | `#E2E8F0` | `#245FD8` | `#377CFF` |
| `og_shine` | OG Shine | Dark | `#14082B` | `#1E1038` | `#29184A` | `#35205E` | `#FF2E9A` | `#FFC24B` |
| `peach` | Peach | Light | `#FFF4EC` | `#FFFFFF` | `#FFE8D6` | `#FFD8BF` | `#D9531E` | `#B45309` |
| `midnight` | Midnight | Dark | `#06152E` | `#0E2A4D` | `#173A5E` | `#1F4A73` | `#25E2F5` | `#377CFF` |
| `lilac` | Lilac | Light | `#FAF7FF` | `#F3F0FF` | `#EDE9FE` | `#E0D9FA` | `#6B46C1` | `#8B5CF6` |
| `dusk` | Dusk | Dark | `#0F0A19` | `#1E1625` | `#241A33` | `#2D2138` | `#F59E0B` | `#A855F7` |
| `seafoam` | Seafoam | Light | `#F0FDF4` | `#ECFDF5` | `#D1FAE5` | `#A9F0D1` | `#047857` | `#10B981` |
| `graphite` | Graphite | Dark | `#0F0F0F` | `#1A1A1A` | `#252525` | `#2E2E2E` | `#94A3B8` | `#64748B` |
| `daybreak` | Daybreak | Light | `#F8FAFF` | `#F1F5FF` | `#E0E7FF` | `#C7D2FE` | `#2563EB` | `#3B82F6` |
| `sherbet` | Sherbet | Light | `#FFF7FB` | `#FEF2F8` | `#FCE7F3` | `#FBCFE8` | `#D91C78` | `#F472B6` |
| `paper` | Paper | Light | `#FAF7F2` | `#F5F3F0` | `#E7E5E4` | `#D6D3D1` | `#92400E` | `#D97757` |
| `carbon` | Carbon (OLED Black) | Dark | `#000000` | `#0A0A0A` | `#111111` | `#1A1A1A` | `#60A5FA` | `#3B82F6` |

### Special Presets Note

- **Carbon (`carbon`)**: True black base surfaces (`#000000`) designed for OLED/AMOLED efficiency, with raised tiers (`#0A0A0A`, `#111111`, `#1A1A1A`) preserving UI hierarchy and clear borders.
- **Midnight (`midnight`)**: Deep navy brand palette (`#06152E` background, `#0E2A4D` surface) with canonical cyan `#25E2F5` primary and blue `#377CFF` accent. Midnight is distinct from Carbon/OLED.

---

## Live Runtime Architecture

Theme state is managed reactively and multiplatform-wide without restarting the application:

1. **Root Composition**: `DoraiTheme` (`DoraiThemeEngine.kt`) wraps the UI hierarchy, injecting the resolved `ColorScheme` into `MaterialTheme`.
2. **State Storage**: `ThemeSettingsRepository` persists preferences in `multiplatform-settings`:
   - `selected_theme_id` — ID of chosen preset or custom palette (`custom_*`).
   - `is_system_theme` — Match Device toggle (follows OS appearance).
   - `theme_style` — Surface treatment: `Flat` or `ThreeD`.
   - `custom_themes_json` — Serialized custom palettes created via the theme builder.
3. **Legacy Migration**: Older `theme_mode` (`System`, `Light`, `Dark`) settings migrate seamlessly to `selected_theme_id` + `is_system_theme` on initial startup, while keeping tandem writes for Android system bar management.
4. **Composition Locals**:
   - `LocalDoraiThemeState` — Global state object (`ThemeState`).
   - `LocalDoraiPalette` — The active rendered `ThemePalette`.
   - `LocalDoraiThemeStyle` — Active surface treatment (`Flat` or `ThreeD`).
   - `LocalDesignColorTokens` — Scheme-aware semantic tokens (`from(ColorScheme)`).
   - `LocalDesignTokens` — Static Aurora constants for backward compatibility.

---

## WCAG 2.1 AA Accessibility & Contrast Rules

The theme engine strictly satisfies WCAG 2.1 Level AA contrast guidelines across all 13 presets and custom user themes:

### 1. Contrast Ratios

- **Normal Text (< 18pt or < 14pt bold)**: Minimum contrast ratio of **4.5:1** against the underlying background or surface.
  - Dark presets pair `#050A1C` / `#08132B` / `#000000` with high-luminance text (`#F1F8FF`, `#E6F4FF`, `#E5E5E5`), yielding contrast ratios > **12:1**.
  - Light presets pair `#FFFFFF` / `#FFF4EC` with deep foreground text (`#000000`, `#1E1B4B`, `#3B2B23`), yielding contrast ratios > **13:1**.
- **Large Text (≥ 18pt or ≥ 14pt bold)**: Minimum contrast ratio of **3.0:1**.
- **UI Components & Graphical Objects**: Minimum contrast ratio of **3.0:1** for borders, focus indicators, icons, and button states against adjacent backgrounds.

### 2. Match Device Luminance Guard

In `ThemeState.resolve(systemInDark)`:

- When **Match Device** is active (`isSystemTheme = true`), the base surface pair (`Dark` or `Light`) tracks the OS appearance.
- The user's selected accent is preserved **only if** the selected palette shares the same luminance class as the OS base:
  $$\text{selected.isDark} == \text{base.isDark}$$
- If a user selects a light preset (e.g. `peach`) but the device is in dark mode, applying a dark accent on an obsidian canvas risks a contrast failure. The engine automatically enforces the base pair's validated primary/accent tokens in cross-luminance scenarios, preventing contrast degradation.

### 3. Contrast Calculation Specifications

The engine implements exact relative luminance ($L$) and contrast ratio calculations in `ThemeModels.kt`:

$$L = 0.2126 \cdot R_{\text{linear}} + 0.7152 \cdot G_{\text{linear}} + 0.0722 \cdot B_{\text{linear}}$$

$$\text{Contrast Ratio} = \frac{L_{\text{lighter}} + 0.05}{L_{\text{darker}} + 0.05}$$

### 4. OLED Black Rules

- Carbon (`carbon`) maintains `#000000` for base canvas and lowest surface containers.
- Foreground text and high-contrast accents (`#60A5FA`, `#3B82F6`) provide contrast ratios well exceeding **7:1** (WCAG AAA level), avoiding eye strain.
- Outlines and container borders remain visible with dedicated `outline` (`#3B82F6`) and `outlineVariant` (`#1E3A8A`) values.

---

## Verification Matrix

- **Unit Tests**:
  - `ThemeEngineTest.kt`: Verifies all 13 preset IDs, hex parsing, fallback behavior, Match Device accent resolution, and JSON round-trips.
  - `ThemeColorSchemeTest.kt`: Verifies canonical hexacore colors, `DesignTokens` integrity, and `DesignColorTokens` derivation.
  - `PresetContrastTest.kt`: WCAG 2.1 AA contrast ratio verification across all 13 presets (normal text ≥ 4.5:1, non-text UI ≥ 3.0:1, body text ≥ 4.5:1).
- **Contrast Verification**: Verified via `relativeLuminance()` and `contrastRatio()` unit validations.
- **Platform Verification**: Desktop JVM, Android, iOS, and Web (Wasm) render identically without restart.
