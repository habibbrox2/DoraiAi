package com.dorai.ai.theme.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * UI v2 font families.
 *
 * - [Sans]: "Noto Sans Bengali"-capable sans. The prototype CSS stack is
 *   `Noto Sans Bengali, Hind Siliguri, Nirmala UI, Kohinoor Bangla, Inter,
 *   system-ui…`. Compose resolves this through the platform font fallback
 *   (`FontFamily.Default`), which surfaces Noto Sans Bengali on devices
 *   that ship it; no bundled font resources are added here.
 * - [Mono]: JetBrains Mono-capable monospace for code/terminal
 *   (prototype: `JetBrains Mono, ui-monospace…`) via the platform monospace
 *   fallback (`FontFamily.Monospace`).
 */
object DoraiFonts {
    val Sans: FontFamily = FontFamily.Default
    val Mono: FontFamily = FontFamily.Monospace
}

/**
 * UI v2 type tokens. Body copy is 16sp with 1.6 line height so multi-line
 * Bengali text (high ascender/descender stack) does not clip — the prototype
 * uses `line-height: 1.6` globally.
 *
 * The legacy `DoraiTypography` (in `ui/Theme.kt`, bodyLarge 16sp/24sp = 1.5)
 * is left untouched; v2 screens should read from [DoraiType] (or
 * [DoraiV2Typography]) so the 1.6 rule lives in one place.
 */
object DoraiType {
    /** Body copy: 16sp / 25.6sp (1.6). */
    val Body = TextStyle(
        fontFamily = DoraiFonts.Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.6.sp,
    )

    /** Secondary body: 14sp / 22.4sp (1.6). */
    val BodyMedium = TextStyle(
        fontFamily = DoraiFonts.Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.4.sp,
    )

    /** Caption: 12sp / 19.2sp (1.6). */
    val BodySmall = TextStyle(
        fontFamily = DoraiFonts.Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.2.sp,
    )

    /** Code / terminal: 13sp / 20.8sp monospace (1.6). */
    val Code = TextStyle(
        fontFamily = DoraiFonts.Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.8.sp,
    )
}

/**
 * Material 3 typography built on [DoraiType]: the existing Dorai size scale,
 * with every body style set to 1.6 line height. For future phases to adopt;
 * the current `DoraiTypography` is intentionally not swapped yet.
 */
val DoraiV2Typography = Typography(
    bodyLarge = DoraiType.Body,
    bodyMedium = DoraiType.BodyMedium,
    bodySmall = DoraiType.BodySmall,
)
