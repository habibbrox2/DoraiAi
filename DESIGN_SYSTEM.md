# DORAI AI Design System

The DORAI AI Design System provides a unified, accessible, and high-performance foundation built for Material Design 3 and Compose Multiplatform across Android and Desktop.

---

## 1. Design Tokens

Central design tokens are defined in [`DesignTokens.kt`](composeApp/src/commonMain/kotlin/com/dorai/ai/ui/theme/DesignTokens.kt). Screens and components reference tokens instead of hard-coded constants.

### 1.1 Color Tokens

| Token | Hex / Value | Purpose | Accessibility |
| :--- | :--- | :--- | :--- |
| **`PrimaryMagenta` (`AccentPrimary`)** | `#D81B60` | Single primary accent for all switches, toggles, buttons, selected states, and sliders | Contrast ≥ 5.1:1 on white (WCAG AA), ≥ 3.3:1 on dark surfaces. Replaces harsh neon `#FF2E9A` |
| **`PrimaryCyan` (`AccentSecondary`)** | `#22D3EE` / `#25CDE2` | Secondary info/accent only (badges, highlights, links). **Never used for toggles** | High readability on dark theme |
| **`AgentViolet` (`AccentAgent`)** | `#8B78FF` | Agent-specific indicators and automation chips | Meets contrast requirements |
| **`Success`** | `#34D399` | Soft pastel emerald for successful executions/connections | Always paired with `Icons.Default.Check` |
| **`Warning`** | `#FBBF24` | Warm pastel amber for permission alerts & API key notices | Always paired with `Icons.Default.WarningAmber` |
| **`Error`** | `#FB7185` | Soft pastel rose/red for timeouts, 404s, and failures | Always paired with `Icons.Default.Close` / `ErrorOutline` |
| **`Info`** | `#22D3EE` | Informational callouts and tooltips | Always paired with `Icons.Default.Info` |

### 1.2 Surface Tiers

Surfaces provide 3 distinct tonal levels with subtle 1dp borders to guarantee depth and visual separation:

| Tier | Token | Value | Border | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Tier 1: Canvas** | `Background` | `#050A1C` | None | Window / scaffold background |
| **Tier 2: Card** | `SurfaceCard` / `SurfaceDark` | `#0D1933` / `#08132B` | `BorderSubtle` (`0x1AFFFFFF`) | Standard cards, settings rows, grouped controls |
| **Tier 3: Elevated** | `SurfaceElevated` / `SurfaceRaised` | `#0D1C38` / `#13284A` | `BorderSoft` (`0x2BFFFFFF`) | Dropdowns, popups, hero cards, action sheets |

### 1.3 Spacing Scale

Spacing uses the standard 4/8/12/16/24 dp scale defined in `DoraiSpacing`:

- `XXS` (2dp) / `XS` (4dp): Fine micro-spacing, chip content gaps.
- `SM` (8dp): Standard spacing between grouped rows, icon-to-label spacing.
- `MD` (12dp): Card padding, intra-section spacing.
- `LG` (16dp): Screen margin, standard card interior padding.
- `XL` (24dp): Inter-section breaks, empty state padding.
- `TouchTarget`: Minimum 48dp on all interactive elements.

### 1.4 Typography & Bengali Line-Height

Material 3 typography scale is defined in `DoraiTypography` with explicit font multipliers:
- **Bengali clipping fix**: `bodyLarge`, `bodyMedium`, and `bodySmall` use a `1.6x` line-height multiplier (`25.6sp`, `22.4sp`, `19.2sp` respectively) to prevent Bengali vowel matras and conjuncts from clipping.
- **Section headers**: Large, bold (`titleMedium` / `titleLarge`), high contrast (`textPrimary`).
- **Secondary text**: `textSecondary` maintain ≥ 4.5:1 contrast against dark surfaces.

### 1.5 Motion Tokens

Animations use standard durations:
- `MotionDurationShort`: 150ms (micro-interactions, button state changes).
- `MotionDurationMedium`: 200ms (accordion expansion, modal sheets).
- `MotionDurationLong`: 300ms (screen transitions, full-panel fades).
- All transitions respect user accessibility preferences (`prefersReducedMotion`).

---

## 2. Reusable Components

Located in [`DoraiDesignSystemComponents.kt`](composeApp/src/commonMain/kotlin/com/dorai/ai/ui/components/v2/DoraiDesignSystemComponents.kt).

### 2.1 SettingsCard & SettingsRow

```kotlin
SettingsCard(modifier = Modifier.padding(16.dp)) {
    SettingsRow(
        title = "Model Routing",
        subtitle = "Automatically select the best model",
        icon = Icons.Default.Tune,
        onClick = { /* navigate */ }
    )
}
```

### 2.2 AppSwitch

Compact Material 3 switch adhering to a minimum 48dp touch target and styled in `AccentPrimary` (`#D81B60`) for active state, subtle surface for inactive state. Padded to never touch the card's right edge.

```kotlin
AppSwitch(
    checked = isEnabled,
    onCheckedChange = { isEnabled = it }
)
```

### 2.3 AppSlider

Thin 4dp track with an elevated ≥20dp thumb, displaying real-time value label and min/max bounds:

```kotlin
AppSlider(
    value = scale,
    onValueChange = { scale = it },
    valueRange = 0.5f..2.0f,
    valueLabel = "${(scale * 100).toInt()}%",
    minRangeLabel = "50%",
    maxRangeLabel = "200%"
)
```

### 2.4 SegmentedPillSelector

Container-matching segmented control for modes (Ask, Plan, Build, Review) with one-line active mode descriptions:

```kotlin
SegmentedPillSelector(
    items = listOf(AgentMode.ASK, AgentMode.PLAN, AgentMode.BUILD, AgentMode.REVIEW),
    selectedItem = currentMode,
    onItemSelected = { currentMode = it },
    labelProvider = { it.displayName }
)
```

### 2.5 StatusChip

Semantic indicator pairing soft status colors with icons:

```kotlin
StatusChip(
    text = "Connected",
    type = StatusType.SUCCESS // Icons.Default.Check
)
```

### 2.6 ActionButton

Accessible button supporting primary, secondary, outlined, and destructive styles:

```kotlin
ActionButton(
    text = "Clear all conversations",
    onClick = { showDialog = true },
    variant = ActionButtonVariant.DESTRUCTIVE
)
```

### 2.7 AppTextField

Outlined/tonal text field with high contrast labels, clipboard paste button, show/hide password toggle, and clear action:

```kotlin
AppTextField(
    value = apiKey,
    onValueChange = { apiKey = it },
    label = "API Key",
    isPassword = true,
    showPasteButton = true
)
```

### 2.8 EmptyState

Centered icon, bold title, helper description, and primary CTA:

```kotlin
EmptyState(
    icon = Icons.Default.Code,
    title = "No Repositories Connected",
    helper = "Connect your GitHub account to inspect code and open pull requests.",
    ctaText = "Connect GitHub",
    onCtaClick = { vm.connect() }
)
```

### 2.9 ErrorLogItem

Collapsible diagnostics card with status icon, relative timestamp, human-readable summary, collapsed raw error / URL stack, and "Retry" / "Copy details" buttons:

```kotlin
ErrorLogItem(
    title = "Heartbeat Check",
    timeText = "5m ago",
    shortMessage = "Model endpoint not found (HTTP 404)",
    detailsText = "POST https://generativelanguage.googleapis.com/... returned 404",
    hintText = "Check provider and model ID",
    onRetry = { refresh() }
)
```
