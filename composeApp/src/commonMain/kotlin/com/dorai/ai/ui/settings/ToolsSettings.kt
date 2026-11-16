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
import androidx.compose.ui.semantics.Role
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
        SettingsSectionHeader(
            title = stringResource(Res.string.settings_plugins),
            count = tools.size,
            description = stringResource(Res.string.settings_plugins_description),
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
            .clickable(role = Role.Button) { onToggle(!tool.isEnabled) }
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
