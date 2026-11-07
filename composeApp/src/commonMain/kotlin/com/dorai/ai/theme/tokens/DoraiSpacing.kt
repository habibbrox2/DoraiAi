package com.dorai.ai.theme.tokens

import androidx.compose.ui.unit.Dp
import com.dorai.ai.ui.theme.DesignTokens

/**
 * UI v2 spacing scale — one place for all padding/gap values.
 *
 * Deliberately delegates to [DesignTokens] so there is a single source of
 * truth:
 * - [XS] = 4dp  → `DesignTokens.SpacingXs`
 * - [SM] = 8dp  → `DesignTokens.SpacingSm`
 * - [MD] = 12dp → `DesignTokens.SpacingMd`
 * - [LG] = 16dp → `DesignTokens.SpacingLg`
 * - [XL] = 24dp → `DesignTokens.SpacingXxl`
 * - [XXL] = 32dp → `DesignTokens.SpacingXxxl`
 *
 * (`DesignTokens.SpacingXxs` 2dp and `SpacingXl` 20dp stay available for
 * legacy screens but are not part of the v2 scale.)
 */
object DoraiSpacing {
    val XS: Dp = DesignTokens.SpacingXs
    val SM: Dp = DesignTokens.SpacingSm
    val MD: Dp = DesignTokens.SpacingMd
    val LG: Dp = DesignTokens.SpacingLg
    val XL: Dp = DesignTokens.SpacingXxl
    val XXL: Dp = DesignTokens.SpacingXxxl
}
