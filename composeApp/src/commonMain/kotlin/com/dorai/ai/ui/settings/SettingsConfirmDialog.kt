package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.DesignTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_confirm_dialog_cancel
import org.jetbrains.compose.resources.stringResource

/** Interaction-state color for the destructive confirm button. */
internal fun confirmationConfirmColor(enabled: Boolean, destructiveColor: Color): Color = if (enabled) destructiveColor else destructiveColor.copy(alpha = 0.38f)

/**
 * Canonical confirmation dialog for destructive settings actions.
 *
 * High-impact removals (delete a configured service, uninstall a skill, remove an
 * MCP server) should not fire on a single tap. This dialog spells out what is about
 * to happen and routes the decision back to the caller, which typically then performs
 * the removal and shows the existing undo snackbar.
 *
 * Interaction states follow the shared spec: enabled uses the destructive token,
 * disabled fades it; pressed/focused use Material 3 defaults via ButtonDefaults.
 *
 * @param title the short confirmation question.
 * @param message the explanation of what will be removed or changed.
 * @param confirmLabel label for the destructive confirmation button.
 * @param onConfirm invoked when the user confirms the destructive action.
 * @param onDismiss invoked when the user cancels or dismisses the dialog.
 * @param enabled when false the confirm button cannot be pressed (loading/disabled state).
 */
@Composable
fun SettingsConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    enabled: Boolean = true,
    /** Override for non-destructive confirmations (e.g. opting into a plugin). */
    confirmColor: Color = DangerAlert,
) {
    val resolvedDismissLabel = dismissLabel ?: stringResource(Res.string.settings_confirm_dialog_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title, style = DoraiTypography.titleMedium) },
        text = { Text(message, style = DoraiTypography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = confirmationConfirmColor(enabled, confirmColor),
                    disabledContentColor = confirmationConfirmColor(false, confirmColor),
                ),
                modifier = Modifier.defaultMinSize(minHeight = DesignTokens.MinTouchTarget),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = DesignTokens.MinTouchTarget),
            ) {
                Text(resolvedDismissLabel)
            }
        },
    )
}

/** Backwards-compatible alias; new code should use [SettingsConfirmationDialog]. */
@Composable
fun SettingsConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
) {
    SettingsConfirmationDialog(
        title = title,
        message = message,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        dismissLabel = dismissLabel,
    )
}
