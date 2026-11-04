package com.dorai.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.InfoBlue
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SuccessGreen
import com.dorai.ai.ui.WarningAmber
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/** Color configuration for a DoraiSnackbar variant. */
private data class SnackbarColors(
    val background: Color,
    val text: Color,
    val border: Color,
)

/**
 * Lightweight toast/snackbar shown at the bottom of the screen.
 *
 * @param message Message text
 * @param variant Visual variant: info, success, warning, or error
 * @param onDismiss Called when the user dismisses the snackbar
 * @param modifier Compose modifier
 */
@Composable
fun DoraiSnackbar(
    message: String,
    variant: SnackbarVariant = SnackbarVariant.Info,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = snackbarColors(variant)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(DoraiShapes.pill)
            .background(colors.background, DoraiShapes.pill)
            .border(BorderStroke(1.dp, colors.border), DoraiShapes.pill),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.text) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message,
                    style = DoraiTypography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                onDismiss?.let {
                    IconButton(
                        onClick = it,
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        androidx.compose.material3.Text(
                            text = "✕",
                            style = DoraiTypography.labelSmall,
                            color = colors.text,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun snackbarColors(
    variant: SnackbarVariant,
): SnackbarColors {
    val colors = LocalDesignColorTokens.current
    return when (variant) {
        SnackbarVariant.Success ->
            SnackbarColors(
                background = SuccessGreen,
                text = colors.textPrimary,
                border = SuccessGreen.copy(alpha = 0.4f),
            )

        SnackbarVariant.Warning ->
            SnackbarColors(
                background = WarningAmber,
                text = colors.textPrimary,
                border = WarningAmber.copy(alpha = 0.4f),
            )

        SnackbarVariant.Error ->
            SnackbarColors(
                background = DangerAlert,
                text = colors.textPrimary,
                border = DangerAlert.copy(alpha = 0.4f),
            )

        SnackbarVariant.Info ->
            SnackbarColors(
                background = InfoBlue,
                text = colors.textPrimary,
                border = InfoBlue.copy(alpha = 0.4f),
            )
    }
}

/** Visual variants for DoraiSnackbar. */
enum class SnackbarVariant {
    Info,
    Success,
    Warning,
    Error,
}
