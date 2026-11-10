package com.dorai.ai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.github.CloneState
import com.dorai.ai.github.GitHubConnectionStatus
import com.dorai.ai.github.GitHubIntegrationViewModel
import com.dorai.ai.github.GitHubRepository
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_github_clone_button
import dorai.composeapp.generated.resources.settings_github_clone_cancel
import dorai.composeapp.generated.resources.settings_github_cloned
import dorai.composeapp.generated.resources.settings_github_cloning
import dorai.composeapp.generated.resources.settings_github_confirm_clone_message
import dorai.composeapp.generated.resources.settings_github_confirm_clone_title
import dorai.composeapp.generated.resources.settings_github_connect
import dorai.composeapp.generated.resources.settings_github_connected
import dorai.composeapp.generated.resources.settings_github_description
import dorai.composeapp.generated.resources.settings_github_error
import dorai.composeapp.generated.resources.settings_github_last_sync
import dorai.composeapp.generated.resources.settings_github_not_connected
import dorai.composeapp.generated.resources.settings_github_project_name_hint
import dorai.composeapp.generated.resources.settings_github_project_name_label
import dorai.composeapp.generated.resources.settings_github_refresh_status
import dorai.composeapp.generated.resources.settings_github_repositories
import dorai.composeapp.generated.resources.settings_github_section_title
import dorai.composeapp.generated.resources.settings_github_sync
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GitHubSection() {
    val vm: GitHubIntegrationViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = LocalDesignColorTokens.current

    val showConfirm = remember { mutableStateOf(false) }
    val projectName = remember { mutableStateOf("") }
    val repositoryToClone = remember { mutableStateOf<GitHubRepository?>(null) }

    fun onRepositorySelected(repo: GitHubRepository) {
        repositoryToClone.value = repo
        projectName.value = repo.name
        showConfirm.value = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_github_section_title),
            style = DoraiTypography.titleLarge,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(Res.string.settings_github_description),
            style = DoraiTypography.bodyMedium,
            color = colors.textSecondary,
        )

        // Connection status + connect button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.connectionStatus == GitHubConnectionStatus.Connected) {
                    stringResource(Res.string.settings_github_connected)
                } else {
                    stringResource(Res.string.settings_github_not_connected)
                },
                style = DoraiTypography.bodyMedium,
                color = colors.textPrimary,
            )
            if (state.connectionStatus != GitHubConnectionStatus.Connected) {
                Button(
                    onClick = { vm.connect() },
                    modifier = Modifier.width(140.dp),
                ) {
                    Text(text = stringResource(Res.string.settings_github_connect))
                }
            }
        }

        // Repository list (read-only browsing)
        Text(
            text = stringResource(Res.string.settings_github_repositories),
            style = DoraiTypography.titleMedium,
            color = colors.textPrimary,
        )
        if (state.repositories.isEmpty()) {
            Text(
                text = "No repositories available.",
                style = DoraiTypography.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            state.repositories.forEach { repo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRepositorySelected(repo) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = repo.fullName,
                        style = DoraiTypography.bodyMedium,
                        color = colors.textPrimary,
                    )
                    if (repositoryToClone.value == repo) {
                        IconButton(onClick = { repositoryToClone.value = null }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }

        // Clone controls (only after selecting a repository)
        repositoryToClone.value?.let { repo ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.settings_github_project_name_label),
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                )
                OutlinedTextField(
                    value = projectName.value,
                    onValueChange = { projectName.value = it },
                    label = { Text(text = stringResource(Res.string.settings_github_project_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { vm.refreshGitStatus() },
                        enabled = state.cloneState !is CloneState.Cloning,
                    ) {
                        Text(text = stringResource(Res.string.settings_github_refresh_status))
                    }
                    TextButton(
                        onClick = { vm.sync() },
                        enabled = state.cloneState !is CloneState.Cloning,
                    ) {
                        Text(text = stringResource(Res.string.settings_github_sync))
                    }
                    Button(
                        onClick = { showConfirm.value = true },
                        enabled = projectName.value.isNotBlank() && state.cloneState !is CloneState.Cloning,
                        modifier = Modifier.width(100.dp),
                    ) {
                        Text(text = stringResource(Res.string.settings_github_clone_button))
                    }
                }

                // Clone status
                when (val clone = state.cloneState) {
                    is CloneState.Cloning -> {
                        Text(
                            text = stringResource(Res.string.settings_github_cloning),
                            style = DoraiTypography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }

                    is CloneState.Success -> {
                        Text(
                            text = stringResource(Res.string.settings_github_cloned),
                            style = DoraiTypography.bodySmall,
                            color = colors.textPrimary,
                        )
                    }

                    is CloneState.Failure -> {
                        Text(
                            text = clone.error,
                            style = DoraiTypography.bodySmall,
                            color = colors.error,
                        )
                    }

                    CloneState.Idle -> {}
                }

                state.lastSync?.let { instant ->
                    Text(
                        text = stringResource(Res.string.settings_github_last_sync, instant.toString()),
                        style = DoraiTypography.bodySmall,
                        color = colors.textSecondary,
                    )
                }

                state.lastError?.let { error ->
                    Text(
                        text = stringResource(Res.string.settings_github_error, error),
                        style = DoraiTypography.bodySmall,
                        color = colors.error,
                    )
                }
            }
        }

        // Confirmation dialog
        if (showConfirm.value && repositoryToClone.value != null) {
            val repo = repositoryToClone.value!!
            AlertDialog(
                onDismissRequest = {
                    showConfirm.value = false
                    repositoryToClone.value = null
                },
                title = {
                    Text(text = stringResource(Res.string.settings_github_confirm_clone_title))
                },
                text = {
                    Text(
                        text = stringResource(
                            Res.string.settings_github_confirm_clone_message,
                            repo.fullName,
                            projectName.value.ifBlank { "<folder>" },
                        ),
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = projectName.value.ifBlank { repo.name }
                            vm.selectAndClone(repo, name)
                            showConfirm.value = false
                            repositoryToClone.value = null
                        },
                    ) {
                        Text(text = stringResource(Res.string.settings_github_clone_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showConfirm.value = false
                            repositoryToClone.value = null
                        },
                    ) {
                        Text(text = stringResource(Res.string.settings_github_clone_cancel))
                    }
                },
            )
        }
    }
}
