package com.dorai.ai.ui.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.VerticalScrollbarForList
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.conversations_clear_all
import dorai.composeapp.generated.resources.conversations_clear_all_confirm_action
import dorai.composeapp.generated.resources.conversations_clear_all_confirm_body
import dorai.composeapp.generated.resources.conversations_clear_all_confirm_title
import dorai.composeapp.generated.resources.data_manage_description
import dorai.composeapp.generated.resources.settings_configured_services_format
import dorai.composeapp.generated.resources.settings_page_account_description
import dorai.composeapp.generated.resources.settings_page_agent_description
import dorai.composeapp.generated.resources.settings_page_appearance_description
import dorai.composeapp.generated.resources.settings_page_general_description
import dorai.composeapp.generated.resources.settings_page_github_description
import dorai.composeapp.generated.resources.settings_page_services_description
import dorai.composeapp.generated.resources.settings_page_tools_description
import dorai.composeapp.generated.resources.settings_section_header_prefix
import dorai.composeapp.generated.resources.settings_tab_account
import dorai.composeapp.generated.resources.settings_tab_agent
import dorai.composeapp.generated.resources.settings_tab_appearance
import dorai.composeapp.generated.resources.settings_tab_data
import dorai.composeapp.generated.resources.settings_tab_general
import dorai.composeapp.generated.resources.settings_tab_github
import dorai.composeapp.generated.resources.settings_tab_services
import dorai.composeapp.generated.resources.settings_tab_tools
import dorai.composeapp.generated.resources.settings_version
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
                SettingsTab.Services -> 720.dp
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

                            SettingsTab.GitHub -> {
                                GitHubSection()
                            }

                            SettingsTab.Account -> {
                                DataControlContent(uiState = uiState, actions = actions)
                                AccountPlanContent(uiState = uiState)
                            }
                        }
                    }
                }
            }
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
fun DataControlContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    var showClearAllConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_tab_data),
            style = DoraiTypography.titleMedium,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        SettingsCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.data_manage_description),
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Button(onClick = { showClearAllConfirmationDialog = true }) {
                    Text(stringResource(Res.string.conversations_clear_all))
                }
            }
        }
    }

    if (showClearAllConfirmationDialog) {
        SettingsConfirmDialog(
            title = stringResource(Res.string.conversations_clear_all_confirm_title),
            message = stringResource(Res.string.conversations_clear_all_confirm_body),
            confirmLabel = stringResource(Res.string.conversations_clear_all_confirm_action),
            onConfirm = {
                showClearAllConfirmationDialog = false
                actions.onClearAllConversations()
            },
            onDismiss = {
                showClearAllConfirmationDialog = false
            },
        )
    }
}

@Composable
fun AccountPlanContent(
    uiState: SettingsUiState,
) {
    SettingsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "DORAI AI",
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            Text(
                text = stringResource(Res.string.settings_version, com.dorai.ai.Version.appVersion),
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
            Text(
                text = stringResource(Res.string.settings_configured_services_format, uiState.configuredServices.size),
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
    }
}

@Composable
private fun SettingsPageHeading(
    tab: SettingsTab,
    modifier: Modifier = Modifier,
) {
    val title = when (tab) {
        SettingsTab.General -> stringResource(Res.string.settings_tab_general)
        SettingsTab.Appearance -> stringResource(Res.string.settings_tab_appearance)
        SettingsTab.Agent -> stringResource(Res.string.settings_tab_agent)
        SettingsTab.Services -> stringResource(Res.string.settings_tab_services)
        SettingsTab.Tools -> stringResource(Res.string.settings_tab_tools)
        SettingsTab.GitHub -> stringResource(Res.string.settings_tab_github)
        SettingsTab.Account -> stringResource(Res.string.settings_tab_account)
    }
    val description = when (tab) {
        SettingsTab.General -> stringResource(Res.string.settings_page_general_description)
        SettingsTab.Appearance -> stringResource(Res.string.settings_page_appearance_description)
        SettingsTab.Agent -> stringResource(Res.string.settings_page_agent_description)
        SettingsTab.Services -> stringResource(Res.string.settings_page_services_description)
        SettingsTab.Tools -> stringResource(Res.string.settings_page_tools_description)
        SettingsTab.GitHub -> stringResource(Res.string.settings_page_github_description)
        SettingsTab.Account -> stringResource(Res.string.settings_page_account_description)
    }
    Column(
        modifier = modifier.semantics { heading() },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_section_header_prefix) + title.uppercase(),
            style = DoraiTypography.labelSmall,
            color = LocalDesignColorTokens.current.primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
        Text(
            text = title,
            style = DoraiTypography.headlineSmall,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Text(
            text = description,
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )
    }
}
