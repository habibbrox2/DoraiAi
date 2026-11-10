package com.dorai.ai.ui.sandbox

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dorai.ai.ui.handCursor
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_cancel_replace_all
import dorai.composeapp.generated.resources.a11y_confirm_replace_all
import dorai.composeapp.generated.resources.action_cancel
import dorai.composeapp.generated.resources.find_replace_replace_all
import dorai.composeapp.generated.resources.find_replace_replace_all_body_many
import dorai.composeapp.generated.resources.find_replace_replace_all_body_one
import dorai.composeapp.generated.resources.find_replace_replace_all_confirm_title
import org.jetbrains.compose.resources.stringResource

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
    val confirmDescription = stringResource(Res.string.a11y_confirm_replace_all)
    val cancelDescription = stringResource(Res.string.a11y_cancel_replace_all)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.find_replace_replace_all_confirm_title)) },
        text = {
            Text(
                text = if (matchCount == 1) {
                    stringResource(Res.string.find_replace_replace_all_body_one)
                } else {
                    stringResource(Res.string.find_replace_replace_all_body_many, matchCount)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier
                    .handCursor()
                    .semantics { contentDescription = confirmDescription },
            ) {
                Text(stringResource(Res.string.find_replace_replace_all))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .handCursor()
                    .semantics { contentDescription = cancelDescription },
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
