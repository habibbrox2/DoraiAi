package com.dorai.ai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dorai.ai.mcp.PopularMcpServer
import com.dorai.ai.network.tools.ToolInfo
import com.dorai.ai.skills.RegistrySkillEntry
import com.dorai.ai.skills.SkillManifest
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.components.DoraiSearchField
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_plugins
import dorai.composeapp.generated.resources.settings_plugins_description
import dorai.composeapp.generated.resources.settings_plugins_no_match
import dorai.composeapp.generated.resources.settings_plugins_search
import dorai.composeapp.generated.resources.settings_tools_none_available
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * The merged Tools tab. Three library surfaces share one shell:
 * - Connectors: MCP servers you own plus curated ones to add.
 * - Skills: installed slash-command skills plus the registry.
 * - Plugins: the native tools the agent can run.
 */
@Composable
internal fun ToolsContent(
    tools: ImmutableList<ToolInfo>,
    onToggleTool: (String, Boolean) -> Unit,
    mcpServers: ImmutableList<McpServerUiState>,
    onAddMcpServer: (String, String, Map<String, String>) -> Unit,
    onRemoveMcpServer: (String) -> Unit,
    onToggleMcpServer: (String, Boolean) -> Unit,
    onRefreshMcpServer: (String) -> Unit,
    showAddMcpServerDialog: Boolean,
    onShowAddMcpServerDialog: (Boolean) -> Unit,
    onAddPopularMcpServer: (PopularMcpServer) -> Unit,
    skills: ImmutableList<SkillManifest>,
    onUninstallSkill: (String) -> Unit,
    showAddSkillDialog: Boolean,
    onShowAddSkillDialog: (Boolean) -> Unit,
    onInstallGitHubSkill: (String) -> Unit,
    onInstallBrowsedSkill: (RegistrySkillEntry) -> Unit,
    onBrowseSkills: () -> Unit,
    isInstallingSkill: Boolean,
    skillInstallError: String?,
    browsableSkills: ImmutableList<RegistrySkillEntry>,
    isBrowsingSkills: Boolean,
    browseSkillsFailed: Boolean,
    showSkills: Boolean,
    isSandboxInstalled: Boolean,
    onNavigateToSandbox: () -> Unit,
) {
    var section by remember { mutableStateOf(ConnectorLibrarySection.Connectors) }
    val sections = remember(showSkills) {
        if (showSkills) {
            ConnectorLibrarySection.entries
        } else {
            ConnectorLibrarySection.entries.filter { it != ConnectorLibrarySection.Skills }
        }
    }
    LaunchedEffect(sections) {
        if (section !in sections) section = sections.first()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ConnectorLibraryTabs(
            selected = section,
            onSelect = { section = it },
            sections = sections,
        )

        Spacer(Modifier.height(18.dp))

        when (section) {
            ConnectorLibrarySection.Connectors -> McpServersSection(
                mcpServers = mcpServers,
                onAddMcpServer = onAddMcpServer,
                onRemoveMcpServer = onRemoveMcpServer,
                onToggleMcpServer = onToggleMcpServer,
                onRefreshMcpServer = onRefreshMcpServer,
                onToggleTool = onToggleTool,
                showAddDialog = showAddMcpServerDialog,
                onShowAddDialog = onShowAddMcpServerDialog,
                onAddPopularMcpServer = onAddPopularMcpServer,
            )

            ConnectorLibrarySection.Skills -> SkillsSection(
                skills = skills,
                onUninstallSkill = onUninstallSkill,
                showAddDialog = showAddSkillDialog,
                onShowAddDialog = onShowAddSkillDialog,
                onInstallGitHub = onInstallGitHubSkill,
                onInstallBrowsed = onInstallBrowsedSkill,
                isInstalling = isInstallingSkill,
                installError = skillInstallError,
                browsableSkills = browsableSkills,
                isBrowsing = isBrowsingSkills,
                browseFailed = browseSkillsFailed,
                isSandboxInstalled = isSandboxInstalled,
                onNavigateToSandbox = onNavigateToSandbox,
                onBrowseSkills = onBrowseSkills,
            )

            ConnectorLibrarySection.Plugins -> PluginsSection(
                tools = tools,
                onToggleTool = onToggleTool,
            )
        }
    }
}

/**
 * The Tools tab: everything that widens what the agent can do — native tools,
 * MCP connectors and skills. The state and action plumbing lives here so the tab
 * itself stays a one-liner in the settings detail pane.
 */
@Composable
internal fun ToolsTabContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
    sandboxState: SandboxUiState,
) {
    ToolsContent(
        tools = uiState.tools,
        onToggleTool = actions.onToggleTool,
        mcpServers = uiState.mcpServers,
        onAddMcpServer = actions.onAddMcpServer,
        onRemoveMcpServer = actions.onRemoveMcpServer,
        onToggleMcpServer = actions.onToggleMcpServer,
        onRefreshMcpServer = actions.onRefreshMcpServer,
        showAddMcpServerDialog = uiState.showAddMcpServerDialog,
        onShowAddMcpServerDialog = actions.onShowAddMcpServerDialog,
        onAddPopularMcpServer = actions.onAddPopularMcpServer,
        skills = uiState.skills,
        onUninstallSkill = actions.onUninstallSkill,
        showAddSkillDialog = uiState.showAddSkillDialog,
        onShowAddSkillDialog = actions.onShowAddSkillDialog,
        onInstallGitHubSkill = actions.onInstallGitHubSkill,
        onInstallBrowsedSkill = actions.onInstallBrowsedSkill,
        onBrowseSkills = actions.onBrowseSkills,
        isInstallingSkill = uiState.isInstallingSkill,
        skillInstallError = uiState.skillInstallError,
        browsableSkills = uiState.browsableSkills,
        isBrowsingSkills = uiState.isBrowsingSkills,
        browseSkillsFailed = uiState.browseSkillsFailed,
        // Skills run inside the sandbox, so they only exist where the sandbox does.
        showSkills = sandboxState.showSandbox,
        isSandboxInstalled = sandboxState.sandboxInstalled,
        onNavigateToSandbox = { actions.onSelectTab(SettingsTab.Sandbox) },
    )
}

@Composable
internal fun PluginsSection(
    tools: ImmutableList<ToolInfo>,
    onToggleTool: (String, Boolean) -> Unit,
) {
    var toolFilter by remember { mutableStateOf("") }
    val resolvedTools = tools.map { tool ->
        Triple(
            tool,
            tool.nameRes?.let { stringResource(it) } ?: tool.name,
            tool.descriptionRes?.let { stringResource(it) } ?: tool.description,
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.settings_plugins),
            style = DoraiTypography.titleLarge,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.settings_plugins_description),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )

        Spacer(Modifier.height(16.dp))

        if (tools.isEmpty()) {
            CollectionEmptyState(
                message = stringResource(Res.string.settings_tools_none_available),
            )
        } else {
            // Filtering only matters once the plugin list is large enough to scan.
            if (tools.size >= 8) {
                DoraiSearchField(
                    query = toolFilter,
                    onQueryChange = { toolFilter = it },
                    placeholder = stringResource(Res.string.settings_plugins_search),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            val visibleTools = if (toolFilter.isBlank()) {
                resolvedTools
            } else {
                resolvedTools.filter { (_, name, description) ->
                    name.contains(toolFilter, ignoreCase = true) ||
                        description.contains(toolFilter, ignoreCase = true)
                }
            }
            if (visibleTools.isEmpty()) {
                CollectionEmptyState(
                    message = stringResource(Res.string.settings_plugins_no_match),
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = when {
                        maxWidth >= 800.dp -> 3
                        maxWidth >= 500.dp -> 2
                        else -> 1
                    }
                    val rows = visibleTools.chunked(columns)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowTools ->
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowTools.forEach { (tool, _, _) ->
                                    ToolItem(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        tool = tool,
                                        onToggle = { enabled -> onToggleTool(tool.id, enabled) },
                                    )
                                }
                                repeat(columns - rowTools.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Integrations used to be a separate Settings tab; the request entry point
        // now lives at the bottom of the merged Tools page.
        IntegrationRequestCard()
    }
}

@Composable
private fun ToolItem(
    tool: ToolInfo,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBg = SurfaceRaised.copy(alpha = 0.5f)
    val cardBorder = BorderStroke(1.dp, BorderSoft)
    val cardShape = SettingsShapes.card

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { onToggle(!tool.isEnabled) }
            .handCursor(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        shape = cardShape,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.nameRes?.let { stringResource(it) } ?: tool.name,
                    style = DoraiTypography.titleSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Text(
                    text = tool.descriptionRes?.let { stringResource(it) } ?: tool.description,
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }

            Spacer(Modifier.width(16.dp))

            Switch(
                checked = tool.isEnabled,
                onCheckedChange = null,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = com.dorai.ai.ui.AccentPrimary,
                    checkedTrackColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.3f),
                    uncheckedThumbColor = LocalDesignColorTokens.current.textSecondary,
                    uncheckedTrackColor = BorderSoft.copy(alpha = 0.5f),
                ),
            )
        }
    }
}
