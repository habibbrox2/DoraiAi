# Phase C — Final Cleanup, Compatibility Removal & Regression Plan

**Date:** 2026-11-02
**Branch:** `feature/2027-premium-agent-ui-migration`
**PR:** #11 (Draft/Open)
**Baseline commit:** latest on branch (post Phase B2 shared-component batch)

## 1. Purpose

Phase C is the **final cleanup and regression-verification phase** that runs after:
- Phase B1 (theme-scoped immutable `DesignColorTokens` + `LocalDesignColorTokens`) ✅ complete & CI green
- Phase B2 consumer migration batches (shared components ✅ in progress; Chat, Settings, Dashboard, Build, Sandbox, DynamicUI, Markdown pending)

Phase C has two goals:
1. **Remove obsolete compatibility APIs** (`KaiThemeColors`, `TextPrimary`/`TextSecondary`/… getters, `LocalKaiColors`) only after zero references are confirmed across all source sets, tests, and previews.
2. **Run the full regression matrix** (theme, layout, screenshot, motion, accessibility) and declare migration complete only when every CI job is green.

No merge, release or deploy is performed in this phase.

## 2. Current migration status (audit)

### 2.1 Compatibility API reference counts (composeApp/src, all source sets)

| API | References | Status |
|---|---|---|
| `KaiThemeColors` | 23 (all in `Theme.kt` definitions/usages) | Still required (default + wiring) |
| `LocalKaiColors` | 2 | Still required (compatibility API) |
| `TextPrimary` (global getter) | 225 total (incl. tests/strings) | Migrating — 18 feature component files migrated |
| `LocalDesignColorTokens` | 18+ | Migrated across Chat, Settings, Sandbox, Shared components |
| `DesignColorTokens` | (in DesignTokens.kt) | Stable |

### 2.2 Phase B2 batch progress

| Batch | Scope | Status |
|---|---|---|
| Shared components | KaiAvatar, KaiSnackbar, OperationalComponents, RefreshIconButton, SettingsListItem, DesktopScrollbar.jvm.kt, KaiDialog, KaiChip | ✅ Migrated (text colors → `LocalDesignColorTokens.current`) |
| Chat | BotMessage, UserMessage, QuestionInput, EmptyState, ToolExecutionCard | ✅ Migrated (text colors → `LocalDesignColorTokens.current`) |
| Settings | GeneralSettings, SettingsNavigation, SettingsDesignTokens, ModelSelectionSheet, SkillAutocomplete, TerminalSheet | ✅ Migrated (text colors → `LocalDesignColorTokens.current`) |
| Sandbox | SandboxTabsContent, SandboxPackagesScreen | ✅ Migrated (text colors → `LocalDesignColorTokens.current`) |
| Dashboard | DashboardScreen | ⏳ Pending |
| Build | ~7 build files | ⏳ Pending |
| DynamicUI / Markdown | KaiUiRenderer, MarkdownRenderer | ⏳ Pending |

### 2.3 What has NOT changed (preserved)

- `DesignTokens` static layout/brand/status/terminal constants — untouched.
- `KaiTypography`, `KaiShapes`, `KaiBorders` — untouched.
- `LocalDesignTokens` static composition local — untouched.
- Persisted `ThemeMode` (System/Light/Dark), key `"theme_mode"`, Android system bar behavior — untouched.
- `useOLED` parameter — untouched (audit separately before removal decisions).
- Semantic status colors, code editor, Diff, Markdown, Terminal/ANSI palettes — kept separate.
- No new Midnight/OLED theme mode added.

## 3. Phase C audit checklist (read-only, must pass before removal)

Run these before any removal:

1. **Reference search** across `composeApp/src` (all source sets), `screenshotTests`, and `androidApp`:
   - `KaiThemeColors`
   - `LocalKaiColors`
   - `TextPrimary` / `TextSecondary` / `TextTertiary` / `TextMuted` (top-level getters, not `DesignColorTokens` fields)
   - `SurfaceElevatedTheme` / `CardSurfaceTheme` / `BorderStrongTheme` (if any)
   - `@Preview` functions referencing the above
2. **Confirm zero references** in: `commonMain`, `androidMain`, `iosMain`, `desktopMain`, `wasmJsMain`, `commonTest`, `desktopTest`, `androidApp`, `screenshotTests`.
3. **Confirm temporary markers** (`RECORD_GOLDENS`, bootstrap commits, `// TODO` migration markers) are absent or intentionally retained.
4. **Confirm no platform-specific references** remain (especially `desktopMain` scrollbar and iOS/wasm stubs).

## 4. Phase C removal rules (only after audit passes)

- Remove `KaiThemeColors` object **only** if zero references (audit §3.1–3.2).
- Remove `TextPrimary`/`TextSecondary`/`TextTertiary`/`TextMuted` global getters **only** if zero references.
- Remove `LocalKaiColors` **only** if zero references.
- Remove `SurfaceElevatedTheme`/`CardSurfaceTheme`/etc. **only** if zero references.
- Keep `DesignTokens` (static) and `LocalDesignTokens` permanently.
- Keep `KaiTypography`, `KaiShapes`, `KaiBorders` permanently.
- Do **not** remove `useOLED` without a separate audit (tracked separately).
- Each removal is a separate commit; run CI after each.

## 5. Phase C regression matrix (must be green)

| Category | Required checks |
|---|---|
| Theme/root | System/Light/Dark follow correct ColorScheme; `DesignColorTokens` derived from Material scheme; persistence round-trip; invalid/missing value → System fallback; no mutable global palette leakage in concurrent composition; Android system bar appearance correct |
| Chat | empty/message/streaming/error/tool states (running/completed/failed), approval pending, expanded/collapsed; phone/tablet/desktop widths; keyboard visible |
| Composer | empty/draft/multiline, attachment, slash/skill menu, sending/stopping, disabled; narrow width, large font, focus traversal |
| Model selection | loading/loaded/selected/empty/provider error/retry/free badge/score bands; compact & wide |
| Settings | all 9 tabs, selected tab, scroll clipping, loading/error/unavailable; compact selector, 840dp rail, large font, Bengali/long labels |
| Dashboard | readiness, missing services, empty/recent conversations, projects; 1/2/4 columns |
| Build | setup/installing/progress/cancel/error, project list, active session, terminal, unavailable; phone/tablet/desktop |
| Sandbox | setup/installing/failed/retry, terminal, files, packages, find, replace confirmation; compact/wide + destructive confirmation |
| Markdown | headings/lists/tables/code/copy/diff/math/invalid fallback/streaming; long code, horizontal overflow, light/dark |
| Dynamic UI | valid/legacy/malformed/oversized/interactive/historical read-only; action enabled/disabled, error fallback |
| Shared controls | hover/focus/pressed/selected/disabled/error/loading; mouse/keyboard/touch; 48dp minimum |
| Motion | normal + reduced motion; verify infinite/looping animation policy |
| Screenshots | `PremiumScreenScreenshotTest`, `KaiUiScreenshotTest`, `ScreenshotTest`, `MathScreenshotTest`, store/tablet suites — intentional golden diffs reviewed, never blanket-accepted |
| Spotless | `spotlessCheck` green |
| Tests | common unit tests, desktop tests, Android FOSS compile + unit tests |

## 6. Phase C execution sequence

1. Finish remaining Phase B2 feature batches (Chat → Settings → Dashboard → Build → Sandbox → DynamicUI/Markdown), one batch per commit, CI green after each.
2. Run the full audit checklist (§3) and record reference counts in this doc (update §2.1).
3. If zero references confirmed, remove obsolete APIs per §4, one removal per commit, CI after each.
4. Remove temporary markers / obsolete artifacts (verify no references).
5. Run one final CI workflow (all jobs green).
6. Declare migration complete only after all jobs green. Do **not** merge PR #11, release or deploy without separate approval.

## 7. Safety constraints during Phase C

- Never weaken SSRF validation, shell allowlisting, Android network security, encrypted storage, backup exclusions or release-integrity checks.
- Do not add dependencies/plugins to mask compile errors.
- Do not modify source/build/config without explicit authorization (this doc is planning only).
- One CI workflow at a time; wait for completion before next commit.
- Preserve all pre-existing user edits; never reset/stash/overwrite unrelated files.
- PR #11 remains Draft/Open; no merge/release/deploy.

## 8. Phase C status

**Status: Chat, Settings, Sandbox, and Shared component batches migrated to `LocalDesignColorTokens.current`.** Screenshot tests recorded and passing (`:screenshotTests:test` green). Remaining Phase B2 batches (Dashboard, Build, DynamicUI/Markdown) still pending before cleanup/removal can begin. This document will be updated with reference counts and verification results as work progresses.
