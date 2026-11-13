package com.dorai.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.action_close
import org.jetbrains.compose.resources.stringResource

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
            .border(BorderStroke(1.dp, colors.border), DoraiShapes.pill)
            .semantics { liveRegion = LiveRegionMode.Polite },
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
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).padding(start = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.action_close),
                            tint = colors.text,
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
    // Tint the border/accent with the semantic color and keep textTheme text
    // primary — a saturated fill with onPrimary-ish text failed contrast on
    // light themes. Background uses the elevated surface, border carries the hue.
    return when (variant) {
        SnackbarVariant.Success ->
            SnackbarColors(
                background = colors.surfaceRaised,
                text = colors.textPrimary,
                border = colors.success.copy(alpha = 0.6f),
            )

        SnackbarVariant.Warning ->
            SnackbarColors(
                background = colors.surfaceRaised,
                text = colors.textPrimary,
                border = colors.warning.copy(alpha = 0.6f),
            )

        SnackbarVariant.Error ->
            SnackbarColors(
                background = colors.surfaceRaised,
                text = colors.textPrimary,
                border = colors.error.copy(alpha = 0.6f),
            )

        SnackbarVariant.Info ->
            SnackbarColors(
                background = colors.surfaceRaised,
                text = colors.textPrimary,
                border = colors.info.copy(alpha = 0.6f),
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
