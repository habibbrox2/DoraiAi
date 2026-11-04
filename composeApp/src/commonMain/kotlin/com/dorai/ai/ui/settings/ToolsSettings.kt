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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.dorai.ai.ui.KaiOutlinedTextField
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.components.CapabilityCountCard
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.kaiAdaptiveCardBorder
import com.dorai.ai.ui.kaiAdaptiveCardColors
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_tools_description
import dorai.composeapp.generated.resources.settings_tools_none_available
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

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
    isInstallingSkill: Boolean,
    skillInstallError: String?,
    browsableSkills: ImmutableList<RegistrySkillEntry>,
    isBrowsingSkills: Boolean,
    browseSkillsFailed: Boolean,
    showSkills: Boolean,
    isSandboxInstalled: Boolean,
    onNavigateToSandbox: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Tools Overview (Plan 4, Phase 2): one glance at what the workspace can
        // do — enabled tools, connected servers, installed skills — before any
        // management surface.
        ToolsOverviewHeader(
            tools = tools,
            mcpServers = mcpServers,
            skills = skills,
            showSkills = showSkills,
            isSandboxInstalled = isSandboxInstalled,
        )

        Spacer(Modifier.height(16.dp))

        // MCP Servers section
        McpServersSection(
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

        // Skills section — sandbox-backed, so Android only.
        if (showSkills) {
            Spacer(Modifier.height(24.dp))
            SkillsSection(
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
            )
        }

        Spacer(Modifier.height(24.dp))

        // Native tools section
        Text(
            text = stringResource(Res.string.settings_tools_description),
            style = KaiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )

        Spacer(Modifier.height(16.dp))

        if (tools.isEmpty()) {
            CollectionEmptyState(
                message = stringResource(Res.string.settings_tools_none_available),
            )
        } else {
            // Search/filter once the collection is large (Plan 4, Phase 2, item 5);
            // display strings are resolved first so localized names match too.
            var toolFilter by remember { mutableStateOf("") }
            val resolvedTools = tools.map { tool ->
                Triple(
                    tool,
                    tool.nameRes?.let { stringResource(it) } ?: tool.name,
                    tool.descriptionRes?.let { stringResource(it) } ?: tool.description,
                )
            }
            if (tools.size >= 8) {
                KaiOutlinedTextField(
                    value = toolFilter,
                    onValueChange = { toolFilter = it },
                    label = { Text("Search tools") },
                    singleLine = true,
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
                    message = "No tools match \"$toolFilter\".",
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
                                // Fill empty slots so last row items don't stretch
                                repeat(columns - rowTools.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Tools tab: everything that widens what the agent can do — native tools, MCP servers and
 * skills. The state and action plumbing lives here so the tab itself stays a one-liner in the
 * settings detail pane.
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

/**
 * Capability summary for the Tools page (Plan 4, Phase 2): count cards for
 * enabled native tools, connected MCP servers, and installed skills, each with
 * a status badge when something needs attention.
 */
@Composable
private fun ToolsOverviewHeader(
    tools: ImmutableList<ToolInfo>,
    mcpServers: ImmutableList<McpServerUiState>,
    skills: ImmutableList<SkillManifest>,
    showSkills: Boolean,
    isSandboxInstalled: Boolean,
) {
    val enabledTools = tools.count { it.isEnabled }
    val connectedServers = mcpServers.count {
        it.isEnabled && it.connectionStatus == McpConnectionStatus.Connected
    }
    val mcpStatus = when {
        mcpServers.any { it.connectionStatus == McpConnectionStatus.Error } -> OperationalStatus.Error
        connectedServers > 0 -> OperationalStatus.Connected
        mcpServers.isNotEmpty() -> OperationalStatus.Disabled
        else -> null
    }
    val cards: List<@Composable () -> Unit> = buildList {
        add {
            CapabilityCountCard(
                label = "Native tools enabled",
                count = "$enabledTools / ${tools.size}",
                status = when {
                    tools.isNotEmpty() && enabledTools == 0 -> OperationalStatus.Disabled
                    tools.isNotEmpty() && enabledTools == tools.size -> OperationalStatus.Active
                    else -> null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        add {
            CapabilityCountCard(
                label = "MCP servers connected",
                count = "$connectedServers / ${mcpServers.size}",
                status = mcpStatus,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showSkills) {
            add {
                CapabilityCountCard(
                    label = "Skills installed",
                    count = skills.size.toString(),
                    status = when {
                        !isSandboxInstalled -> OperationalStatus.NeedsSetup
                        skills.isNotEmpty() -> OperationalStatus.Ready
                        else -> null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Same breakpoints idea as the native grid: three across when there is
        // room, otherwise stacked so counts never shrink into unreadable tiles.
        val columns = when {
            cards.size == 3 && maxWidth >= 720.dp -> 3
            maxWidth >= 480.dp && cards.size > 1 -> 2
            else -> 1
        }
        if (columns == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.forEach { card -> card() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(columns).forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowCards.forEach { card ->
                            Box(modifier = Modifier.weight(1f)) { card() }
                        }
                        repeat(columns - rowCards.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
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
    val cardShape = com.dorai.ai.ui.settings.SettingsShapes.card

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
                    style = KaiTypography.titleSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Text(
                    text = tool.descriptionRes?.let { stringResource(it) } ?: tool.description,
                    style = KaiTypography.bodySmall,
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
