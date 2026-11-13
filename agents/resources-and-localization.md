# Compose Resources and Localization Guide

## Resource locations
- Shared Compose Resources are under `composeApp/src/commonMain/composeResources/`.
- The default string resource file is `values/strings.xml`; many localized `values-xx/strings.xml` folders exist.
- Android platform resources are separate under `composeApp/src/androidMain/res/`; do not confuse Android `R.string` resources with generated Compose Resources.

## Locale parity gate (mandatory for locale changes)
- `composeApp/src/desktopTest/kotlin/com/dorai/ai/resources/LocaleParityTest.kt` — XML parse; key parity across **all** `values-*` directories; duplicate key detection; placeholder/plural parity; mojibake scan for Bengali-like sequences.
- `scripts/validate-locales.ps1` — standalone local/CI-runnable validator (`pwsh scripts/validate-locales.ps1`); regex-based; intentional fallback for environments without a JVM test runner.
- `composeApp/src/desktopTest/kotlin/com/dorai/ai/resources/BengaliCanonicalFormTest.kt` — every locale string must be NFC-normalized; the two spellings of য় (`U+09DF` vs `U+09AF U+09BC`) must not be mixed inside one file.
- `composeApp/src/desktopTest/kotlin/com/dorai/ai/resources/LocaleCoverageBaselineTest.kt` — a locale must never **lose** keys it already had (baseline: `scripts/locale-coverage-baseline.json`).
- `scripts/locale-coverage.ps1` — coverage report, per-locale missing-key worklist export (`-Out <file>`) and the same regression check (`-FailOnRegression`).
- Any string added/renamed/removed in `values/strings.xml` must land in every `values-xx/strings.xml` in the same change (or the parity gate goes red). The 53 stale locales are a currently-known drift — do not add new stale locales.
- When adding a new locale: copy `values/strings.xml` wholesale, translate every value, then run the parity gate. Do not drop keys that exist in `values/`.
- Do not normalize Bengali `য়`/`য়` inconsistently; if normalization is needed, do it once with a corpus test covering both forms.
- The parity gate does not bless en/bn as the only supported locales; other locales follow the same rules.
- Reminder: Bengali text edits require UTF-8 integrity (no mojibake repair by regex) — see `docs/development/bengali-language-and-rendering-policy.md`.

## Adding or repairing a string
1. Search `values/strings.xml` for the intended key and inspect nearby naming conventions.
2. Check whether the key is present in the target locale files and whether translations are expected for this feature.
3. Use the generated Compose Resources API already used by the surrounding code (for example `Res.string.*` and `stringResource`).
4. Do not hand-edit generated build output or invent generated package names.
5. Validate resource XML structure, duplicate names, placeholders/format arguments and capitalization.
6. Compile the affected target; verify the UI text in the intended locale when feasible.

## Translation safety
- Preserve placeholder order and meaning across languages.
- Do not overwrite all locale files with English as a shortcut.
- Avoid introducing hard-coded user-facing strings when the surrounding UI is resource-based.
- When a key is renamed or removed, search all usages and all locale files first.

## Device verification (not covered by CI)
- `docs/development/bangla-device-verification-checklist.md` — the language switch, Bengali IME composition, terminal backspace and dynamic-UI items that need real hardware.
- CI/JVM tests can prove the *logic* (locale application on the JVM, grapheme-aware IME delete counts, resource integrity). They cannot prove on-device behaviour; report those items as **pending** or **blocked**, never as passed.