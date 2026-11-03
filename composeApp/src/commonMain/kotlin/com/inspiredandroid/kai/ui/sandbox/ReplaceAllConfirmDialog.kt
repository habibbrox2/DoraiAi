package com.inspiredandroid.kai.ui.sandbox

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.inspiredandroid.kai.ui.handCursor

/**
 * Confirmation before Replace all rewrites the buffer (Plan 3, Phase 5).
 *
 * Replacing every match is a single irreversible edit through the editor's
 * content path, so it requires an explicit decision, matching the destructive
 * action pattern used elsewhere in the app.
 */
@Composable
internal fun ReplaceAllConfirmDialog(
    matchCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace all matches?") },
        text = {
            Text(
                text = if (matchCount == 1) {
                    "This will replace the 1 match in this file."
                } else {
                    "This will replace all $matchCount matches in this file."
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier
                    .handCursor()
                    .semantics { contentDescription = "Confirm replace all" },
            ) {
                Text("Replace all")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .handCursor()
                    .semantics { contentDescription = "Cancel replace all" },
            ) {
                Text("Cancel")
            }
        },
    )
}
