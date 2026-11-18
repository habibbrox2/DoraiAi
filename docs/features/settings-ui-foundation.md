# Settings UI/UX Foundation

**Last verified:** 2026-11-18

DOR AI settings screens share one consistent, accessible, reusable foundation so every detail page (General, Agent, Providers, Tools, MCP) fills in content instead of reinventing UI.

## Behavior

### Shared header

Every Settings detail page opens with the same shared header: a back button when a parent screen exists, a category icon in a filled pill, the screen title announced as a semantic heading, an optional one-line description, and contextual actions on the right. Back navigation always returns to the Settings home, never to a different category.

### Shared rows

Settings content uses a small set of shared rows:

- **Preference row**: title, optional subtitle, optional leading icon, full-width tap target that navigates to a sub-screen.
- **Switch row**: title and description with the whole row as one switch target, so the label and control are announced together. Minimum 48dp touch height.
- **Action row**: title, optional subtitle, and a labelled action button (primary, secondary, destructive, or outlined variant).

Every interactive control meets the 48dp minimum touch target and exposes the correct accessibility role (`Role.Button`, `Role.Switch`, `Role.RadioButton`).

### Shared states

Screens render real states, never placeholder or fake data:

- **Status card**: a labelled status value with a semantic color tone.
- **Info card**: an informational banner.
- **Empty state**: icon, title, helper text, and an optional primary call-to-action; shown when a section genuinely has no data.
- **Error state**: an error banner with an optional retry action, announced politely to screen readers.
- **Loading state**: a progress indicator with an optional label; no invented content appears while loading.

### Responsive layout

Settings reuse the existing adaptive list/detail scaffold:

- **Compact** (under 600dp): a single scrollable detail column; category cards on the home and a fixed header above each detail page.
- **Medium and wider**: a category list pane on the left plus the detail pane on the right, with category selection driving the detail content. Scroll position resets per category.

### Duplicate titles

A category heading and the card beneath it never repeat the same title. The title appears once, at the page header.

### Interaction states

Buttons, switches, and rows show pressed, focused, disabled, loading, error, and success states through color and semantics, not color alone. Destructive actions use the error tone and require a confirmation dialog with undo where data is removed.

## Accessibility

- Every interactive control is at least 48dp.
- Section titles use `heading()` semantics exactly once per group.
- Selected state is exposed on chip rows and category list items.
- Focus order follows visual order; visible focus uses Material 3 defaults.
- All user-facing strings live in Compose Resources; Bengali strings are added in lockstep for any new key.

## Key Files

| File | Role |
|------|------|
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsV2Components.kt` | `SettingsDetailHeader`, `SettingsGroup`, `SettingsSwitchCard`, `SettingsChipRow`, shared primitives |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsRows.kt` | Preference/switch/action row primitives |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsStates.kt` | Status/info/empty/error/loading state components |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/SettingsConfirmDialog.kt` | Destructive-action confirmation dialog |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/ui/settings/DoraiSettingsScreen.kt` | Responsive settings shell using the adaptive list/detail scaffold |
| `composeApp/src/commonTest/kotlin/com/dorai/ai/ui/settings/` | Compile-time signature tests for shared components |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | All user-facing strings (`Res.string.*`) |
