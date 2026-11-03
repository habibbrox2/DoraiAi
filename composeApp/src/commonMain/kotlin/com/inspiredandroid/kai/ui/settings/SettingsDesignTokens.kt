package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.theme.DesignTokens
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens

/**
 * Shared design tokens for the Brox Kai 2.0 settings control center.
 * Updated for 2027 Premium AI Agent Workspace design system.
 */
object SettingsSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp
    val xxxl: Dp = 32.dp

    val minTouchTarget: Dp = 48.dp
    val sectionGap: Dp = 20.dp
    val cardGap: Dp = 16.dp
}

object SettingsShapes {
    val card = RoundedCornerShape(18.dp)
    val section = RoundedCornerShape(22.dp)
    val row = RoundedCornerShape(14.dp)
    val pill = RoundedCornerShape(999.dp)
    val input = RoundedCornerShape(14.dp)
    val button = RoundedCornerShape(999.dp)
    val panel = RoundedCornerShape(24.dp)
}

object SettingsStatusColors {
    val connected = DesignTokens.Success
    val checking = DesignTokens.Warning
    val error = DesignTokens.Error
    val unknown = DesignTokens.TertiaryText
    val attention = DesignTokens.Warning
    val glow = DesignTokens.PrimaryCyan
}

object SettingsSurfaces {
    /** Page background - dark theme */
    @Composable
    fun page(): Color = DesignTokens.Background

    /** Glassmorphic card background with blur effect */
    @Composable
    fun card(): Color = DesignTokens.SurfaceElevated

    /** Card border - subtle neural border */
    @Composable
    fun border(): Color = DesignTokens.BorderSubtle

    /** Raised surface for floating elements */
    @Composable
    fun raised(): Color = DesignTokens.SurfaceRaised

    /** Elevated surface for panels */
    @Composable
    fun elevated(): Color = DesignTokens.SurfaceElevated

    /** Primary accent color for interactive elements */
    @Composable
    fun accent(): Color = DesignTokens.PrimaryCyan

    /** Text colors */
    @Composable
    fun textPrimary(): Color = LocalDesignColorTokens.current.textPrimary

    @Composable
    fun textSecondary(): Color = LocalDesignColorTokens.current.textSecondary

    @Composable
    fun textTertiary(): Color = LocalDesignColorTokens.current.textTertiary

    /** Danger/destructive color */
    @Composable
    fun danger(): Color = DesignTokens.Error
}




