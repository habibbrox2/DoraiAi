package com.dorai.ai.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.Version
import com.dorai.ai.data.AppSettings
import com.dorai.ai.linux.LinuxDistro
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiListDetailPaneScaffold
import com.dorai.ai.ui.adaptive.DoraiWindowWidthClass
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.components.LogoAnimation
import com.dorai.ai.ui.components.v2.DoraiModalSheet
import com.dorai.ai.ui.components.v2.DoraiPillTabs
import com.dorai.ai.ui.components.v2.DoraiProviderBadge
import com.dorai.ai.ui.components.v2.DoraiStatusBadge
import com.dorai.ai.ui.components.v2.DoraiThemePresetTile
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_approval_ask_all
import dorai.composeapp.generated.resources.settings_approval_ask_edits
import dorai.composeapp.generated.resources.settings_approval_auto_read
import dorai.composeapp.generated.resources.settings_approval_policy
import dorai.composeapp.generated.resources.settings_cat_account
import dorai.composeapp.generated.resources.settings_cat_agent
import dorai.composeapp.generated.resources.settings_cat_appearance
import dorai.composeapp.generated.resources.settings_cat_general
import dorai.composeapp.generated.resources.settings_cat_mcp
import dorai.composeapp.generated.resources.settings_cat_providers
import dorai.composeapp.generated.resources.settings_cat_sandbox
import dorai.composeapp.generated.resources.settings_cat_tools
import dorai.composeapp.generated.resources.settings_delete_all_chats
import dorai.composeapp.generated.resources.settings_delete_chats_confirm
import dorai.composeapp.generated.resources.settings_mcp_add_server
import dorai.composeapp.generated.resources.settings_mcp_invalid_url
import dorai.composeapp.generated.resources.settings_mcp_test_connection
import dorai.composeapp.generated.resources.settings_mcp_url_hint
import dorai.composeapp.generated.resources.settings_timeline_desc
import dorai.composeapp.generated.resources.settings_timeline_toggle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

enum class SettingsCategory(
    val id: String,
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    General("general", Res.string.settings_cat_general, Icons.Default.Tune),
    Appearance("appearance", Res.string.settings_cat_appearance, Icons.Default.Palette),
    Agent("agent", Res.string.settings_cat_agent, Icons.Default.SmartToy),
    Providers("providers", Res.string.settings_cat_providers, Icons.Default.VpnKey),
    Tools("tools", Res.string.settings_cat_tools, Icons.Default.Build),
    Mcp("mcp", Res.string.settings_cat_mcp, Icons.Default.Extension),
    Sandbox("sandbox", Res.string.settings_cat_sandbox, Icons.Default.Terminal),
    Account("account", Res.string.settings_cat_account, Icons.Default.AccountCircle),
}

/**
 * UI v2 Settings screen:
 * - 8 categories, strictly ordered.
 * - Compact: scrollable pill tab bar at top + content below.
 * - Medium+: category list in the list pane + content in detail pane.
 */
@Composable
fun DoraiSettingsScreen(
    activeCategory: SettingsCategory = SettingsCategory.General,
    onCategoryChanged: (SettingsCategory) -> Unit,
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    currentThemeStyle: ThemeStyle = ThemeStyle.Flat,
    onThemeStyleSelected: ((ThemeStyle) -> Unit)? = null,
    modifier: Modifier = Modifier,
    uiState: SettingsUiState? = null,
    actions: SettingsActions? = null,
    sandboxState: SandboxUiState? = null,
    onToggleSandbox: ((Boolean) -> Unit)? = null,
    onSelectDistro: ((LinuxDistro) -> Unit)? = null,
    onSetupSandbox: (() -> Unit)? = null,
    onCancelSandbox: (() -> Unit)? = null,
    onResetSandbox: (() -> Unit)? = null,
    onInstallPackages: (() -> Unit)? = null,
    onMigrateHome: (() -> Unit)? = null,
    androidUpdateContent: (@Composable () -> Unit)? = null,
    debugBuildStamp: String? = null,
) {
    val widthClass = LocalDoraiWindowWidthClass.current
    val colors = LocalDoraiColors.current
    var selectedCat by remember(activeCategory) { mutableStateOf(activeCategory) }

    if (widthClass.isCompact) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            DoraiPillTabs(
                items = SettingsCategory.entries,
                selectedItem = selectedCat,
                onItemSelected = {
                    selectedCat = it
                    onCategoryChanged(it)
                },
                labelProvider = { stringResource(it.titleRes) },
                modifier = Modifier.padding(DoraiSpacing.MD),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                SettingsCategoryContent(
                    category = selectedCat,
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected,
                    currentThemeStyle = currentThemeStyle,
                    onThemeStyleSelected = onThemeStyleSelected,
                    uiState = uiState,
                    actions = actions,
                    sandboxState = sandboxState,
                    onToggleSandbox = onToggleSandbox,
                    onSelectDistro = onSelectDistro,
                    onSetupSandbox = onSetupSandbox,
                    onCancelSandbox = onCancelSandbox,
                    onResetSandbox = onResetSandbox,
                    onInstallPackages = onInstallPackages,
                    onMigrateHome = onMigrateHome,
                    androidUpdateContent = androidUpdateContent,
                    debugBuildStamp = debugBuildStamp,
                    onNavigateToCategory = {
                        selectedCat = it
                        onCategoryChanged(it)
                    },
                )
            }
        }
    } else {
        DoraiListDetailPaneScaffold(
            listPane = {
                SettingsCategoryListPane(
                    selectedCategory = selectedCat,
                    onCategorySelected = {
                        selectedCat = it
                        onCategoryChanged(it)
                    },
                )
            },
            detailPane = {
                SettingsCategoryContent(
                    category = selectedCat,
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected,
                    currentThemeStyle = currentThemeStyle,
                    onThemeStyleSelected = onThemeStyleSelected,
                    uiState = uiState,
                    actions = actions,
                    sandboxState = sandboxState,
                    onToggleSandbox = onToggleSandbox,
                    onSelectDistro = onSelectDistro,
                    onSetupSandbox = onSetupSandbox,
                    onCancelSandbox = onCancelSandbox,
                    onResetSandbox = onResetSandbox,
                    onInstallPackages = onInstallPackages,
                    onMigrateHome = onMigrateHome,
                    androidUpdateContent = androidUpdateContent,
                    debugBuildStamp = debugBuildStamp,
                    onNavigateToCategory = {
                        selectedCat = it
                        onCategoryChanged(it)
                    },
                )
            },
            modifier = modifier,
        )
    }
}

/**
 * Production-connected wrapper for UI v2 Settings, binding [SettingsViewModel] and [SandboxViewModel].
 */
@Composable
fun DoraiSettingsScreenConnected(
    activeCategory: SettingsCategory = SettingsCategory.General,
    onCategoryChanged: (SettingsCategory) -> Unit,
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    currentThemeStyle: ThemeStyle = ThemeStyle.Flat,
    onThemeStyleSelected: ((ThemeStyle) -> Unit)? = null,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    sandboxViewModel: SandboxViewModel = koinViewModel(),
    androidUpdateContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by settingsViewModel.state.collectAsStateWithLifecycle()
    val sandboxState by sandboxViewModel.state.collectAsStateWithLifecycle()

    DoraiSettingsScreen(
        activeCategory = activeCategory,
        onCategoryChanged = onCategoryChanged,
        selectedThemeId = selectedThemeId,
        onThemeSelected = onThemeSelected,
        currentThemeStyle = currentThemeStyle,
        onThemeStyleSelected = onThemeStyleSelected,
        uiState = uiState,
        actions = settingsViewModel.actions,
        sandboxState = sandboxState,
        onToggleSandbox = sandboxViewModel::onToggleSandbox,
        onSelectDistro = sandboxViewModel::onSelectDistro,
        onSetupSandbox = sandboxViewModel::onSetupSandbox,
        onCancelSandbox = sandboxViewModel::onCancelSandbox,
        onResetSandbox = sandboxViewModel::onResetSandbox,
        onInstallPackages = sandboxViewModel::onInstallPackages,
        onMigrateHome = sandboxViewModel::onMigrateHome,
        androidUpdateContent = androidUpdateContent,
        debugBuildStamp = "v${Version.appVersion} · uiV2=on",
        modifier = modifier,
    )
}

@Composable
private fun SettingsCategoryListPane(
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit,
) {
    val colors = LocalDoraiColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(DoraiSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Settings",
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = DoraiSpacing.SM),
        )

        SettingsCategory.entries.forEach { cat ->
            val isSelected = cat == selectedCategory
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (isSelected) colors.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onCategorySelected(cat) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.primary else colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(cat.titleRes),
                        color = if (isSelected) colors.primary else colors.text,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryContent(
    category: SettingsCategory,
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    currentThemeStyle: ThemeStyle = ThemeStyle.Flat,
    onThemeStyleSelected: ((ThemeStyle) -> Unit)? = null,
    uiState: SettingsUiState? = null,
    actions: SettingsActions? = null,
    sandboxState: SandboxUiState? = null,
    onToggleSandbox: ((Boolean) -> Unit)? = null,
    onSelectDistro: ((LinuxDistro) -> Unit)? = null,
    onSetupSandbox: (() -> Unit)? = null,
    onCancelSandbox: (() -> Unit)? = null,
    onResetSandbox: (() -> Unit)? = null,
    onInstallPackages: (() -> Unit)? = null,
    onMigrateHome: (() -> Unit)? = null,
    androidUpdateContent: (@Composable () -> Unit)? = null,
    debugBuildStamp: String? = null,
    onNavigateToCategory: (SettingsCategory) -> Unit = {},
) {
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoraiSpacing.MD)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
    ) {
        Text(
            text = stringResource(category.titleRes),
            color = colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        if (uiState != null && actions != null) {
            when (category) {
                SettingsCategory.General -> {
                    if (debugBuildStamp != null) {
                        SettingsSectionCard {
                            Text("Build & Environment", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(DoraiSpacing.XS))
                            Text(debugBuildStamp, color = colors.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    GeneralContent(uiState = uiState, actions = actions, androidUpdateContent = androidUpdateContent)
                    BottomInfo()
                }

                SettingsCategory.Appearance -> AppearanceSettingsContent(
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected,
                )

                SettingsCategory.Agent -> {
                    AgentPolicyHeader()
                    AgentContent(uiState = uiState, actions = actions)
                }

                SettingsCategory.Providers -> ServicesContent(uiState = uiState, actions = actions)

                SettingsCategory.Tools -> {
                    PluginsSection(tools = uiState.tools, onToggleTool = actions.onToggleTool)
                    if (uiState.skills.isNotEmpty() || sandboxState?.showSandbox == true) {
                        SkillsSection(
                            skills = uiState.skills,
                            onUninstallSkill = actions.onUninstallSkill,
                            showAddDialog = uiState.showAddSkillDialog,
                            onShowAddDialog = actions.onShowAddSkillDialog,
                            onInstallGitHub = actions.onInstallGitHubSkill,
                            onInstallBrowsed = actions.onInstallBrowsedSkill,
                            isInstalling = uiState.isInstallingSkill,
                            installError = uiState.skillInstallError,
                            browsableSkills = uiState.browsableSkills,
                            isBrowsing = uiState.isBrowsingSkills,
                            browseFailed = uiState.browseSkillsFailed,
                            isSandboxInstalled = sandboxState?.sandboxInstalled ?: false,
                            onNavigateToSandbox = { onNavigateToCategory(SettingsCategory.Sandbox) },
                            onBrowseSkills = actions.onBrowseSkills,
                        )
                    }
                }

                SettingsCategory.Mcp -> {
                    McpServersSection(
                        mcpServers = uiState.mcpServers,
                        onAddMcpServer = actions.onAddMcpServer,
                        onRemoveMcpServer = actions.onRemoveMcpServer,
                        onToggleMcpServer = actions.onToggleMcpServer,
                        onRefreshMcpServer = actions.onRefreshMcpServer,
                        onToggleTool = actions.onToggleTool,
                        showAddDialog = uiState.showAddMcpServerDialog,
                        onShowAddDialog = actions.onShowAddMcpServerDialog,
                        onAddPopularMcpServer = actions.onAddPopularMcpServer,
                    )
                }

                SettingsCategory.Sandbox -> {
                    if (sandboxState != null && sandboxState.showSandbox) {
                        SandboxSettingsCard(
                            sandboxState = sandboxState,
                            onToggleSandbox = onToggleSandbox ?: {},
                            onSelectDistro = onSelectDistro ?: {},
                            onSetupSandbox = onSetupSandbox ?: {},
                            onCancelSandbox = onCancelSandbox ?: {},
                            onResetSandbox = onResetSandbox ?: {},
                            onInstallPackages = onInstallPackages ?: {},
                            onMigrateHome = onMigrateHome ?: {},
                        )
                    } else {
                        SandboxUnavailableContent()
                    }
                }

                SettingsCategory.Account -> {
                    AccountProfileCard()
                    DataControlContent(uiState = uiState, actions = actions)
                    AccountPlanContent(uiState = uiState)
                }
            }
        } else {
            when (category) {
                SettingsCategory.General -> GeneralContent(uiState = SettingsUiState(), actions = SettingsActions.NoOp)

                SettingsCategory.Appearance -> AppearanceSettingsContent(
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected,
                )

                SettingsCategory.Agent -> {
                    AgentPolicyHeader()
                    AgentSettingsContent()
                }

                SettingsCategory.Providers -> ServicesContent(uiState = SettingsUiState(), actions = SettingsActions.NoOp)

                SettingsCategory.Tools -> ToolsSettingsContent()

                SettingsCategory.Mcp -> McpSettingsContent()

                SettingsCategory.Sandbox -> SandboxSettingsContent()

                SettingsCategory.Account -> {
                    AccountProfileCard()
                    AccountSettingsContent()
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsContent(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current

    SettingsSectionCard {
        Text("Theme Presets (13)", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        val cols = if (widthClass == DoraiWindowWidthClass.Compact) 3 else 4
        val presets = ThemePresets.all

        Column(verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM)) {
            presets.chunked(cols).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
                ) {
                    rowPresets.forEach { palette ->
                        Box(modifier = Modifier.weight(1f)) {
                            DoraiThemePresetTile(
                                palette = palette,
                                isSelected = palette.id == selectedThemeId,
                                onSelect = { onThemeSelected(palette.id) },
                            )
                        }
                    }
                    // Fill remaining slots in last row
                    repeat(cols - rowPresets.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentSettingsContent() {
    AgentPolicyHeader()
}

@Composable
private fun ToolsSettingsContent() {
    val colors = LocalDoraiColors.current
    var toolPolicies by remember {
        mutableStateOf(
            mutableMapOf(
                "read_file" to false,
                "search" to false,
                "edit_file" to true,
                "shell" to true,
                "web_search" to true,
            ),
        )
    }

    var skillStates by remember {
        mutableStateOf(
            mutableMapOf(
                "code_review" to true,
                "git_commit" to true,
                "compose_tips" to true,
            ),
        )
    }

    Text(
        text = "টুল / Tools",
        color = colors.muted,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
    SettingsSectionCard {
        val toolList = listOf(
            Triple("read_file", "ফাইল পড়া / Read files", "read_file"),
            Triple("search", "কোডে খোঁজা / Search code", "search"),
            Triple("edit_file", "ফাইল বদলানো / Edit files", "edit_file"),
            Triple("shell", "কমান্ড চালানো / Run commands", "shell"),
            Triple("web_search", "ওয়েবে খোঁজা / Search the web", "web_search"),
        )
        toolList.forEachIndexed { idx, (id, label, _) ->
            val isRisky = toolPolicies[id] == true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = toolPolicies.toMutableMap()
                        next[id] = !isRisky
                        toolPolicies = next
                    }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(id, color = colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(label, color = colors.muted, fontSize = 12.sp)
                }
                DoraiStatusBadge(
                    text = if (isRisky) "Asks first" else "Auto",
                    color = if (isRisky) colors.warn else colors.ok,
                )
            }
            if (idx < toolList.lastIndex) {
                HorizontalDivider(color = colors.line.copy(alpha = 0.4f), thickness = 1.dp)
            }
        }
    }

    Text(
        text = "স্কিল / Skills",
        color = colors.muted,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
    SettingsSectionCard {
        val skills = listOf(
            Triple("code_review", "Code review", "Analyze syntax and safety regressions"),
            Triple("git_commit", "Git commit messages", "Generate conventional commit descriptions"),
            Triple("compose_tips", "Compose UI tips", "State hoisting and recomposition audits"),
        )
        skills.forEachIndexed { idx, (id, title, desc) ->
            val isChecked = skillStates[id] == true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(desc, color = colors.muted, fontSize = 12.sp)
                }
                Switch(
                    checked = isChecked,
                    onCheckedChange = {
                        val next = skillStates.toMutableMap()
                        next[id] = it
                        skillStates = next
                    },
                )
            }
            if (idx < skills.lastIndex) {
                HorizontalDivider(color = colors.line.copy(alpha = 0.4f), thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun McpSettingsContent() {
    val colors = LocalDoraiColors.current
    var showAddDialog by remember { mutableStateOf(false) }

    // + Add server button
    Button(
        onClick = { showAddDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.primary),
        border = BorderStroke(1.dp, colors.line),
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(Res.string.settings_mcp_add_server), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }

    Spacer(modifier = Modifier.height(DoraiSpacing.SM))

    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Local SQLite Bridge", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text("https://localhost:8080/mcp · 7 tools connected", color = colors.muted, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DoraiStatusBadge("Connected", colors.ok)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {}) {
                    Text("Test", color = colors.primary, fontSize = 13.sp)
                }
            }
        }
    }

    if (showAddDialog) {
        AddMcpServerModalSheet(onDismiss = { showAddDialog = false })
    }
}

@Composable
private fun AddMcpServerModalSheet(onDismiss: () -> Unit) {
    val colors = LocalDoraiColors.current
    var urlInput by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf("oauth") } // "oauth" vs "header" vs "none"
    val isHttps = urlInput.startsWith("https://")
    val isPrivateBlocked = !urlInput.contains("192.168.") && !urlInput.contains("10.0.") && !urlInput.contains("172.16.")
    val isValid = isHttps && isPrivateBlocked && urlInput.length > 10

    DoraiModalSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DoraiSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
        ) {
            Text(stringResource(Res.string.settings_mcp_add_server), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // Auth Selector (Supports OAuth 2.0 system)
            Text("Authorization System", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("oauth" to "OAuth 2.0", "header" to "API Header", "none" to "No Auth").forEach { (type, label) ->
                    val isSel = authType == type
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSel) colors.primary else colors.surface2)
                            .border(1.dp, if (isSel) colors.primary else colors.line, CircleShape)
                            .clickable { authType = type }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(label, color = if (isSel) colors.onPrimary else colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                placeholder = { Text("https://mcp-server.example.com", color = colors.muted) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.info,
                    unfocusedBorderColor = colors.line,
                ),
            )

            // 3-step live security check list (prototype lines 498-502)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface2,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecurityCheckItem(
                        passed = isHttps,
                        label = if (isHttps) "HTTPS ব্যবহার হচ্ছে / Uses HTTPS" else "HTTPS প্রয়োজন / HTTPS required",
                    )
                    SecurityCheckItem(
                        passed = isPrivateBlocked,
                        label = "প্রাইভেট নেটওয়ার্ক ব্লক / Private networks blocked",
                    )
                    SecurityCheckItem(
                        passed = true,
                        label = "রিডাইরেক্ট সীমিত ও যাচাই হবে / Redirects re-checked",
                    )
                }
            }

            Button(
                onClick = onDismiss,
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(stringResource(Res.string.settings_mcp_add_server), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SecurityCheckItem(passed: Boolean, label: String) {
    val colors = LocalDoraiColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (passed) colors.ok else colors.err,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = if (passed) colors.text else colors.err, fontSize = 12.5.sp)
    }
}

@Composable
private fun SandboxSettingsContent(
    sandboxState: SandboxUiState? = null,
    onToggleSandbox: ((Boolean) -> Unit)? = null,
    onResetSandbox: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    var isRunning by remember { mutableStateOf(sandboxState?.sandboxReady ?: true) }
    var allowInternet by remember { mutableStateOf(true) }

    // Card 1: Status & Start/Stop
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Linux Sandbox", color = colors.text, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    Text(if (isRunning) "চলছে · Running" else "বন্ধ · Stopped", color = colors.muted, fontSize = 12.sp)
                }
            }
            Button(
                onClick = {
                    val next = !isRunning
                    isRunning = next
                    onToggleSandbox?.invoke(next)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) colors.err.copy(alpha = 0.15f) else colors.primary,
                    contentColor = if (isRunning) colors.err else colors.onPrimary,
                ),
            ) {
                Text(if (isRunning) "Stop" else "Start", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Card 2: Storage Meter Bar (Prototype line 513)
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("স্টোরেজ / Storage", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("1.2 / 4.0 GB", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(colors.surface2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.30f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.primary),
            )
        }
    }

    // Card 3: Network isolation policy
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("নেটওয়ার্ক / Network", color = colors.text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                Text("Allow internet for package installs", color = colors.muted, fontSize = 12.sp)
            }
            Switch(checked = allowInternet, onCheckedChange = { allowInternet = it })
        }
    }

    // Card 4: Run workspace setup again
    Button(
        onClick = { onResetSandbox?.invoke() },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.text),
        border = BorderStroke(1.dp, colors.line),
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Run workspace setup again", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountSettingsContent() {
    val colors = LocalDoraiColors.current
    var syncEnabled by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Card 1: Account Profile Header
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoAnimation(size = 40.dp, animate = false)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("DORAI Local Account", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("dorai-local · on-device encrypted vault", color = colors.muted, fontSize = 12.5.sp)
            }
        }
    }

    // Card 2: Sync switch
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("সিঙ্ক / Sync", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("বন্ধ থাকলে সব ডেটা শুধু এই ডিভাইসে / Stays on this device", color = colors.muted, fontSize = 12.5.sp)
            }
            Switch(checked = syncEnabled, onCheckedChange = { syncEnabled = it })
        }
    }

    // Card 2: Combined Data Actions (Prototype line 514)
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ডেটা এক্সপোর্ট / Export data", color = colors.text, fontSize = 14.sp)
            TextButton(onClick = {}) {
                Text("Export", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = colors.line.copy(alpha = 0.4f), thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("সব চ্যাট মুছুন / Delete all chats", color = colors.text, fontSize = 14.sp)
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.err.copy(alpha = 0.15f), contentColor = colors.err),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("Delete", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteConfirm) {
        DoraiModalSheet(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(DoraiSpacing.LG),
                verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
            ) {
                Text("সব চ্যাট মুছবেন? / Delete all chats?", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("এটি ফেরানো যাবে না। প্রজেক্ট ফাইল মুছবে না। / This cannot be undone. Project files are not deleted.", color = colors.muted, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.text),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) {
                        Text("বাতিল / Cancel")
                    }
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.err, contentColor = colors.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) {
                        Text("মুছুন / Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentPolicyHeader(
    appSettings: AppSettings = koinInject(),
) {
    val colors = LocalDoraiColors.current
    var defaultMode by remember { mutableStateOf(appSettings.getDefaultAgentMode()) }
    var policy by remember { mutableStateOf(appSettings.getApprovalPolicy()) }
    var timelineEnabled by remember { mutableStateOf(appSettings.isTimelineEnabled()) }
    var autoReadEnabled by remember { mutableStateOf(appSettings.isAutoReadEnabled()) }

    val modes = listOf(
        Triple("ask", "Ask", "Q&A only. No tools run."),
        Triple("plan", "Plan", "Reads files and writes a plan. Changes nothing."),
        Triple("build", "Build", "Edits files and runs commands, asking before risky steps."),
        Triple("review", "Review", "Reads code and provides comments."),
    )

    val policies = listOf(
        "all" to "Ask for everything",
        "edits" to "Ask for edits and shell",
        "read" to "Auto read-only",
    )

    // Card 1: Default Mode
    SettingsSectionCard {
        Text("Default mode", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        ) {
            modes.forEach { (id, label, _) ->
                val isSelected = defaultMode == id
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else colors.surface2)
                        .border(1.dp, if (isSelected) colors.primary else colors.line, CircleShape)
                        .clickable {
                            defaultMode = id
                            appSettings.setDefaultAgentMode(id)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.onPrimary else colors.text,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        val currentDesc = modes.firstOrNull { it.first == defaultMode }?.third.orEmpty()
        if (currentDesc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(DoraiSpacing.XS))
            Text(currentDesc, color = colors.muted, fontSize = 12.5.sp)
        }
    }

    // Card 2: Approval Policy
    SettingsSectionCard {
        Text("Approval policy", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        ) {
            policies.forEach { (id, label) ->
                val isSelected = policy == id
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else colors.surface2)
                        .border(1.dp, if (isSelected) colors.primary else colors.line, CircleShape)
                        .clickable {
                            policy = id
                            appSettings.setApprovalPolicy(id)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.onPrimary else colors.text,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }

    // Card 3: Activity Timeline Toggle
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Activity timeline", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Show steps as a rail in the Agent activity view", color = colors.muted, fontSize = 12.5.sp)
            }
            Switch(
                checked = timelineEnabled,
                onCheckedChange = {
                    timelineEnabled = it
                    appSettings.setTimelineEnabled(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                ),
            )
        }
    }

    // Card 4: Auto-run read-only tools
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-run read-only tools", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("read_file and search run without asking for approval", color = colors.muted, fontSize = 12.5.sp)
            }
            Switch(
                checked = autoReadEnabled,
                onCheckedChange = {
                    autoReadEnabled = it
                    appSettings.setAutoReadEnabled(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                ),
            )
        }
    }
}

@Composable
private fun AccountProfileCard() {
    val colors = LocalDoraiColors.current
    var syncEnabled by remember { mutableStateOf(false) }

    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(DoraiSpacing.MD))
            Column {
                Text(
                    text = "Local Device Account",
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Offline-first · Zero remote tracking",
                    color = colors.muted,
                    fontSize = 13.sp,
                )
            }
        }
    }

    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Local-only storage", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("When active, all chat data and keys stay strictly on this device", color = colors.muted, fontSize = 12.5.sp)
            }
            Switch(
                checked = !syncEnabled,
                onCheckedChange = { syncEnabled = !it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                ),
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(content: @Composable () -> Unit) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .padding(DoraiSpacing.MD),
    ) {
        content()
    }
}
