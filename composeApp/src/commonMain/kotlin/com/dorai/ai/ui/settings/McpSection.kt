package com.dorai.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.mcp.PopularMcpServer
import com.dorai.ai.mcp.authorizationHeaderValue
import com.dorai.ai.mcp.matchesPopularMcpUrl
import com.dorai.ai.mcp.popularMcpServers
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiOutlinedTextField
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.components.StatusBadge
import com.dorai.ai.ui.components.VerticalScrollbarForScroll
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.ic_arrow_drop_down
import dorai.composeapp.generated.resources.settings_connectors
import dorai.composeapp.generated.resources.settings_connectors_add
import dorai.composeapp.generated.resources.settings_connectors_description
import dorai.composeapp.generated.resources.settings_connectors_empty
import dorai.composeapp.generated.resources.settings_connectors_no_match
import dorai.composeapp.generated.resources.settings_connectors_search
import dorai.composeapp.generated.resources.settings_connectors_search_discover
import dorai.composeapp.generated.resources.settings_connectors_setup_required
import dorai.composeapp.generated.resources.settings_connectors_top
import dorai.composeapp.generated.resources.settings_library_publisher
import dorai.composeapp.generated.resources.settings_mcp_add
import dorai.composeapp.generated.resources.settings_mcp_add_header
import dorai.composeapp.generated.resources.settings_mcp_add_server
import dorai.composeapp.generated.resources.settings_mcp_api_key
import dorai.composeapp.generated.resources.settings_mcp_api_key_help
import dorai.composeapp.generated.resources.settings_mcp_authorize
import dorai.composeapp.generated.resources.settings_mcp_authorized
import dorai.composeapp.generated.resources.settings_mcp_header_key
import dorai.composeapp.generated.resources.settings_mcp_header_value
import dorai.composeapp.generated.resources.settings_mcp_no_tools
import dorai.composeapp.generated.resources.settings_mcp_popular_servers
import dorai.composeapp.generated.resources.settings_mcp_refresh
import dorai.composeapp.generated.resources.settings_mcp_remove
import dorai.composeapp.generated.resources.settings_mcp_revoke
import dorai.composeapp.generated.resources.settings_mcp_server_name
import dorai.composeapp.generated.resources.settings_mcp_server_url
import dorai.composeapp.generated.resources.settings_mcp_status_connected
import dorai.composeapp.generated.resources.settings_mcp_status_connecting
import dorai.composeapp.generated.resources.settings_mcp_status_error
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun McpServersSection(
    mcpServers: ImmutableList<McpServerUiState>,
    onAddMcpServer: (String, String, Map<String, String>) -> Unit,
    onRemoveMcpServer: (String) -> Unit,
    onToggleMcpServer: (String, Boolean) -> Unit,
    onRefreshMcpServer: (String) -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    showAddDialog: Boolean,
    onShowAddDialog: (Boolean) -> Unit,
    onAddPopularMcpServer: (PopularMcpServer) -> Unit,
    onAuthorizeMcpServer: (String) -> Unit,
    onRevokeMcpServerOAuth: (String) -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<McpServerUiState?>(null) }
    var scope by remember { mutableStateOf(ConnectorLibraryScope.Yours) }
    var query by remember { mutableStateOf("") }
    var onlyNoSetup by remember { mutableStateOf(false) }
    var prefillPopular by remember { mutableStateOf<PopularMcpServer?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.settings_connectors),
            style = DoraiTypography.titleLarge,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.settings_connectors_description),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectorScopeToggle(
                selected = scope,
                onSelect = {
                    scope = it
                    query = ""
                },
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (scope == ConnectorLibraryScope.Yours) {
                    mcpServers.size.toString()
                } else {
                    popularMcpServers.size.toString()
                },
                style = DoraiTypography.labelMedium,
                color = LocalDesignColorTokens.current.textTertiary,
            )
        }

        Spacer(Modifier.height(12.dp))

        ConnectorSearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = if (scope == ConnectorLibraryScope.Yours) {
                stringResource(Res.string.settings_connectors_search)
            } else {
                stringResource(Res.string.settings_connectors_search_discover)
            },
            addContentDescription = stringResource(Res.string.settings_connectors_add),
            onAdd = {
                prefillPopular = null
                onShowAddDialog(true)
            },
            showFilter = scope == ConnectorLibraryScope.Discover,
            isFilterActive = onlyNoSetup,
            onToggleFilter = { onlyNoSetup = !onlyNoSetup },
        )

        Spacer(Modifier.height(16.dp))

        when (scope) {
            ConnectorLibraryScope.Yours -> {
                val visibleServers = if (query.isBlank()) {
                    mcpServers
                } else {
                    mcpServers.filter { server ->
                        server.name.contains(query, ignoreCase = true) ||
                            server.url.contains(query, ignoreCase = true)
                    }
                }
                if (mcpServers.isEmpty()) {
                    CollectionEmptyState(
                        message = stringResource(Res.string.settings_connectors_empty),
                    )
                } else if (visibleServers.isEmpty()) {
                    CollectionEmptyState(
                        message = stringResource(Res.string.settings_connectors_no_match),
                    )
                } else {
                    for (server in visibleServers) {
                        McpServerCard(
                            server = server,
                            onToggle = { enabled -> onToggleMcpServer(server.id, enabled) },
                            onRemove = { pendingRemoval = server },
                            onRefresh = { onRefreshMcpServer(server.id) },
                            onToggleTool = onToggleTool,
                            onAuthorize = { onAuthorizeMcpServer(server.id) },
                            onRevoke = { onRevokeMcpServerOAuth(server.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            ConnectorLibraryScope.Discover -> {
                val visibleServers = remember(query, onlyNoSetup) {
                    popularMcpServers.filter { server ->
                        (!onlyNoSetup || !server.requiresAuth) &&
                            (
                                query.isBlank() ||
                                    server.name.contains(query, ignoreCase = true) ||
                                    server.description.contains(query, ignoreCase = true)
                                )
                    }
                }
                if (visibleServers.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    CollectionEmptyState(
                        message = stringResource(Res.string.settings_connectors_no_match),
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.settings_connectors_top),
                        style = DoraiTypography.titleSmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    visibleServers.forEachIndexed { index, server ->
                        val isAdded = mcpServers.any { saved ->
                            matchesPopularMcpUrl(saved.url, server.url)
                        }
                        val publisher = connectorPublisher(server)
                        val meta = buildString {
                            append(stringResource(Res.string.settings_library_publisher, publisher))
                            if (server.requiresAuth) {
                                append(" · ")
                                append(stringResource(Res.string.settings_connectors_setup_required))
                            }
                        }
                        ConnectorDiscoverRow(
                            name = server.name,
                            description = server.description,
                            meta = meta,
                            isAdded = isAdded,
                            addContentDescription = stringResource(
                                Res.string.settings_connectors_add,
                            ),
                            onAdd = {
                                if (server.requiresAuth) {
                                    prefillPopular = server
                                    onShowAddDialog(true)
                                } else {
                                    onAddPopularMcpServer(server)
                                }
                            },
                        )
                        if (index != visibleServers.lastIndex) {
                            HorizontalDivider(
                                color = LocalDesignColorTokens.current.borderSubtle,
                            )
                        }
                    }
                }
            }
        }
    }

    pendingRemoval?.let { target ->
        SettingsConfirmDialog(
            title = "Remove ${target.name}?",
            message = "This MCP server and its tools will be removed from your workspace. You can re-add it later.",
            confirmLabel = "Remove",
            onConfirm = {
                onRemoveMcpServer(target.id)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    if (showAddDialog) {
        AddMcpServerDialog(
            onDismiss = {
                onShowAddDialog(false)
                prefillPopular = null
            },
            onAdd = onAddMcpServer,
            onAddPopular = onAddPopularMcpServer,
            initialPopular = prefillPopular,
        )
    }
}

private fun connectorPublisher(server: PopularMcpServer): String {
    val host = server.url
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
    return when (host) {
        "mcp.context7.com" -> "Context7"
        "mcp.mdn.mozilla.net" -> "Mozilla"
        "mcp.deepwiki.com" -> "DeepWiki"
        "search.parallel.ai" -> "Parallel"
        "gateway.mcpservers.org" -> "MCP Servers"
        "mcp.api.coingecko.com" -> "CoinGecko"
        "mcp.jina.ai" -> "Jina AI"
        "mcp.open-mcp.org" -> "Open MCP"
        "mcp.kiwi.com" -> "Kiwi.com"
        "scamguard.malwarebytes.com" -> "Malwarebytes"
        "tldraw-mcp-app.tldraw.workers.dev" -> "tldraw"
        "api.findadomain.dev" -> "Find-A-Domain"
        "subwayinfo.nyc" -> "SubwayInfo"
        "api.githubcopilot.com" -> "GitHub"
        "mcp.notion.com" -> "Notion"
        "mcp.figma.com" -> "Figma"
        "mcp.cloudflare.com" -> "Cloudflare"
        "mcp.supabase.com" -> "Supabase"
        "mcp.tavily.com" -> "Tavily"
        "mcp.firecrawl.dev" -> "Firecrawl"
        else -> host.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun McpServerCard(
    server: McpServerUiState,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onRefresh: () -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    onAuthorize: () -> Unit,
    onRevoke: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .handCursor()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = doraiAdaptiveCardColors(),
        border = doraiAdaptiveCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Status badge: text + color, standardized across tools/skills/sandbox
                // (Plan 4, Phase 1). Disabled is its own state, not an error.
                val status = when {
                    !server.isEnabled -> OperationalStatus.Disabled
                    server.connectionStatus == McpConnectionStatus.Connected -> OperationalStatus.Connected
                    server.connectionStatus == McpConnectionStatus.Connecting -> OperationalStatus.Connecting
                    server.connectionStatus == McpConnectionStatus.Error -> OperationalStatus.Error
                    else -> OperationalStatus.Ready
                }
                val statusLabel = when {
                    !server.isEnabled -> "Disabled"

                    server.connectionStatus == McpConnectionStatus.Connected ->
                        stringResource(Res.string.settings_mcp_status_connected)

                    server.connectionStatus == McpConnectionStatus.Connecting ->
                        stringResource(Res.string.settings_mcp_status_connecting)

                    server.connectionStatus == McpConnectionStatus.Error ->
                        stringResource(Res.string.settings_mcp_status_error)

                    else -> "Not checked"
                }
                StatusBadge(label = statusLabel, status = status)
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                    Text(
                        text = server.url,
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Switch(
                    checked = server.isEnabled,
                    onCheckedChange = onToggle,
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))

                // OAuth status / actions
                if (server.oauthAuthorized) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StatusBadge(
                            label = stringResource(Res.string.settings_mcp_authorized),
                            status = OperationalStatus.Connected,
                        )
                        TextButton(onClick = onRevoke) {
                            Text(
                                text = stringResource(Res.string.settings_mcp_revoke),
                                color = DangerAlert,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                } else {
                    Text(
                        text = stringResource(Res.string.settings_connectors_setup_required),
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onAuthorize) {
                        Text(text = stringResource(Res.string.settings_mcp_authorize))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Status text
                val statusText = when (server.connectionStatus) {
                    McpConnectionStatus.Connected -> stringResource(Res.string.settings_mcp_status_connected)
                    McpConnectionStatus.Connecting -> stringResource(Res.string.settings_mcp_status_connecting)
                    McpConnectionStatus.Error -> stringResource(Res.string.settings_mcp_status_error)
                    McpConnectionStatus.Unknown -> ""
                }
                if (statusText.isNotEmpty()) {
                    Text(
                        text = statusText,
                        style = DoraiTypography.bodySmall,
                        color = when (server.connectionStatus) {
                            McpConnectionStatus.Error -> DangerAlert
                            else -> LocalDesignColorTokens.current.textSecondary
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Tools list
                if (server.tools.isNotEmpty()) {
                    for (tool in server.tools) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tool.name,
                                    style = DoraiTypography.bodyMedium,
                                    color = LocalDesignColorTokens.current.textPrimary,
                                )
                                if (tool.description.isNotEmpty()) {
                                    Text(
                                        text = tool.description,
                                        style = DoraiTypography.bodySmall,
                                        color = LocalDesignColorTokens.current.textSecondary,
                                    )
                                }
                            }
                            Switch(
                                checked = tool.isEnabled,
                                onCheckedChange = { enabled -> onToggleTool(tool.id, enabled) },
                            )
                        }
                    }
                } else if (server.connectionStatus == McpConnectionStatus.Connected) {
                    Text(
                        text = stringResource(Res.string.settings_mcp_no_tools),
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRefresh, modifier = Modifier.handCursor()) {
                        Text(stringResource(Res.string.settings_mcp_refresh))
                    }
                    TextButton(onClick = onRemove, modifier = Modifier.handCursor()) {
                        Text(
                            text = stringResource(Res.string.settings_mcp_remove),
                            color = DangerAlert,
                        )
                    }
                }
            }
        }
    }
}

private data class HeaderEntry(val key: String = "Authorization", val value: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Map<String, String>) -> Unit,
    onAddPopular: (PopularMcpServer) -> Unit,
    initialPopular: PopularMcpServer? = null,
) {
    var name by remember { mutableStateOf(initialPopular?.name.orEmpty()) }
    var url by remember { mutableStateOf(initialPopular?.url.orEmpty()) }
    val headers = remember {
        mutableStateListOf<HeaderEntry>().apply {
            val defaults = initialPopular?.headers.orEmpty()
            if (defaults.isEmpty()) {
                add(HeaderEntry())
            } else {
                defaults.forEach { (key, value) -> add(HeaderEntry(key = key, value = value)) }
            }
        }
    }
    // When a popular server needs auth (e.g. Jina), prefill the form and show an API key field.
    var requiresAuth by remember { mutableStateOf(initialPopular?.requiresAuth == true) }
    var apiKey by remember { mutableStateOf("") }
    val mcpScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    fun prefillPopularWithAuth(server: PopularMcpServer) {
        name = server.name
        url = server.url
        requiresAuth = true
        apiKey = ""
        headers.clear()
        headers.add(HeaderEntry(key = "Authorization", value = ""))
        scope.launch { mcpScrollState.animateScrollTo(0) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .verticalScroll(mcpScrollState)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_mcp_add_server),
                    style = DoraiTypography.headlineSmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Spacer(Modifier.height(16.dp))

                DoraiOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.settings_mcp_server_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                DoraiOutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(Res.string.settings_mcp_server_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                if (requiresAuth) {
                    DoraiOutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text(stringResource(Res.string.settings_mcp_api_key)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.settings_mcp_api_key_help),
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                } else {
                    headers.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // The floating labels must stay on one line: at large font
                            // scales a wrapped label grows upward out of the field and
                            // lands on top of the URL field above it.
                            DoraiOutlinedTextField(
                                value = entry.key,
                                onValueChange = { headers[index] = entry.copy(key = it) },
                                label = {
                                    Text(
                                        text = stringResource(Res.string.settings_mcp_header_key),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(0.5f),
                            )
                            Spacer(Modifier.width(8.dp))
                            DoraiOutlinedTextField(
                                value = entry.value,
                                onValueChange = { headers[index] = entry.copy(value = it) },
                                label = {
                                    Text(
                                        text = stringResource(Res.string.settings_mcp_header_value),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(0.5f),
                            )
                            IconButton(
                                onClick = { headers.removeAt(index) },
                                modifier = Modifier.handCursor(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(Res.string.settings_mcp_remove),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    TextButton(
                        onClick = { headers.add(HeaderEntry(key = "", value = "")) },
                        modifier = Modifier.handCursor(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.settings_mcp_add_header))
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            val headerMap = buildMap {
                                headers
                                    .filter { it.key.isNotBlank() && it.value.isNotBlank() }
                                    .forEach { put(it.key.trim(), it.value.trim()) }
                                if (requiresAuth && apiKey.isNotBlank()) {
                                    // Replace any Authorization row with the dedicated API key field.
                                    keys.filter { it.equals("Authorization", ignoreCase = true) }
                                        .toList()
                                        .forEach { remove(it) }
                                    put("Authorization", authorizationHeaderValue(apiKey))
                                }
                            }
                            onAdd(name, url, headerMap)
                        },
                        // Auth-required popular servers (e.g. Jina) can still be added without a
                        // key — some tools work unauthenticated; search needs a key later.
                        enabled = name.isNotBlank() && url.isNotBlank(),
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(stringResource(Res.string.settings_mcp_add))
                    }
                }

                if (popularMcpServers.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.settings_mcp_popular_servers),
                        style = DoraiTypography.titleSmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    for (server in popularMcpServers) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CardDefaults.shape)
                                .clickable {
                                    if (server.requiresAuth) {
                                        prefillPopularWithAuth(server)
                                    } else {
                                        onAddPopular(server)
                                    }
                                }
                                .handCursor(),
                            colors = CardDefaults.cardColors(
                                containerColor = CardFill,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = server.name,
                                    style = DoraiTypography.bodyMedium,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                )
                                Text(
                                    text = server.description,
                                    style = DoraiTypography.bodySmall,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
            VerticalScrollbarForScroll(
                scrollState = mcpScrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}
