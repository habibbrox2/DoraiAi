package com.dorai.ai.ui.sandbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.action_close
import dorai.composeapp.generated.resources.find_replace_find
import dorai.composeapp.generated.resources.find_replace_match_many
import dorai.composeapp.generated.resources.find_replace_match_one
import dorai.composeapp.generated.resources.find_replace_no_matches
import dorai.composeapp.generated.resources.find_replace_replace_all
import dorai.composeapp.generated.resources.find_replace_replace_with
import dorai.composeapp.generated.resources.find_replace_type_prompt
import org.jetbrains.compose.resources.stringResource

/**
 * File-scoped Find and Replace panel (Plan 3, Phase 5).
 *
 * Purely presentational: it reports the match count for the current query and
 * delegates replacement to the caller through the editor's existing content
 * change path, so replace-all can never bypass dirty tracking or Save.
 */
@Composable
internal fun FindReplacePanel(
    text: String,
    replacement: String,
    onReplacementChange: (String) -> Unit,
    onReplaceAll: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var confirmReplaceAll by remember { mutableStateOf(false) }

    val matchCount = if (query.isEmpty()) 0 else countMatches(text, query)

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(Res.string.find_replace_find)) },
            )
            OutlinedTextField(
                value = replacement,
                onValueChange = onReplacementChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(Res.string.find_replace_replace_with)) },
            )
            TextButton(onClick = onClose, modifier = Modifier.handCursor()) {
                Text(stringResource(Res.string.action_close))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    query.isEmpty() -> stringResource(Res.string.find_replace_type_prompt)
                    matchCount == 0 -> stringResource(Res.string.find_replace_no_matches)
                    matchCount == 1 -> stringResource(Res.string.find_replace_match_one)
                    else -> stringResource(Res.string.find_replace_match_many, matchCount)
                },
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { confirmReplaceAll = true },
                enabled = matchCount > 0,
                modifier = Modifier.handCursor(),
            ) {
                Text(
                    text = stringResource(Res.string.find_replace_replace_all),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }

    // Replacement rewrites the whole buffer through the normal content-change
    // path, so it stays undoable in the editor's own dirty/Save model.
    if (confirmReplaceAll) {
        ReplaceAllConfirmDialog(
            matchCount = matchCount,
            onConfirm = {
                confirmReplaceAll = false
                onReplaceAll(query, replacement)
            },
            onDismiss = { confirmReplaceAll = false },
        )
    }
}

/** Case-sensitive literal match count; plain scan keeps large files off the regex path. */
internal fun countMatches(text: String, query: String): Int {
    if (query.isEmpty()) return 0
    var count = 0
    var index = text.indexOf(query)
    while (index >= 0) {
        count++
        index = text.indexOf(query, index + query.length)
    }
    return count
}
