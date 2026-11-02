package com.inspiredandroid.kai.ui.sandbox

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
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens

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
                label = { Text("Find") },
            )
            OutlinedTextField(
                value = replacement,
                onValueChange = onReplacementChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Replace with") },
            )
            TextButton(onClick = onClose, modifier = Modifier.handCursor()) {
                Text("Close")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    query.isEmpty() -> "Type to search this file"
                    matchCount == 0 -> "No matches"
                    matchCount == 1 -> "1 match"
                    else -> "$matchCount matches"
                },
                style = KaiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { confirmReplaceAll = true },
                enabled = matchCount > 0,
                modifier = Modifier.handCursor(),
            ) {
                Text(
                    text = "Replace all",
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
