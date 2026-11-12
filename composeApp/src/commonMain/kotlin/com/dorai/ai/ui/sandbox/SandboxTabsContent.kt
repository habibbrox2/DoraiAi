package com.dorai.ai.ui.sandbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.SandboxController
import com.dorai.ai.TerminalLine
import com.dorai.ai.linux.LinuxDistro
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.components.StatusBadge
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.settings.SandboxUiState
import com.dorai.ai.ui.settings.SettingsCard
import com.dorai.ai.ui.settings.TerminalContent
import com.dorai.ai.ui.settings.TerminalDarkBg
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.sandbox_retry_setup
import dorai.composeapp.generated.resources.sandbox_session_chip_session
import dorai.composeapp.generated.resources.sandbox_session_chip_temporary
import dorai.composeapp.generated.resources.sandbox_setup_persistence_note
import dorai.composeapp.generated.resources.sandbox_workspace_heading
import dorai.composeapp.generated.resources.settings_sandbox_cancel
import dorai.composeapp.generated.resources.settings_sandbox_description
import dorai.composeapp.generated.resources.settings_sandbox_distro_alpine_detail
import dorai.composeapp.generated.resources.settings_sandbox_distro_debian_detail
import dorai.composeapp.generated.resources.settings_sandbox_install
import dorai.composeapp.generated.resources.settings_sandbox_subtab_files
import dorai.composeapp.generated.resources.settings_sandbox_subtab_packages
import dorai.composeapp.generated.resources.settings_sandbox_subtab_terminal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

internal enum class SandboxSubTab { Terminal, Files, Packages }

@Composable
internal fun SandboxTabsContent(
    sandboxState: SandboxUiState,
    onSetupSandbox: () -> Unit = {},
    onCancelSandbox: () -> Unit = {},
    previewLines: ImmutableList<TerminalLine> = persistentListOf(),
    modifier: Modifier = Modifier,
) {
    val statusText = sandboxStatusText(sandboxState.sandboxStatusLabel)
    if (sandboxState.sandboxReady) {
        val isPreview = LocalInspectionMode.current
        val sandboxController: SandboxController? = if (!isPreview) koinInject() else null
        val sessionViewModel: SandboxSessionViewModel? = if (!isPreview) koinViewModel() else null
        var localSubTab by remember { mutableStateOf(SandboxSubTab.Terminal) }
        val subTab = sessionViewModel?.selectedTab?.collectAsStateWithLifecycle()?.value ?: localSubTab
        val onSelectTab: (SandboxSubTab) -> Unit = sessionViewModel?.let { vm ->
            { vm.selectTab(it) }
        } ?: { localSubTab = it }
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Common workspace header (Plan 4, Phase 4): one identity and one
            // readiness statement shared by Terminal, Files, and Packages.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(com.dorai.ai.ui.DoraiShapes.card)
                    .background(com.dorai.ai.ui.CardFill)
                    .border(com.dorai.ai.ui.DoraiBorders.soft()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = com.dorai.ai.ui.workspace.WorkspaceSpacing.md, vertical = com.dorai.ai.ui.workspace.WorkspaceSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.sandbox_workspace_heading),
                        style = com.dorai.ai.ui.DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    StatusBadge(status = OperationalStatus.Ready)
                }
            }

            SandboxSubTabSelector(currentTab = subTab, onSelectTab = onSelectTab)

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (subTab) {
                    SandboxSubTab.Terminal -> Column(modifier = Modifier.fillMaxSize()) {
                        if (sessionViewModel != null) {
                            SessionChipRow(viewModel = sessionViewModel)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = com.dorai.ai.ui.workspace.WorkspaceSpacing.xs)
                                .weight(1f)
                                .clip(com.dorai.ai.ui.DoraiShapes.card)
                                .background(TerminalDarkBg)
                                .border(com.dorai.ai.ui.DoraiBorders.soft()),
                        ) {
                            TerminalContent(
                                sandboxController = sandboxController,
                                modifier = Modifier.fillMaxSize(),
                                darkBackground = true,
                                initialLines = previewLines,
                                sessionViewModel = sessionViewModel,
                            )
                        }
                    }

                    SandboxSubTab.Files -> SandboxFilesContent(
                        modifier = Modifier.fillMaxSize(),
                    )

                    SandboxSubTab.Packages -> SandboxPackagesContent(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            SettingsCard {
                // Readiness flow (Plan 4, Phase 3): which environment, what state
                // it is in, what setup costs, progress, and an explicit retry —
                // the error is never the only thing on screen.
                val badgeStatus = when {
                    sandboxState.hasError -> OperationalStatus.Error
                    sandboxState.isWorking || sandboxState.sandboxProgress != null -> OperationalStatus.Installing
                    else -> OperationalStatus.NeedsSetup
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        // Distro comes from state — a hard-coded name goes stale the
                        // moment the user picks the other distribution.
                        text = sandboxState.distro.displayName,
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(status = badgeStatus)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.settings_sandbox_description),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )

                Spacer(Modifier.height(6.dp))

                // Storage expectation from the distro's own copy, then the session
                // expectation, so "what am I signing up for" is answered up front.
                Text(
                    text = when (sandboxState.distro) {
                        LinuxDistro.DEBIAN ->
                            stringResource(Res.string.settings_sandbox_distro_debian_detail)

                        LinuxDistro.ALPINE ->
                            stringResource(Res.string.settings_sandbox_distro_alpine_detail)
                    },
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Text(
                    text = stringResource(Res.string.sandbox_setup_persistence_note),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )

                if (sandboxState.sandboxProgress != null) {
                    SandboxProgressRow(sandboxState.sandboxProgress, statusText, onCancelSandbox)
                } else if (sandboxState.isWorking) {
                    SandboxProgressRow(null, statusText, onCancelSandbox)
                } else if (sandboxState.hasError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = DoraiTypography.bodySmall,
                        color = DangerAlert,
                    )
                }

                if (!sandboxState.isWorking) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onSetupSandbox, modifier = Modifier.handCursor()) {
                        // A failed attempt must offer a way out, not repeat the
                        // original invitation verbatim (Plan 4, Phase 3).
                        Text(
                            if (sandboxState.hasError) {
                                stringResource(Res.string.sandbox_retry_setup)
                            } else {
                                stringResource(Res.string.settings_sandbox_install)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionChipRow(viewModel: SandboxSessionViewModel) {
    val tabs = viewModel.visibleSessions.collectAsStateWithLifecycle().value
    val selectedId = viewModel.selectedSessionId.collectAsStateWithLifecycle().value
    if (tabs.size <= 1) return // Nothing to switch between — keep the UI quiet.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.id == selectedId
            val label = stringResource(
                if (tab.isTerminal) {
                    Res.string.sandbox_session_chip_temporary
                } else {
                    Res.string.sandbox_session_chip_session
                },
            )

            val buttonColors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) {
                    com.dorai.ai.ui.theme.DesignTokens.PrimaryCyan
                } else {
                    LocalDesignColorTokens.current.surfaceElevated
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.background
                } else {
                    com.dorai.ai.ui.theme.LocalDesignColorTokens.current.textSecondary
                },
            )

            Button(
                onClick = { viewModel.selectSession(tab.id) },
                modifier = Modifier
                    .handCursor()
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                colors = buttonColors,
                shape = com.dorai.ai.ui.DoraiShapes.pill,
                border = if (isSelected) com.dorai.ai.ui.DoraiBorders.accent() else com.dorai.ai.ui.DoraiBorders.soft(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = label,
                    style = com.dorai.ai.ui.DoraiTypography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SandboxSubTabSelector(
    currentTab: SandboxSubTab,
    onSelectTab: (SandboxSubTab) -> Unit,
) {
    val tabs = SandboxSubTab.entries
    val scrollState = rememberScrollState()
    val tabFocusRequesters = remember { tabs.map { _ -> FocusRequester() } }
    var focusedTabIndex by remember { mutableStateOf(tabs.indexOf(currentTab).coerceAtLeast(0)) }

    LaunchedEffect(currentTab) {
        focusedTabIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(4.dp)
            .onKeyEvent { event ->
                when (event.key) {
                    Key.DirectionLeft -> {
                        focusedTabIndex = (focusedTabIndex - 1).coerceIn(0, tabs.size - 1)
                        onSelectTab(tabs[focusedTabIndex])
                        true
                    }

                    Key.DirectionRight -> {
                        focusedTabIndex = (focusedTabIndex + 1).coerceIn(0, tabs.size - 1)
                        onSelectTab(tabs[focusedTabIndex])
                        true
                    }

                    Key.Enter, Key.NumPadEnter -> {
                        onSelectTab(tabs[focusedTabIndex])
                        true
                    }

                    else -> false
                }
            },
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = currentTab == tab
            val isFocused = focusedTabIndex == index

            val label = when (tab) {
                SandboxSubTab.Terminal -> stringResource(Res.string.settings_sandbox_subtab_terminal)
                SandboxSubTab.Files -> stringResource(Res.string.settings_sandbox_subtab_files)
                SandboxSubTab.Packages -> stringResource(Res.string.settings_sandbox_subtab_packages)
            }

            val buttonColors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isSelected -> com.dorai.ai.ui.theme.DesignTokens.PrimaryCyan
                    isFocused -> com.dorai.ai.ui.theme.DesignTokens.PrimaryCyan.copy(alpha = 0.25f)
                    else -> LocalDesignColorTokens.current.surfaceElevated
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.background
                } else {
                    com.dorai.ai.ui.theme.LocalDesignColorTokens.current.textSecondary
                },
            )

            val border = when {
                isSelected -> com.dorai.ai.ui.DoraiBorders.accent()
                isFocused -> com.dorai.ai.ui.DoraiBorders.focus()
                else -> com.dorai.ai.ui.DoraiBorders.soft()
            }

            Button(
                onClick = { onSelectTab(tab) },
                modifier = Modifier
                    .handCursor()
                    .focusRequester(tabFocusRequesters[index])
                    .onFocusChanged { focused ->
                        if (focused.isFocused) {
                            focusedTabIndex = index
                        }
                    }
                    .onKeyEvent { event ->
                        when (event.key) {
                            Key.DirectionLeft -> {
                                focusedTabIndex = (focusedTabIndex - 1).coerceIn(0, tabs.size - 1)
                                onSelectTab(tabs[focusedTabIndex])
                                true
                            }

                            Key.DirectionRight -> {
                                focusedTabIndex = (focusedTabIndex + 1).coerceIn(0, tabs.size - 1)
                                onSelectTab(tabs[focusedTabIndex])
                                true
                            }

                            Key.Enter, Key.NumPadEnter -> {
                                onSelectTab(tabs[focusedTabIndex])
                                true
                            }

                            else -> false
                        }
                    }
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                colors = buttonColors,
                shape = com.dorai.ai.ui.DoraiShapes.pill,
                border = border,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = label,
                    style = com.dorai.ai.ui.DoraiTypography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun SandboxProgressRow(progress: Float?, statusText: String, onCancel: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    if (progress != null) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = statusText,
            modifier = Modifier.weight(1f),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )
        TextButton(onClick = onCancel, modifier = Modifier.handCursor()) {
            Text(stringResource(Res.string.settings_sandbox_cancel))
        }
    }
}
