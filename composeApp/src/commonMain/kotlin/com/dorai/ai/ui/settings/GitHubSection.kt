package com.dorai.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.Platform
import com.dorai.ai.currentPlatform
import com.dorai.ai.github.CloneState
import com.dorai.ai.github.GitHubConnectionStatus
import com.dorai.ai.github.GitHubDeviceFlowError
import com.dorai.ai.github.GitHubIntegrationViewModel
import com.dorai.ai.github.GitHubRepository
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.components.StatusBadge
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.rememberCopyToClipboard
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.github_mark
import dorai.composeapp.generated.resources.settings_github_account_connected
import dorai.composeapp.generated.resources.settings_github_cancel_flow
import dorai.composeapp.generated.resources.settings_github_clear_selection
import dorai.composeapp.generated.resources.settings_github_clone_button
import dorai.composeapp.generated.resources.settings_github_clone_cancel
import dorai.composeapp.generated.resources.settings_github_clone_error_failed
import dorai.composeapp.generated.resources.settings_github_clone_error_missing_git
import dorai.composeapp.generated.resources.settings_github_clone_error_not_repository
import dorai.composeapp.generated.resources.settings_github_cloned
import dorai.composeapp.generated.resources.settings_github_cloning
import dorai.composeapp.generated.resources.settings_github_code_copied
import dorai.composeapp.generated.resources.settings_github_confirm_clone_message
import dorai.composeapp.generated.resources.settings_github_confirm_clone_title
import dorai.composeapp.generated.resources.settings_github_connect
import dorai.composeapp.generated.resources.settings_github_connected
import dorai.composeapp.generated.resources.settings_github_connecting
import dorai.composeapp.generated.resources.settings_github_copy_code
import dorai.composeapp.generated.resources.settings_github_description
import dorai.composeapp.generated.resources.settings_github_disconnect
import dorai.composeapp.generated.resources.settings_github_disconnect_confirm_message
import dorai.composeapp.generated.resources.settings_github_disconnect_confirm_title
import dorai.composeapp.generated.resources.settings_github_dismiss
import dorai.composeapp.generated.resources.settings_github_error
import dorai.composeapp.generated.resources.settings_github_error_access_denied
import dorai.composeapp.generated.resources.settings_github_error_device_disabled
import dorai.composeapp.generated.resources.settings_github_error_expired
import dorai.composeapp.generated.resources.settings_github_error_network
import dorai.composeapp.generated.resources.settings_github_error_not_configured
import dorai.composeapp.generated.resources.settings_github_error_rate_limited
import dorai.composeapp.generated.resources.settings_github_error_section_title
import dorai.composeapp.generated.resources.settings_github_error_unauthorized
import dorai.composeapp.generated.resources.settings_github_hero_title
import dorai.composeapp.generated.resources.settings_github_installations
import dorai.composeapp.generated.resources.settings_github_last_sync
import dorai.composeapp.generated.resources.settings_github_loading
import dorai.composeapp.generated.resources.settings_github_no_repositories
import dorai.composeapp.generated.resources.settings_github_no_repositories_hint
import dorai.composeapp.generated.resources.settings_github_not_connected
import dorai.composeapp.generated.resources.settings_github_not_supported
import dorai.composeapp.generated.resources.settings_github_open_github
import dorai.composeapp.generated.resources.settings_github_project_name_hint
import dorai.composeapp.generated.resources.settings_github_project_name_label
import dorai.composeapp.generated.resources.settings_github_refresh_repositories
import dorai.composeapp.generated.resources.settings_github_refresh_status
import dorai.composeapp.generated.resources.settings_github_repositories
import dorai.composeapp.generated.resources.settings_github_retry
import dorai.composeapp.generated.resources.settings_github_signed_in
import dorai.composeapp.generated.resources.settings_github_sync
import dorai.composeapp.generated.resources.settings_github_user_code
import dorai.composeapp.generated.resources.settings_github_waiting_description
import dorai.composeapp.generated.resources.settings_github_waiting_title
import kotlinx.coroutines.delay
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GitHubSection() {
    val colors = LocalDesignColorTokens.current

    // GitHub integration is Android + desktop only; iOS/Web show an unsupported notice.
    if (currentPlatform !is Platform.Mobile.Android && currentPlatform !is Platform.Desktop) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.settings_github_not_supported),
                style = DoraiTypography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        return
    }

    val vm: GitHubIntegrationViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    val showConfirm = remember { mutableStateOf(false) }
    val showDisconnectConfirm = remember { mutableStateOf(false) }
    val projectName = remember { mutableStateOf("") }
    val repositoryToClone = remember { mutableStateOf<GitHubRepository?>(null) }

    fun onRepositorySelected(repo: GitHubRepository) {
        repositoryToClone.value = repo
        // Only prefill when the field was empty or already bound to this same repo —
        // never clobber an in-flight edit.
        if (projectName.value.isBlank() || projectName.value == repo.name) {
            projectName.value = repo.name
        }
        showConfirm.value = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero card: the GitHub mark, the current state and the one action that changes it.
        // The category chip already says "GitHub", so the headline states the *state* instead
        // of repeating the name.
        val isConnected = state.connectionStatus == GitHubConnectionStatus.Connected
        val isWaiting = state.connectionStatus == GitHubConnectionStatus.WaitingForDevice
        val isFailed = state.connectionStatus == GitHubConnectionStatus.Error
        val headline = when {
            isConnected -> state.user?.login?.let {
                stringResource(Res.string.settings_github_signed_in, it)
            } ?: stringResource(Res.string.settings_github_account_connected)

            isWaiting -> stringResource(Res.string.settings_github_waiting_title)

            else -> stringResource(Res.string.settings_github_hero_title)
        }
        val statusLabel = when {
            isConnected -> stringResource(Res.string.settings_github_connected)
            isWaiting -> stringResource(Res.string.settings_github_waiting_title)
            isFailed -> stringResource(Res.string.settings_github_error_section_title)
            else -> stringResource(Res.string.settings_github_not_connected)
        }
        val statusKind = when {
            isConnected -> OperationalStatus.Connected
            isWaiting -> OperationalStatus.Connecting
            isFailed -> OperationalStatus.Error
            else -> OperationalStatus.NeedsSetup
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = doraiAdaptiveCardColors(),
            border = doraiAdaptiveCardBorder(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceRaised)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.github_mark),
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headline,
                            style = DoraiTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        StatusBadge(label = statusLabel, status = statusKind)
                    }
                }

                Text(
                    text = stringResource(Res.string.settings_github_description),
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (isConnected) {
                        OutlinedButton(onClick = { showDisconnectConfirm.value = true }) {
                            Text(text = stringResource(Res.string.settings_github_disconnect))
                        }
                    } else if (!isWaiting) {
                        Button(
                            onClick = { vm.connect() },
                            enabled = !state.isLoading,
                        ) {
                            Text(
                                text = if (state.isLoading) {
                                    stringResource(Res.string.settings_github_connecting)
                                } else {
                                    stringResource(Res.string.settings_github_connect)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Signed-in identity + installations (Connected only)
        if (state.connectionStatus == GitHubConnectionStatus.Connected) {
            if (state.installations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_github_installations),
                        style = DoraiTypography.titleSmall,
                        color = colors.textPrimary,
                    )
                    state.installations.forEach { installation ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = installation.account.login,
                                style = DoraiTypography.bodyMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = installation.account.type,
                                style = DoraiTypography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }

        // Waiting-for-authorization card (cancelled when this tab leaves composition)
        if (state.connectionStatus == GitHubConnectionStatus.WaitingForDevice && state.userCode != null) {
            DeviceFlowWaitingCard(
                userCode = state.userCode!!,
                verificationUri = state.verificationUri,
                onCancel = { vm.cancelDeviceFlow() },
            )
        }

        // Error banner: a dead session gets the full card with Retry; a recoverable
        // failure (rate limit, transient network) stays a dismissible non-fatal banner.
        if (state.error != null && state.connectionStatus == GitHubConnectionStatus.Error) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                    .padding(DoraiSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_github_error_section_title),
                    style = DoraiTypography.titleSmall,
                    color = colors.error,
                )
                Text(
                    text = githubErrorMessage(state.error!!),
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                )
                OutlinedButton(onClick = { vm.retry() }) {
                    Text(text = stringResource(Res.string.settings_github_retry))
                }
            }
        } else if (state.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = githubErrorMessage(state.error!!),
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                IconButton(
                    onClick = { vm.dismissError() },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.settings_github_dismiss),
                    )
                }
            }
        }

        // Repository list (read-only browsing)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.settings_github_repositories),
                    style = DoraiTypography.titleMedium,
                    color = colors.textPrimary,
                )
                if (state.repositories.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.repositories.size.toString(),
                        style = DoraiTypography.labelMedium,
                        color = colors.textTertiary,
                    )
                }
            }
            if (state.connectionStatus == GitHubConnectionStatus.Connected) {
                TextButton(
                    onClick = { vm.refreshRepositories() },
                    enabled = !state.isLoading,
                ) {
                    Text(text = stringResource(Res.string.settings_github_refresh_repositories))
                }
            }
        }
        if (state.repositories.isEmpty()) {
            if (state.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.settings_github_loading),
                        style = DoraiTypography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            } else {
                CollectionEmptyState(
                    message = if (state.connectionStatus == GitHubConnectionStatus.Connected) {
                        stringResource(Res.string.settings_github_no_repositories_hint)
                    } else {
                        stringResource(Res.string.settings_github_no_repositories)
                    },
                )
            }
        } else {
            state.repositories.forEach { repo ->
                val isSelected = repositoryToClone.value == repo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(SettingsShapes.row)
                        .background(if (isSelected) colors.surfaceElevated else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.borderStrong else colors.borderSubtle,
                            shape = SettingsShapes.row,
                        )
                        .clickable(
                            role = Role.Button,
                            onClick = { onRepositorySelected(repo) },
                            onClickLabel = repo.fullName,
                        )
                        .semantics {
                            if (isSelected) selected = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = repo.fullName,
                        style = DoraiTypography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isSelected) {
                        IconButton(
                            onClick = { repositoryToClone.value = null },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(Res.string.settings_github_clear_selection),
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
                            text = cloneErrorMessage(clone.error)
                                ?: stringResource(Res.string.settings_github_error, clone.error),
                            style = DoraiTypography.bodySmall,
                            color = colors.error,
                        )
                    }

                    CloneState.Idle -> {}
                }

                state.lastSync?.let { instant ->
                    val local = kotlinx.datetime.Instant.fromEpochMilliseconds(instant.toEpochMilliseconds())
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    Text(
                        text = stringResource(
                            Res.string.settings_github_last_sync,
                            "${local.day}/${local.monthNumber}/${local.year} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}",
                        ),
                        style = DoraiTypography.bodySmall,
                        color = colors.textSecondary,
                    )
                }

                state.lastError?.let { error ->
                    Text(
                        text = cloneErrorMessage(error) ?: stringResource(Res.string.settings_github_error, error),
                        style = DoraiTypography.bodySmall,
                        color = colors.error,
                    )
                }
            }
        }

        // Confirmation dialog (clone)
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
                            projectName.value.ifBlank { repo.name },
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

        // Confirmation dialog (disconnect)
        if (showDisconnectConfirm.value) {
            AlertDialog(
                onDismissRequest = { showDisconnectConfirm.value = false },
                title = {
                    Text(text = stringResource(Res.string.settings_github_disconnect_confirm_title))
                },
                text = {
                    Text(text = stringResource(Res.string.settings_github_disconnect_confirm_message))
                },
                confirmButton = {
                    Button(onClick = {
                        vm.disconnect()
                        showDisconnectConfirm.value = false
                    }) {
                        Text(text = stringResource(Res.string.settings_github_disconnect))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisconnectConfirm.value = false }) {
                        Text(text = stringResource(Res.string.settings_github_clone_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun DeviceFlowWaitingCard(
    userCode: String,
    verificationUri: String?,
    onCancel: () -> Unit,
) {
    val colors = LocalDesignColorTokens.current
    val copyToClipboard = rememberCopyToClipboard()
    val uriHandler = LocalUriHandler.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    // Leaving the GitHub tab must not leave a background poll loop running — abort the flow.
    DisposableEffect(Unit) {
        onDispose { onCancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
            .padding(DoraiSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colors.primary,
            )
            Text(
                text = stringResource(Res.string.settings_github_waiting_title),
                style = DoraiTypography.titleSmall,
                color = colors.textPrimary,
            )
        }

        Text(
            text = stringResource(
                Res.string.settings_github_waiting_description,
                verificationUri ?: "github.com/login/device",
            ),
            style = DoraiTypography.bodySmall,
            color = colors.textSecondary,
        )

        Text(
            text = stringResource(Res.string.settings_github_user_code),
            style = DoraiTypography.labelSmall,
            color = colors.textSecondary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = userCode,
                    style = DoraiTypography.titleLarge,
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = {
                    copyToClipboard(userCode)
                    copied = true
                }) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = stringResource(if (copied) Res.string.settings_github_code_copied else Res.string.settings_github_copy_code),
                        tint = colors.primary,
                    )
                }
            }
        }

        verificationUri?.let { uri ->
            Button(onClick = { uriHandler.openUri(uri) }) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(Res.string.settings_github_open_github),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        TextButton(onClick = onCancel) {
            Text(text = stringResource(Res.string.settings_github_cancel_flow))
        }
    }
}

@Composable
private fun githubErrorMessage(error: GitHubDeviceFlowError): String = when (error) {
    GitHubDeviceFlowError.MissingClientId -> stringResource(Res.string.settings_github_error_not_configured)

    GitHubDeviceFlowError.ExpiredToken -> stringResource(Res.string.settings_github_error_expired)

    GitHubDeviceFlowError.AccessDenied -> stringResource(Res.string.settings_github_error_access_denied)

    GitHubDeviceFlowError.DeviceFlowDisabled -> stringResource(Res.string.settings_github_error_device_disabled)

    GitHubDeviceFlowError.BadRefreshToken -> stringResource(Res.string.settings_github_error_network)

    GitHubDeviceFlowError.AuthorizationPending,
    GitHubDeviceFlowError.SlowDown,
    -> stringResource(Res.string.settings_github_waiting_title)

    is GitHubDeviceFlowError.Network -> {
        val message = error.message
        when {
            message.contains("rate limit", ignoreCase = true) -> stringResource(Res.string.settings_github_error_rate_limited)
            message.contains("unauthorized", ignoreCase = true) -> stringResource(Res.string.settings_github_error_unauthorized)
            else -> stringResource(Res.string.settings_github_error_network)
        }
    }
}

/**
 * Maps the internal git error vocabulary (enum names from WorkspaceGitResult/WorkspaceGitError,
 * or a pre-existing user-facing message) to localized text. Returns null for messages that do
 * not match a known key so callers can fall back to the generic wrapper.
 */
@Composable
private fun cloneErrorMessage(raw: String): String? = when (raw) {
    "NotRepository" -> stringResource(Res.string.settings_github_clone_error_not_repository)
    "MissingGit" -> stringResource(Res.string.settings_github_clone_error_missing_git)
    "Failed" -> stringResource(Res.string.settings_github_clone_error_failed)
    else -> null
}
