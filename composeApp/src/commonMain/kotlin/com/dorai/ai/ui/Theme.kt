@file:Suppress("DEPRECATION")

package com.dorai.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.ui.theme.DesignColorTokens
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.ui.theme.LocalDesignTokens
import org.jetbrains.compose.ui.tooling.preview.Preview

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// BROX KAI 2.0 Ã¢â‚¬â€ 2027 MINIMAL DESIGN SYSTEM
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Core Brand Colors Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
val broxNavy = DesignTokens.BrandNavy
val broxDeepNavy = DesignTokens.Background
val broxCharcoal = DesignTokens.SurfaceRaised
val BackgroundPrimary = Color(0xFF0E0F12)
val AccentPrimary = com.dorai.ai.ui.theme.DesignTokens.PrimaryCyan
val broxCyan = AccentPrimary
val broxLightSurface = Color(0xFFFFFFFF)
val broxMutedText = Color(0xFF64748B)

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ 2027 Design Tokens (scheme-aware via LocalDesignColorTokens) Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

val SurfaceElevated: Color
    @Composable get() = LocalDesignColorTokens.current.surfaceElevated
val SurfaceRaised: Color
    @Composable get() = LocalDesignColorTokens.current.surfaceRaised

/** Card surfaces */
val CardFill: Color
    @Composable get() = LocalDesignColorTokens.current.surfaceElevated
val CardSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

/** Borders */
val BorderSubtle: Color
    @Composable get() = LocalDesignColorTokens.current.borderSubtle
val BorderSoft: Color
    @Composable get() = LocalDesignColorTokens.current.borderSoft
val BorderStrong: Color
    @Composable get() = LocalDesignColorTokens.current.borderStrong

/** Primary accent - vibrant cyan/teal */
val AccentPrimaryHover = Color(0xFF19B8D0)
val AccentPrimaryActive = Color(0xFF0B8FA8)

/** Accent gradient colors */
val AccentGradientStart = AccentPrimary
val AccentGradientEnd = DesignTokens.BrandBlue

/** Status colors */
val DangerAlert = DesignTokens.Error
val SuccessGreen = DesignTokens.Success
val WarningAmber = DesignTokens.Warning
val InfoBlue = Color(0xFF3B82F6)

// DoraiTypography - 2026 Modern Type Scale
val DoraiTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        letterSpacing = (-0.25).sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        letterSpacing = 0.5.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp,
    ),
)

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ DoraiShapes Ã¢â‚¬â€ 2026 Modern Shape System Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
object DoraiShapes {
    /** Extra small elements */
    val xs = RoundedCornerShape(8.dp)

    /** Small elements */
    val sm = RoundedCornerShape(10.dp)

    /** Row / item container radius */
    val row = RoundedCornerShape(12.dp)

    /** Input field radius */
    val input = RoundedCornerShape(14.dp)

    /** Card / panel default radius */
    val card = RoundedCornerShape(16.dp)

    /** Medium containers */
    val md = RoundedCornerShape(18.dp)

    /** Section container radius */
    val section = RoundedCornerShape(20.dp)

    /** Large panels */
    val lg = RoundedCornerShape(24.dp)

    /** Extra large panels/sheets */
    val xl = RoundedCornerShape(28.dp)

    /** Pill / chip full radius */
    val pill = RoundedCornerShape(999.dp)

    /** Button radius */
    val button = RoundedCornerShape(999.dp)

    /** User message bubble - modern asymmetric */
    val userBubble = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 6.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp,
    )

    /** Assistant message surface */
    val assistantSurface = RoundedCornerShape(20.dp)

    /** Status card */
    val statusCard = RoundedCornerShape(14.dp)

    /** Quick-start card */
    val quickStartCard = RoundedCornerShape(18.dp)

    /** Composer container */
    val composer = RoundedCornerShape(14.dp)

    /** Panel / sheet heading */
    val panel = RoundedCornerShape(24.dp)

    /** Modal/dialog */
    val dialog = RoundedCornerShape(28.dp)
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ DoraiBorders Ã¢â‚¬â€ Centralized stroke definitions Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
object DoraiBorders {
    /** Subtle 0.5px hairline */
    @Composable fun hairline(): BorderStroke = BorderStroke(0.5.dp, BorderSoft)

    /** Standard 1px soft outline */
    @Composable fun soft(): BorderStroke = BorderStroke(1.dp, BorderSoft)

    /** Neon cyan accent border */
    @Composable fun accent(): BorderStroke = BorderStroke(1.dp, AccentPrimary)

    /** Focused state border with glow */
    @Composable fun focus(): BorderStroke = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.6f))

    /** Input focused border */
    @Composable fun inputFocused(): BorderStroke = BorderStroke(1.dp, AccentPrimary)

    /** Disabled border */
    @Composable fun disabled(): BorderStroke = BorderStroke(1.dp, BorderSoft.copy(alpha = 0.3f))
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ DoraiElevation Ã¢â‚¬â€ Cross-platform elevation using borders + alpha overlays Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
object DoraiElevation {
    /** Subtle card elevation Ã¢â‚¬â€ soft border + faint overlay */
    @Composable fun level1(): BorderStroke = BorderStroke(1.dp, BorderSoft.copy(alpha = 0.8f))

    /** Raised panel elevation Ã¢â‚¬â€ soft border + visible overlay */
    @Composable fun level2(): BorderStroke = BorderStroke(1.dp, BorderSoft.copy(alpha = 1.0f))

    /** Interactive element elevation Ã¢â‚¬â€ accent border + glow overlay */
    @Composable fun level3(): BorderStroke = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))

    /** Modal / dialog elevation Ã¢â‚¬â€ accent border + strong glow */
    @Composable fun level4(): BorderStroke = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.6f))
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Animated border gradient colors (Brox brand palette) Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// gradientPurple = broxNavy, gradientViolet = broxCyan, gradientMagenta = broxDeepNavy
// (now referenced directly in AnimatedGradientBorder.kt)

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Utility Modifiers Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
fun Modifier.handCursor() = pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true)

/** 2026 Modern card surface */
@Composable
fun Modifier.modernCardSurface(
    shape: Shape = DoraiShapes.card,
    elevation: Boolean = true,
): Modifier = this
    .clip(shape)
    .background(CardFill, shape)
    .border(BorderStroke(1.dp, BorderSubtle), shape)
    .then(
        if (elevation) {
            Modifier.border(BorderStroke(1.dp, BorderSoft.copy(alpha = 0.5f)), shape)
        } else {
            Modifier
        },
    )

/** Glassmorphic card surface with backdrop blur effect (legacy) */
@Composable
fun Modifier.glassmorphicSurface(
    shape: Shape = DoraiShapes.card,
    elevation: Boolean = true,
): Modifier = modernCardSurface(shape, elevation)

/** Modern card with hover/focus glow */
@Composable
fun Modifier.modernCard(
    shape: Shape = DoraiShapes.card,
    isInteractive: Boolean = false,
    isFocused: Boolean = false,
    isHovered: Boolean = false,
): Modifier = this
    .clip(shape)
    .background(CardFill, shape)
    .border(
        if (isFocused || isHovered) BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.6f)) else BorderStroke(1.dp, BorderSubtle),
        shape,
    )
    .then(
        if (isInteractive && (isFocused || isHovered)) {
            Modifier.border(BorderStroke(1.dp, AccentPrimary), shape)
        } else {
            Modifier
        },
    )

/** Glassmorphic card with hover/focus glow (legacy) */
@Composable
fun Modifier.glassmorphicCard(
    shape: Shape = DoraiShapes.card,
    isInteractive: Boolean = false,
    isFocused: Boolean = false,
    isHovered: Boolean = false,
): Modifier = modernCard(shape, isInteractive, isFocused, isHovered)

/** Cyan glow modifier for active/primary elements.
 *  Cross-platform implementation using border + alpha overlay instead of RenderEffect.
 */
@Composable
fun Modifier.cyanGlow(enabled: Boolean = true): Modifier = if (enabled) {
    this
        .border(DoraiBorders.accent())
        .background(AccentPrimary, shape = DoraiShapes.pill)
} else {
    this
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Color Schemes Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

/** Light theme */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF245FD8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = broxNavy,
    secondary = Color(0xFFE8F0F8),
    onSecondary = broxNavy,
    tertiary = Color(0xFFF0F4FA),
    onTertiary = broxNavy,
    error = DangerAlert,
    onError = Color(0xFFFFFFFF),
    errorContainer = DangerAlert.copy(alpha = 0.2f),
    onErrorContainer = broxNavy,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    inverseSurface = Color(0xFF1E1E1E),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = DesignTokens.BrandBlue,
    surfaceContainerHighest = Color(0xFFE2E8F0),
    surfaceContainerHigh = Color(0xFFEBF1F7),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

/** Dark theme */
val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color(0xFF08132B),
    primaryContainer = broxNavy,
    onPrimaryContainer = DesignTokens.PrimaryText,
    secondary = broxDeepNavy,
    onSecondary = DesignTokens.PrimaryText,
    tertiary = broxCharcoal,
    onTertiary = DesignTokens.PrimaryText,
    error = DangerAlert,
    onError = Color.White,
    errorContainer = DangerAlert.copy(alpha = 0.3f),
    onErrorContainer = DesignTokens.PrimaryText,
    background = com.dorai.ai.ui.theme.DesignTokens.Background,
    onBackground = com.dorai.ai.ui.theme.DesignTokens.PrimaryText,
    surface = com.dorai.ai.ui.theme.DesignTokens.SurfaceDark,
    onSurface = com.dorai.ai.ui.theme.DesignTokens.PrimaryText,
    surfaceVariant = com.dorai.ai.ui.theme.DesignTokens.SurfaceElevated,
    onSurfaceVariant = com.dorai.ai.ui.theme.DesignTokens.SecondaryText,
    outline = com.dorai.ai.ui.theme.DesignTokens.BorderSoft,
    outlineVariant = com.dorai.ai.ui.theme.DesignTokens.BorderSubtle,
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = broxNavy,
    surfaceContainerHighest = com.dorai.ai.ui.theme.DesignTokens.SurfaceRaised,
    surfaceContainerHigh = com.dorai.ai.ui.theme.DesignTokens.SurfaceElevated,
    surfaceContainer = com.dorai.ai.ui.theme.DesignTokens.SurfaceDark,
    surfaceContainerLow = com.dorai.ai.ui.theme.DesignTokens.Background,
    surfaceContainerLowest = com.dorai.ai.ui.theme.DesignTokens.BackgroundOled,
)

/** Lilac theme */
val LilacColorScheme = lightColorScheme(
    primary = Color(0xFF6B46C1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9D8FD),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color.White,
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF2D1B69),
    background = Color(0xFFFAF7FF),
    onBackground = Color(0xFF1E1B4B),
    surface = Color(0xFFF3F0FF),
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFEDE9FE),
    onSurfaceVariant = Color(0xFF4C1D95),
    outline = Color(0xFF8B5CF6),
    outlineVariant = Color(0xFFE9D8FD),
)

/** Dusk theme */
val DuskColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF1C1917),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFCD34D),
    secondary = Color(0xFFA855F7),
    onSecondary = Color.White,
    tertiary = Color(0xFFEC4899),
    onTertiary = Color.White,
    background = Color(0xFF0F0A19),
    onBackground = Color(0xFFF3E8FF),
    surface = Color(0xFF1E1625),
    onSurface = Color(0xFFF3E8FF),
    surfaceVariant = Color(0xFF2D1B69),
    onSurfaceVariant = Color(0xFFD8B4FE),
    outline = Color(0xFFA855F7),
    outlineVariant = Color(0xFF4C1D95),
)

/** Seafoam theme */
val SeafoamColorScheme = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F3D0),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF064E3B),
    background = Color(0xFFF0FDF4),
    onBackground = Color(0xFF064E3B),
    surface = Color(0xFFECFDF5),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFD1FAE5),
    onSurfaceVariant = Color(0xFF047857),
    outline = Color(0xFF10B981),
    outlineVariant = Color(0xFFA7F3D0),
)

/** Graphite theme */
val GraphiteColorScheme = darkColorScheme(
    primary = Color(0xFF94A3B8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    tertiary = Color(0xFF475569),
    onTertiary = Color.White,
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFE5E5E5),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF252525),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF333333),
)

/** Daybreak theme */
val DaybreakColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF1E3A8A),
    background = Color(0xFFF8FAFF),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF1F5FF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE0E7FF),
    onSurfaceVariant = Color(0xFF312E81),
    outline = Color(0xFF3B82F6),
    outlineVariant = Color(0xFFDBEAFE),
)

/** Sherbet theme */
val SherbetColorScheme = lightColorScheme(
    primary = Color(0xFFD91C78),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF9D174D),
    secondary = Color(0xFFF472B6),
    onSecondary = Color.White,
    tertiary = Color(0xFFF9A8D4),
    onTertiary = Color(0xFF831843),
    background = Color(0xFFFFF7FB),
    onBackground = Color(0xFF500724),
    surface = Color(0xFFFEF2F8),
    onSurface = Color(0xFF500724),
    surfaceVariant = Color(0xFFFCE7F3),
    onSurfaceVariant = Color(0xFF9D174D),
    outline = Color(0xFFEC4899),
    outlineVariant = Color(0xFFFCE7F3),
)

/** Paper theme */
val PaperColorScheme = lightColorScheme(
    primary = Color(0xFF92400E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFFD97757),
    onSecondary = Color.White,
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF451A03),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFF5F3F0),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFE7E5E4),
    onSurfaceVariant = Color(0xFF44403C),
    outline = Color(0xFFA8A29E),
    outlineVariant = Color(0xFFD6D3D1),
)

/** Carbon theme */
val CarbonColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    tertiary = Color(0xFF93C5FD),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE5E5E5),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF3B82F6),
    outlineVariant = Color(0xFF1E3A8A),
)

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Card Colors & Borders Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

@Composable
fun doraiAdaptiveCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = LocalDesignColorTokens.current.cardSurface,
)

@Composable
fun doraiAdaptiveCardBorder(): BorderStroke = BorderStroke(1.dp, LocalDesignColorTokens.current.borderSubtle)

@Composable
fun Modifier.doraiAdaptiveCardSurface(shape: Shape = CardDefaults.shape): Modifier = this
    .clip(shape)
    .background(
        LocalDesignColorTokens.current.cardSurface,
        shape,
    )
    .then(
        Modifier.border(1.dp, LocalDesignColorTokens.current.borderSubtle, shape),
    )

/** Glassmorphic card colors for Settings and UI panels */
@Composable
fun glassmorphicCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = LocalDesignColorTokens.current.cardFill,
)

/** Glassmorphic card border */
@Composable
fun glassmorphicCardBorder(): BorderStroke = BorderStroke(1.dp, LocalDesignColorTokens.current.borderSoft)

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Text Field Colors Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

@Composable
fun outlineTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalDesignColorTokens.current.textPrimary,
    unfocusedTextColor = LocalDesignColorTokens.current.textPrimary,
    disabledTextColor = LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.5f),
    errorTextColor = DangerAlert,
    focusedContainerColor = LocalDesignColorTokens.current.surfaceRaised,
    unfocusedContainerColor = LocalDesignColorTokens.current.surfaceRaised,
    disabledContainerColor = LocalDesignColorTokens.current.surfaceRaised.copy(alpha = 0.4f),
    errorContainerColor = DangerAlert.copy(alpha = 0.2f),
    cursorColor = AccentPrimary,
    errorCursorColor = DangerAlert,
    focusedBorderColor = AccentPrimary,
    unfocusedBorderColor = LocalDesignColorTokens.current.borderSoft,
    disabledBorderColor = LocalDesignColorTokens.current.borderSoft.copy(alpha = 0.5f),
    errorBorderColor = DangerAlert,
    focusedLabelColor = AccentPrimary,
    unfocusedLabelColor = LocalDesignColorTokens.current.textSecondary,
    disabledLabelColor = LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.5f),
    errorLabelColor = DangerAlert,
    focusedPlaceholderColor = LocalDesignColorTokens.current.textSecondary,
    unfocusedPlaceholderColor = LocalDesignColorTokens.current.textSecondary,
    disabledPlaceholderColor = LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.5f),
    errorPlaceholderColor = DangerAlert,
    focusedLeadingIconColor = LocalDesignColorTokens.current.textSecondary,
    unfocusedLeadingIconColor = LocalDesignColorTokens.current.textSecondary,
    disabledLeadingIconColor = LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.5f),
    errorLeadingIconColor = DangerAlert,
    focusedTrailingIconColor = LocalDesignColorTokens.current.textSecondary,
    unfocusedTrailingIconColor = LocalDesignColorTokens.current.textSecondary,
    disabledTrailingIconColor = LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.5f),
    errorTrailingIconColor = DangerAlert,
)

@Composable
fun DoraiOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = DoraiShapes.row,
        colors = outlineTextFieldColors(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
fun DoraiClearableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var focused by remember { mutableStateOf(false) }
    DoraiOutlinedTextField(
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.handCursor()
                    .alpha(if (focused && value.isNotEmpty()) 1f else 0f),
                enabled = value.isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
            }
        },
    )
}

@Composable
@Preview
fun Theme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    val isLight = colorScheme == LightColorScheme || colorScheme.background == Color.White || colorScheme.background == Color(0xFFFFFFFF)
    val palette = if (isLight) com.dorai.ai.theme.data.ThemePresets.light else com.dorai.ai.theme.data.ThemePresets.dark
    val themeState = com.dorai.ai.theme.data.ThemeState(
        selectedThemeId = if (isLight) com.dorai.ai.theme.data.ThemePresets.LIGHT_ID else com.dorai.ai.theme.data.ThemePresets.DARK_ID,
        isSystemTheme = false,
    )
    CompositionLocalProvider(
        LocalDesignTokens provides DesignTokens,
        LocalDesignColorTokens provides DesignColorTokens.from(colorScheme),
        com.dorai.ai.theme.engine.LocalDoraiPalette provides palette,
        com.dorai.ai.theme.engine.LocalDoraiThemeState provides themeState,
        com.dorai.ai.theme.engine.LocalDoraiThemeStyle provides themeState.activeStyle,
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
        ) {
            content()
        }
    }
}

// Claude-inspired theme
val ClaudeColorScheme = darkColorScheme(
    primary = Color(0xFFD97757),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8B4513).copy(alpha = 0.3f),
    onPrimaryContainer = Color(0xFFFFD4C4),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color.Black,
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF2D2D2D),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF4A4A4A),
)

// ChatGPT-inspired theme
val ChatGPTColorScheme = darkColorScheme(
    primary = Color(0xFF10A37F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D8A6A).copy(alpha = 0.3f),
    onPrimaryContainer = Color(0xFFB3F0E0),
    secondary = Color(0xFF19C37D),
    onSecondary = Color.White,
    background = Color(0xFF212121),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF2F2F2F),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFABABAB),
    outline = Color(0xFF565656),
)

// Grok-inspired theme
val GrokColorScheme = darkColorScheme(
    primary = Color(0xFF1DA1F2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1DA1F2).copy(alpha = 0.3f),
    onPrimaryContainer = Color(0xFFB3E0FF),
    secondary = Color(0xFF4A9EFF),
    onSecondary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF151515),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF3A3A3A),
)

// Gemini-inspired theme
val GeminiColorScheme = darkColorScheme(
    primary = Color(0xFF4285F4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4285F4).copy(alpha = 0.3f),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF34A853),
    onSecondary = Color.White,
    background = Color(0xFF131314),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF1E1F20),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF2D2E2F),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = Color(0xFF3C4043),
)

/**
 * Glassmorphism effect modifier for cards and surfaces
 */
@Composable
fun Modifier.glassmorphicCard(
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
): Modifier = this
    .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
    .background(backgroundColor ?: LocalDesignColorTokens.current.surfaceElevated)
