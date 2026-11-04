package com.dorai.ai.theme

import androidx.lifecycle.ViewModel
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.theme.data.ThemeState
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.data.isBackgroundDark
import com.dorai.ai.theme.data.isValidHexColor
import com.dorai.ai.theme.data.parseHexColor
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow
import kotlin.random.Random

/**
 * State holder for the Appearance screen. All reads go through [ThemeSettingsRepository], so the
 * Appearance UI and the app root stay in sync; mutations apply immediately, with no restart.
 */
class ThemeViewModel(private val repository: ThemeSettingsRepository) : ViewModel() {

    val state: StateFlow<ThemeState> = repository.state

    /** Select a built-in preset or a custom theme by id. */
    fun selectTheme(id: String) = repository.setSelectedThemeId(id)

    /** Match Device — follow the OS light/dark setting while retaining the selected accent. */
    fun setMatchDevice(enabled: Boolean) = repository.setMatchDevice(enabled)

    /** Flat / 3D surface treatment for premium surfaces. */
    fun setThemeStyle(style: ThemeStyle) = repository.setThemeStyle(style)

    /**
     * Create (or replace) a custom theme from three opaque hex colors. The custom palette is
     * saved, selected immediately and Match Device is turned off so the change is visible.
     *
     * @return true when the palette was saved; false when a hex value is invalid.
     */
    fun saveCustomTheme(displayName: String, backgroundHex: String, surfaceHex: String, primaryHex: String): Boolean {
        val background = parseHexColor(backgroundHex) ?: return false
        val surface = parseHexColor(surfaceHex) ?: return false
        val primary = parseHexColor(primaryHex) ?: return false
        val trimmedName = displayName.trim().ifBlank { "Custom theme" }
        val palette = ThemePalette(
            id = "custom_" + randomIdSuffix(),
            displayName = trimmedName,
            isDark = false,
            background = backgroundHex.trim(),
            surface = surfaceHex.trim(),
            surfaceElevated = surfaceHex.trim(),
            surfaceRaised = surfaceHex.trim(),
            primary = primaryHex.trim(),
            accent = primaryHex.trim(),
            onPrimary = readableOn(primary),
            onBackground = readableOn(background),
            onSurface = readableOn(surface),
            onSurfaceVariant = variantOn(surface),
            outline = outlineFor(surface),
            outlineVariant = variantOutlineFor(surface),
            isCustom = true,
        ).let { palette ->
            // Flag luminance from the actual background so Match Device can pair it correctly.
            palette.copy(isDark = palette.isBackgroundDark())
        }
        repository.saveCustomTheme(palette)
        return true
    }

    fun deleteCustomTheme(id: String) = repository.deleteCustomTheme(id)

    companion object {
        /** Validation helper shared with the create dialog — no hex parsing duplicated in UI. */
        fun isValidHex(hex: String): Boolean = isValidHexColor(hex)

        private fun randomIdSuffix(): String {
            val chars = "0123456789abcdef"
            return buildString { repeat(8) { append(chars[Random.nextInt(chars.length)]) } }
        }

        /** Near-black text on light fills, near-white on dark fills. */
        private fun readableOn(color: androidx.compose.ui.graphics.Color): String =
            if (luminance(color) > 0.5) "#1C1C1E" else "#F1F8FF"

        private fun variantOn(color: androidx.compose.ui.graphics.Color): String =
            if (luminance(color) > 0.5) "#475569" else "#C4D3E8"

        private fun outlineFor(color: androidx.compose.ui.graphics.Color): String =
            if (luminance(color) > 0.5) "#94A3B8" else "#2B3A55"

        private fun variantOutlineFor(color: androidx.compose.ui.graphics.Color): String =
            if (luminance(color) > 0.5) "#CBD5E1" else "#1AFFFFFF"

        private fun luminance(color: androidx.compose.ui.graphics.Color): Double {
            fun lin(c: Float): Double {
                val s = c.coerceIn(0f, 1f).toDouble()
                return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
            }
            val r = lin(color.red)
            val g = lin(color.green)
            val b = lin(color.blue)
            return 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
    }
}
