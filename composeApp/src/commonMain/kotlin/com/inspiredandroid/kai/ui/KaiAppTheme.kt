package com.inspiredandroid.kai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.inspiredandroid.kai.ui.theme.DesignTokens
import com.inspiredandroid.kai.ui.theme.LocalDesignTokens

/**
 * Top‑level theme wrapper for the Brox Kai 2.0 (2027 Premium AI Agent Workspace).
 *
 * Provides the Material 3 colorScheme and injects the 2027 [DesignTokens] via [LocalDesignTokens]
 * so that any composable can access modern design tokens seamlessly.
 */
@Composable
fun KaiAppTheme(
    colorScheme: ColorScheme? = null,
    useOLED: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val scheme = colorScheme ?: if (systemInDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalDesignTokens provides DesignTokens,
    ) {
        Theme(
            colorScheme = scheme,
            content = content,
        )
    }
}
