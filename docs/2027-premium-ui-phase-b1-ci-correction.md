# Phase B1 CI correction record

The CI run for the initial theme-token implementation identified:
- A non-infix invocation of the Compose Color luminance extension in `DesignTokens.kt`.
- Spotless import-order violations in `Theme.kt` and `ThemeColorSchemeTest.kt`.

Corrective commit: `a9670bf60f018ab054a80a2522dea1b07a84c115`.

The correction changes only the extension-call syntax and import ordering. It does not change theme mapping, persisted settings, UI behavior, or compatibility APIs.

Phase B2 remains blocked until all validation jobs pass.
