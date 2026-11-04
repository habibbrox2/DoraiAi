# DORAI AI Theme Contract

This document is the source-of-truth contract for the shared Compose Multiplatform color schemes.

## Theme modes

| Mode | Background | Surface | Intent |
|---|---|---|---|
| System | Platform-selected | Platform-selected | Follow OS appearance |
| Light | #FFFFFF | #F8FAFC | Light neutral surfaces |
| Dark | #121212 | Material dark neutral | Standard dark appearance |
| Midnight | #050810 | #0B1026 | Brand navy workspace with cyan accents |
| OLED Black | #000000 | #000000 | True black base surfaces for OLED/AMOLED displays |

## Brand palette

- Brand navy: #0B1026
- Deep navy: #050810
- Charcoal: #151B2B
- Cyan accent: #22D3EE
- Elevated navy surface: #0A0F1C
- Raised navy surface: #111827

## Runtime mapping

App.kt maps persisted ThemeMode values to exactly one shared scheme:

- ThemeMode.System → system-selected scheme
- ThemeMode.Light → LightColorScheme
- ThemeMode.Dark → DarkColorScheme
- ThemeMode.Midnight → MidnightColorScheme
- ThemeMode.OledBlack → OledColorScheme

Do not approximate OLED mode by changing only the background. The background, base surface, and lowest surface containers must remain #000000; elevated cards may use a distinct raised surface so hierarchy remains visible.

Midnight is not OLED. Its background must remain deep navy and its primary content surface must use brand navy. Do not classify it as OLED when selecting card treatments or other surface behavior.

## Implementation rules

1. Prefer shared tokens from Theme.kt over local duplicate color literals.
2. Keep semantic tokens distinct from component-specific tokens. A component token should reference a semantic token when the visual intent is identical.
3. Avoid using the OLED black token as ordinary dark-theme text unless the foreground sits on a sufficiently bright accent surface.
4. Keep text, focus indicators, borders, and selected states distinguishable on both Midnight and OLED backgrounds.
5. Any change to a theme mode must update the runtime mapping, color assertions, and screenshot coverage together.

## Verification matrix

- Unit assertions: OLED zero-black base surfaces; Midnight navy surfaces; standard Dark not misclassified as OLED.
- Screenshot coverage: Chat and Settings on phone and tablet for Light, Dark, Midnight, and OLED.
- Accessibility review: selected navigation semantics, keyboard focus visibility, and text/border contrast on all dark schemes.
