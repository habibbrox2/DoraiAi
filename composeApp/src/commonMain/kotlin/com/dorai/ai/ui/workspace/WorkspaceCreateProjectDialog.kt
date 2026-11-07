package com.dorai.ai.ui.workspace

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.dorai.ai.ui.handCursor
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.sandbox_files_rename_error_collision
import dorai.composeapp.generated.resources.sandbox_files_rename_error_invalid
import dorai.composeapp.generated.resources.workspace_new_project_title
import dorai.composeapp.generated.resources.workspace_projects_cancel_create
import dorai.composeapp.generated.resources.workspace_projects_create
import dorai.composeapp.generated.resources.workspace_projects_new_placeholder
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Why a new project name cannot be used yet. Blank input is not an error — the confirm stays disabled. */
enum class NewProjectNameError {
    /** Contains a path separator. */
    Invalid,

    /** A folder with that (trimmed) name already exists. */
    Collision,
}

/**
 * Pure validation for a new project name against the already-known [existing] folders.
 * [name] is trimmed first; a blank result returns null (confirm disabled, no error text).
 * Mirrors [RenameProjectDialog]'s invalid/collision rules so create and rename never drift.
 * Exported for tests.
 */
fun newProjectNameError(name: String, existing: List<String>): NewProjectNameError? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.contains('/')) return NewProjectNameError.Invalid
    if (trimmed in existing) return NewProjectNameError.Collision
    return null
}

/**
 * The single create-project dialog, shared by the Workspace explorer and the chat sidebar.
 * Collision feedback comes from [projects] before anything is attempted; the caller passes the
 * raw trimmed name to the ViewModel and the controller's sanitized return drives `openProject`
 * plus the async `scanProjects` rescan — the dialog never appends optimistically, so the list
 * shows the sanitized folder name once the rescan lands.
 */
@Composable
fun CreateProjectDialog(
    projects: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val trimmed = name.trim()
    val error = newProjectNameError(name, projects)
    val errorResource: StringResource? = when (error) {
        NewProjectNameError.Invalid -> Res.string.sandbox_files_rename_error_invalid
        NewProjectNameError.Collision -> Res.string.sandbox_files_rename_error_collision
        null -> null
    }
    val create = { if (trimmed.isNotEmpty() && error == null) onCreate(trimmed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.workspace_new_project_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.workspace_projects_new_placeholder)) },
                singleLine = true,
                isError = error != null,
                supportingText = errorResource?.let { res -> { Text(stringResource(res)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { create() }),
            )
        },
        confirmButton = {
            TextButton(
                onClick = create,
                enabled = trimmed.isNotEmpty() && error == null,
                modifier = Modifier.handCursor(),
            ) { Text(stringResource(Res.string.workspace_projects_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.handCursor()) {
                Text(stringResource(Res.string.workspace_projects_cancel_create))
            }
        },
    )
}
