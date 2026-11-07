package com.dorai.ai.theme.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * Surface treatment for the 2027 premium theme engine.
 *
 * - [Flat]: plain filled surface with a subtle 1dp hairline border.
 * - [ThreeD]: neumorphic depth — soft ambient/spot shadow, a vertical light gradient and a
 *   faint white top-edge highlight, suitable for Web (Wasm), Desktop and mobile.
 */
enum class ThemeStyle {
    Flat,
    ThreeD,
    ;

    companion object {
        val default: ThemeStyle = Flat

        /** Parse a persisted [ThemeStyle.name]; anything unknown falls back to [default]. */
        fun fromStorage(value: String?): ThemeStyle = entries.firstOrNull { it.name == value } ?: default
    }
}

/**
 * Immutable theme palette. Every color is stored as a hex string so custom themes serialize
 * cleanly to JSON (`custom_themes_json`) and back.
 *
 * Built-in presets live in [ThemePresets]; user-created palettes have [isCustom] = true and
 * follow the `custom_*` id convention.
 */
@Serializable
data class ThemePalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    /** Page/canvas background. */
    val background: String,
    /** Base surface (cards, panels). */
    val surface: String,
    /** Elevated surface tier (cards, sheets). */
    val surfaceElevated: String,
    /** Top-most raised tier (pills, inputs, focus wells). */
    val surfaceRaised: String,
    /** Primary brand accent (CTAs, selected state, focus ring). */
    val primary: String,
    /** Secondary accent — agent/violet highlight. */
    val accent: String,
    val onPrimary: String,
    val onBackground: String,
    val onSurface: String,
    val onSurfaceVariant: String,
    val outline: String,
    val outlineVariant: String,
    val success: String = "#10B981",
    val warning: String = "#F59E0B",
    val error: String = "#EF4444",
    val info: String = "#377CFF",
    val isCustom: Boolean = false,
) {
    val backgroundColor: Color get() = parseHexColor(background) ?: Color(0xFF050A1C)
    val surfaceColor: Color get() = parseHexColor(surface) ?: Color(0xFF08132B)
    val surfaceElevatedColor: Color get() = parseHexColor(surfaceElevated) ?: Color(0xFF0D1C38)
    val surfaceRaisedColor: Color get() = parseHexColor(surfaceRaised) ?: Color(0xFF13284A)
    val primaryColor: Color get() = parseHexColor(primary) ?: Color(0xFF25CDE2)
    val accentColor: Color get() = parseHexColor(accent) ?: Color(0xFF8B78FF)
    val onPrimaryColor: Color get() = parseHexColor(onPrimary) ?: Color.White
    val onBackgroundColor: Color get() = parseHexColor(onBackground) ?: Color(0xFFF1F8FF)
    val onSurfaceColor: Color get() = parseHexColor(onSurface) ?: Color(0xFFF1F8FF)
    val onSurfaceVariantColor: Color get() = parseHexColor(onSurfaceVariant) ?: Color(0xFFC4D3E8)
    val outlineColor: Color get() = parseHexColor(outline) ?: Color(0xFF2B3A55)
    val outlineVariantColor: Color get() = parseHexColor(outlineVariant) ?: Color(0xFF1AFFFFFF)
    val successColor: Color get() = parseHexColor(success) ?: Color(0xFF10B981)
    val warningColor: Color get() = parseHexColor(warning) ?: Color(0xFFF59E0B)
    val errorColor: Color get() = parseHexColor(error) ?: Color(0xFFEF4444)
    val infoColor: Color get() = parseHexColor(info) ?: Color(0xFF377CFF)
}

/**
 * Persistence-facing engine state. [selectedThemeId] points at a preset id or a `custom_*` id;
 * [isSystemTheme] is the Match Device switch (follow the OS light/dark setting while retaining
 * the selected accent); [style] is the Flat/3D surface treatment.
 */
@Serializable
data class ThemeState(
    // New-install default: OG Shine brand preset, Match Device off so it shows as-is.
    val selectedThemeId: String = ThemePresets.OG_SHINE_ID,
    val isSystemTheme: Boolean = false,
    val style: String = ThemeStyle.Flat.name,
    val customThemes: List<ThemePalette> = emptyList(),
) {
    /** The active flat/3D treatment. Unknown stored values degrade to [ThemeStyle.Flat]. */
    val activeStyle: ThemeStyle
        get() = ThemeStyle.fromStorage(style)

    /** Alias kept for call-site readability: this is the "Match Device" switch. */
    val matchDevice: Boolean
        get() = isSystemTheme

    /** Built-in presets followed by the user's custom themes, in stable order. */
    val availablePresets: List<ThemePalette>
        get() = ThemePresets.all + customThemes

    /** The palette the user selected, regardless of the Match Device switch. */
    fun basePalette(): ThemePalette = availablePresets.firstOrNull { it.id == selectedThemeId } ?: ThemePresets.ogShine
}

/**
 * Resolve the palette that should actually be shown.
 *
 * With Match Device on, the Light/Dark base pair is picked from the OS setting and the user's
 * selected accent is retained on top of it — but only when the selected palette belongs to the
 * same luminance class as the picked base. A dark accent on a light canvas (or vice versa) is a
 * contrast failure, so in mixed cases the base pair's own accents win. With Match Device off,
 * the selected palette is used as-is.
 */
fun ThemeState.resolve(systemInDark: Boolean): ThemePalette {
    if (!isSystemTheme) return basePalette()
    val base = if (systemInDark) ThemePresets.dark else ThemePresets.light
    val selected = basePalette()
    return if (selected.isDark == base.isDark) {
        base.copy(primary = selected.primary, accent = selected.accent, onPrimary = selected.onPrimary)
    } else {
        base
    }
}

/** Parse `#RGB`, `#RGBA`, `#RRGGBB` or `#AARRGGBB`. Returns null for malformed input. */
fun parseHexColor(hex: String): Color? {
    val trimmed = hex.trim()
    if (!trimmed.startsWith("#")) return null
    val clean = trimmed.removePrefix("#").uppercase()
    if (clean.isEmpty() || !clean.all { it in '0'..'9' || it in 'A'..'F' }) return null
    return runCatching {
        when (clean.length) {
            3 -> Color(0xFF000000L or expandNibbles(clean, 2))
            4 -> Color(expandNibbles(clean, 2))
            6 -> Color(0xFF000000L or clean.toLong(16))
            8 -> Color(clean.toLong(16))
            else -> null
        }
    }.getOrNull()
}

/** True when [hex] parses to a usable color. */
fun isValidHexColor(hex: String): Boolean = parseHexColor(hex) != null

/** Serialize a [Color] back to `#RRGGBB` (alpha is dropped; custom themes are opaque). */
fun Color.toHexString(): String {
    fun channel(v: Float): Int = (v.coerceIn(0f, 1f) * 255f).toInt()
    fun hex(v: Int): String = v.toString(16).uppercase().padStart(2, '0')
    return "#" + hex(channel(red)) + hex(channel(green)) + hex(channel(blue))
}

/** Compute relative luminance of a [Color] per WCAG 2.1 specs. */
fun Color.relativeLuminance(): Double {
    fun lin(c: Float): Double {
        val s = c.coerceIn(0f, 1f).toDouble()
        return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * lin(red) + 0.7152 * lin(green) + 0.0722 * lin(blue)
}

/** Compute WCAG 2.1 contrast ratio between two colors (returns value >= 1.0). */
fun contrastRatio(first: Color, second: Color): Double {
    val l1 = first.relativeLuminance()
    val l2 = second.relativeLuminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

val DefaultLightText: Color = Color(0xFFF1F8FF)
val DefaultDarkText: Color = Color(0xFF1C1C1E)

/**
 * Returns either [DefaultDarkText] or [DefaultLightText], choosing whichever
 * provides the higher WCAG contrast ratio against [background].
 */
fun readableTextColorFor(background: Color): Color {
    val lightRatio = contrastRatio(background, DefaultLightText)
    val darkRatio = contrastRatio(background, DefaultDarkText)
    return if (darkRatio >= lightRatio) DefaultDarkText else DefaultLightText
}

/** Relative luminance check on the background; used to flag dark vs light palettes. */
fun ThemePalette.isBackgroundDark(): Boolean = backgroundColor.relativeLuminance() < 0.5

private fun expandNibbles(s: String, charsPerChannel: Int): Long = s.chunked(charsPerChannel)
    .joinToString("") { it.repeat(2) }
    .toLong(16)
