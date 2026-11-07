package com.dorai.ai.theme.engine

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.theme.data.ThemeState
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.data.resolve
import com.dorai.ai.theme.tokens.DoraiSemanticColors
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.DesignColorTokens
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.ui.theme.LocalDesignTokens
import org.koin.compose.koinInject

/** The persisted engine state (selected theme, Match Device, Flat/3D style, custom themes). */
val LocalDoraiThemeState = staticCompositionLocalOf { ThemeState() }

/** The palette that is actually rendered — Match Device aware. Default: OG Shine brand preset. */
val LocalDoraiPalette = staticCompositionLocalOf { ThemePresets.ogShine }

/** The active Flat/3D surface treatment. */
val LocalDoraiThemeStyle = staticCompositionLocalOf { ThemeStyle.Flat }

/**
 * Root theme for the 2027 Premium AI Agent Workspace.
 *
 * Resolves the selected preset / custom theme against the OS light-dark preference (Match
 * Device), maps it to a Material 3 [ColorScheme] and provides:
 *  - [LocalDoraiThemeState] / [LocalDoraiPalette] / [LocalDoraiThemeStyle] for premium surfaces,
 *  - [LocalDoraiColors] (UI v2 semantic tokens) derived from the same palette — the single
 *    source v2 screens read from,
 *  - [LocalDesignColorTokens] (scheme-aware text/surface/border tokens) derived from the same
 *    palette, so every existing screen follows the selected theme,
 *  - [LocalDesignTokens] (legacy static 2027 AURORA constants) kept for source compatibility.
 *
 * When [repository] is null (normal app startup), it is resolved from Koin. Pass an explicit
 * repository only from tests and previews.
 */
@Composable
fun DoraiTheme(
    repository: ThemeSettingsRepository? = null,
    content: @Composable () -> Unit,
) {
    val repo = repository ?: koinInject()
    val state by repo.state.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    val palette = state.resolve(systemInDark)
    val colorScheme = palette.toColorScheme()

    CompositionLocalProvider(
        LocalDoraiThemeState provides state,
        LocalDoraiPalette provides palette,
        LocalDoraiThemeStyle provides state.activeStyle,
        LocalDoraiColors provides DoraiSemanticColors.from(palette),
        LocalDesignTokens provides DesignTokens,
        LocalDesignColorTokens provides DesignColorTokens.from(colorScheme),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DoraiTypography,
            shapes = Shapes(
                extraSmall = DoraiShapes.input,
                small = DoraiShapes.row,
                medium = DoraiShapes.card,
                large = DoraiShapes.section,
                extraLarge = DoraiShapes.panel,
            ),
            content = content,
        )
    }
}

/** Map a [ThemePalette] onto a complete Material 3 color scheme. */
fun ThemePalette.toColorScheme(): ColorScheme {
    val primary = primaryColor
    val accent = accentColor
    val surfaceElevated = surfaceElevatedColor
    val surfaceRaised = surfaceRaisedColor
    val background = backgroundColor
    val surface = surfaceColor
    val outline = outlineColor
    val outlineVariant = outlineVariantColor
    val isDarkScheme = isDark

    // Inverted roles for the (rare) surfaces that need the opposite luminance class.
    val inverseSurface = if (isDarkScheme) Color(0xFFE0E0E0) else Color(0xFF1E1E1E)
    val inverseOnSurface = if (isDarkScheme) Color(0xFF1C1C1C) else Color(0xFFFFFFFF)

    val scheme = if (isDarkScheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimaryColor,
            primaryContainer = surfaceRaised,
            onPrimaryContainer = onSurfaceColor,
            secondary = accent,
            onSecondary = onPrimaryColor,
            secondaryContainer = surfaceElevated,
            onSecondaryContainer = onSurfaceColor,
            tertiary = accent,
            onTertiary = onPrimaryColor,
            tertiaryContainer = surfaceElevated,
            onTertiaryContainer = onSurfaceColor,
            error = errorColor,
            onError = Color.White,
            errorContainer = errorColor.copy(alpha = 0.3f),
            onErrorContainer = onSurfaceColor,
            background = background,
            onBackground = onBackgroundColor,
            surface = surface,
            onSurface = onSurfaceColor,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = onSurfaceVariantColor,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = Color.Black,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = accent,
            surfaceTint = primary,
            surfaceBright = surfaceRaised,
            surfaceDim = background,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceElevated,
            surfaceContainerHighest = surfaceRaised,
            surfaceContainerLow = background,
            surfaceContainerLowest = Color(0xFF000000),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimaryColor,
            primaryContainer = surfaceElevated,
            onPrimaryContainer = onSurfaceColor,
            secondary = accent,
            onSecondary = onPrimaryColor,
            secondaryContainer = surfaceElevated,
            onSecondaryContainer = onSurfaceColor,
            tertiary = accent,
            onTertiary = onPrimaryColor,
            tertiaryContainer = surfaceElevated,
            onTertiaryContainer = onSurfaceColor,
            error = errorColor,
            onError = Color.White,
            errorContainer = errorColor.copy(alpha = 0.2f),
            onErrorContainer = onSurfaceColor,
            background = background,
            onBackground = onBackgroundColor,
            surface = surface,
            onSurface = onSurfaceColor,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = onSurfaceVariantColor,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = Color.Black,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = accent,
            surfaceTint = primary,
            surfaceBright = surface,
            surfaceDim = surfaceRaised,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceElevated,
            surfaceContainerHighest = surfaceRaised,
            surfaceContainerLow = background,
            surfaceContainerLowest = Color.White,
        )
    }
    return scheme
}
