package com.inspiredandroid.kai.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.inspiredandroid.kai.ui.DangerAlert
import com.inspiredandroid.kai.ui.KaiTypography

/**
 * Confirmation dialog for destructive settings actions (Plan 2, Phase 4).
 *
 * High-impact removals (delete a configured service, uninstall a skill, remove an
 * MCP server) should not fire on a single tap. This dialog spells out what is about
 * to happen and routes the decision back to the caller, which typically then performs
 * the removal and shows the existing undo snackbar.
 *
 * @param title the short confirmation question.
 * @param message the explanation of what will be removed or changed.
 * @param confirmLabel label for the destructive confirmation button.
 * @param onConfirm invoked when the user confirms the destructive action.
 * @param onDismiss invoked when the user cancels or dismisses the dialog.
 */
@Composable
fun SettingsConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title, style = KaiTypography.titleMedium) },
        text = { Text(message, style = KaiTypography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = DangerAlert)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
    )
}
