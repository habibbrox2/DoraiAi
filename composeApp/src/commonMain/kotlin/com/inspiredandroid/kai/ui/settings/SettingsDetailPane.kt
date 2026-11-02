package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.linux.LinuxDistro
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.TextPrimary
import com.inspiredandroid.kai.ui.TextSecondary
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForList
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.settings_tab_data
import org.jetbrains.compose.resources.stringResource

/**
 * Detail pane for the settings screen (Plan 2, Phase 2). Renders the scrollable
 * content for the currently selected tab. Extracted so the adaptive shell can
 * place it beside a navigation rail on expanded windows.
 */
@Composable
fun SettingsDetailPane(
    uiState: SettingsUiState,
    actions: SettingsActions,
    sandboxState: SandboxUiState,
    onToggleSandbox: (Boolean) -> Unit,
    onSelectDistro: (LinuxDistro) -> Unit,
    onSetupSandbox: () -> Unit,
    onCancelSandbox: () -> Unit,
    onResetSandbox: () -> Unit,
    onInstallPackages: () -> Unit,
    onMigrateHome: () -> Unit,
    androidUpdateContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val settingsListState = rememberLazyListState()
    LaunchedEffect(uiState.currentTab) {
        settingsListState.scrollToItem(0)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = settingsListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Spacer(Modifier.height(20.dp)) }
            item {
                SettingsPageHeading(
                    tab = uiState.currentTab,
                    modifier = Modifier
                        .widthIn(max = 900.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
            item { Spacer(Modifier.height(18.dp)) }

            val maxContentWidth = when (uiState.currentTab) {
                SettingsTab.Services -> 500.dp
                else -> 900.dp
            }
            item {
                Column(
                    Modifier.widthIn(max = maxContentWidth).fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        when (uiState.currentTab) {
                            SettingsTab.General -> {
                                SettingsOverview(uiState = uiState, sandboxInstalled = sandboxState.sandboxInstalled)
                                GeneralContent(uiState = uiState, actions = actions, androidUpdateContent = androidUpdateContent)
                            }

                            SettingsTab.Appearance -> {
                                AppearanceContent(uiState = uiState, actions = actions)
                            }

                            SettingsTab.Agent -> {
                                AgentContent(uiState = uiState, actions = actions)
                            }

                            SettingsTab.Services -> {
                                ServicesContent(uiState = uiState, actions = actions)
                            }

                            SettingsTab.Tools -> {
                                ToolsTabContent(
                                    uiState = uiState,
                                    actions = actions,
                                    sandboxState = sandboxState,
                                )
                            }

                            SettingsTab.Integrations -> {
                                IntegrationsContent()
                            }

                            SettingsTab.Sandbox -> {
                                if (sandboxState.showSandbox) {
                                    SandboxSettingsCard(
                                        sandboxState = sandboxState,
                                        onToggleSandbox = onToggleSandbox,
                                        onSelectDistro = onSelectDistro,
                                        onSetupSandbox = onSetupSandbox,
                                        onCancelSandbox = onCancelSandbox,
                                        onResetSandbox = onResetSandbox,
                                        onInstallPackages = onInstallPackages,
                                        onMigrateHome = onMigrateHome,
                                    )
                                } else {
                                    SandboxUnavailableContent()
                                }
                            }

                            SettingsTab.Data -> {
                                DataControlContent(uiState = uiState, actions = actions)
                            }

                            SettingsTab.Account -> {
                                AccountPlanContent(uiState = uiState)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    BottomInfo()
                }
            }
        }

        VerticalScrollbarForList(
            listState = settingsListState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun SandboxUnavailableContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Linux Sandbox",
            style = KaiTypography.titleMedium,
            color = TextPrimary,
        )
        Text(
            text = "The Linux sandbox is only available on Android devices.",
            style = KaiTypography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
fun DataControlContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_tab_data),
            style = KaiTypography.titleMedium,
            color = TextPrimary,
        )
        Text(
            text = "Manage your chat history and stored data.",
            style = KaiTypography.bodySmall,
            color = TextSecondary,
        )
        Button(onClick = { actions.onClearAllConversations() }) {
            Text("Clear all conversations")
        }
    }
}

@Composable
fun AccountPlanContent(
    uiState: SettingsUiState,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Account and Plan",
            style = KaiTypography.titleMedium,
            color = TextPrimary,
        )
        Text(
            text = "Brox Kai 2.0",
            style = KaiTypography.bodyMedium,
            color = TextSecondary,
        )
        Text(
            text = "Version " + com.inspiredandroid.kai.Version.appVersion,
            style = KaiTypography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SettingsPageHeading(
    tab: SettingsTab,
    modifier: Modifier = Modifier,
) {
    val title = when (tab) {
        SettingsTab.General -> "General"
        SettingsTab.Appearance -> "Appearance"
        SettingsTab.Agent -> "Agent"
        SettingsTab.Services -> "Services"
        SettingsTab.Tools -> "Tools"
        SettingsTab.Integrations -> "Integrations"
        SettingsTab.Sandbox -> "Linux Sandbox"
        SettingsTab.Data -> "Data & Privacy"
        SettingsTab.Account -> "Account"
    }
    val description = when (tab) {
        SettingsTab.General -> "Manage your workspace preferences and application behavior."
        SettingsTab.Appearance -> "Tune the visual experience, theme, and accessibility."
        SettingsTab.Agent -> "Configure agent behavior, reasoning, and model defaults."
        SettingsTab.Services -> "Connect providers and manage the models available to Kai."
        SettingsTab.Tools -> "Control the tools and capabilities available to your agent."
        SettingsTab.Integrations -> "Manage MCP servers and external integrations."
        SettingsTab.Sandbox -> "Configure the isolated Linux workspace and its lifecycle."
        SettingsTab.Data -> "Manage conversations, local data, and privacy controls."
        SettingsTab.Account -> "Review your account and application information."
    }
    Column(
        modifier = modifier.semantics { heading() },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "SETTINGS / " + title.uppercase(),
            style = KaiTypography.labelSmall,
            color = com.inspiredandroid.kai.ui.theme.DesignTokens.PrimaryCyan,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
        Text(
            text = title,
            style = KaiTypography.headlineSmall,
            color = TextPrimary,
        )
        Text(
            text = description,
            style = KaiTypography.bodySmall,
            color = TextSecondary,
        )
    }
}
