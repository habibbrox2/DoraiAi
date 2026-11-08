package com.dorai.ai.ui.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.Version
import com.dorai.ai.linux.LinuxDistro
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiListDetailPaneScaffold
import com.dorai.ai.ui.adaptive.DoraiWindowWidthClass
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
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
import org.koin.compose.viewmodel.koinViewModel

enum class SettingsCategory(
    val id: String,
    val titleRes: StringResource,
) {
    General("general", Res.string.settings_cat_general),
    Appearance("appearance", Res.string.settings_cat_appearance),
    Agent("agent", Res.string.settings_cat_agent),
    Providers("providers", Res.string.settings_cat_providers),
    Tools("tools", Res.string.settings_cat_tools),
    Mcp("mcp", Res.string.settings_cat_mcp),
    Sandbox("sandbox", Res.string.settings_cat_sandbox),
    Account("account", Res.string.settings_cat_account),
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
    currentThemeStyle: ThemeStyle,
    onThemeStyleSelected: (ThemeStyle) -> Unit,
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
    currentThemeStyle: ThemeStyle,
    onThemeStyleSelected: (ThemeStyle) -> Unit,
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

@Composable
private fun SettingsCategoryContent(
    category: SettingsCategory,
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    currentThemeStyle: ThemeStyle,
    onThemeStyleSelected: (ThemeStyle) -> Unit,
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
                    currentThemeStyle = currentThemeStyle,
                    onThemeStyleSelected = onThemeStyleSelected,
                )

                SettingsCategory.Agent -> AgentContent(uiState = uiState, actions = actions)

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
                    DataControlContent(uiState = uiState, actions = actions)
                    AccountPlanContent(uiState = uiState)
                }
            }
        } else {
            when (category) {
                SettingsCategory.General -> GeneralSettingsContent()

                SettingsCategory.Appearance -> AppearanceSettingsContent(
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected,
                    currentThemeStyle = currentThemeStyle,
                    onThemeStyleSelected = onThemeStyleSelected,
                )

                SettingsCategory.Agent -> AgentSettingsContent()

                SettingsCategory.Providers -> ProvidersSettingsContent()

                SettingsCategory.Tools -> ToolsSettingsContent()

                SettingsCategory.Mcp -> McpSettingsContent()

                SettingsCategory.Sandbox -> SandboxSettingsContent()

                SettingsCategory.Account -> AccountSettingsContent()
            }
        }
    }
}

@Composable
private fun GeneralSettingsContent() {
    val colors = LocalDoraiColors.current
    SettingsSectionCard {
        Text("Language", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("App Language: বাংলা (Bengali) / English", color = colors.muted, fontSize = 13.sp)
    }
}

@Composable
private fun AppearanceSettingsContent(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    currentThemeStyle: ThemeStyle,
    onThemeStyleSelected: (ThemeStyle) -> Unit,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current

    SettingsSectionCard {
        Text("Surface Style", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Row(horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM)) {
            StyleChip(
                label = "Flat",
                isSelected = currentThemeStyle == ThemeStyle.Flat,
                onClick = { onThemeStyleSelected(ThemeStyle.Flat) },
            )
            StyleChip(
                label = "3D Neumorphic",
                isSelected = currentThemeStyle == ThemeStyle.ThreeD,
                onClick = { onThemeStyleSelected(ThemeStyle.ThreeD) },
            )
        }
    }

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
private fun StyleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalDoraiColors.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) colors.primary else colors.surface2)
            .border(1.dp, if (isSelected) colors.primary else colors.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.onPrimary else colors.text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AgentSettingsContent() {
    val colors = LocalDoraiColors.current
    var policy by remember { mutableStateOf("edits") }
    var timelineEnabled by remember { mutableStateOf(true) }

    SettingsSectionCard {
        Text(stringResource(Res.string.settings_approval_policy), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = policy == "all", onCheckedChange = { policy = "all" })
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                Text(stringResource(Res.string.settings_approval_ask_all), color = colors.text, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = policy == "edits", onCheckedChange = { policy = "edits" })
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                Text(stringResource(Res.string.settings_approval_ask_edits), color = colors.text, fontSize = 14.sp)
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
                Text(stringResource(Res.string.settings_timeline_toggle), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.settings_timeline_desc), color = colors.muted, fontSize = 13.sp)
            }
            Switch(checked = timelineEnabled, onCheckedChange = { timelineEnabled = it })
        }
    }
}

@Composable
private fun ProvidersSettingsContent() {
    val colors = LocalDoraiColors.current
    SettingsSectionCard {
        Text("Configured Providers", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        ProviderRow("Free FAST", isLocal = false, keyStatus = "••••default", isDefault = true)
        ProviderRow("Ollama Local", isLocal = true, keyStatus = "••••local", isDefault = false)
    }
}

@Composable
private fun ProviderRow(name: String, isLocal: Boolean, keyStatus: String, isDefault: Boolean) {
    val colors = LocalDoraiColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                DoraiProviderBadge(isLocal = isLocal, iconOnly = true)
            }
            Text("API Key: $keyStatus", color = colors.muted, fontSize = 12.sp)
        }
        if (isDefault) {
            DoraiStatusBadge("Default", colors.ok)
        }
    }
}

@Composable
private fun ToolsSettingsContent() {
    val colors = LocalDoraiColors.current
    SettingsSectionCard {
        Text("AI Tools & Permissions", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        ToolPermissionRow("edit_file", "Asks first", isRisky = true)
        ToolPermissionRow("run_shell", "Asks first", isRisky = true)
        ToolPermissionRow("read_file", "Auto", isRisky = false)
    }
}

@Composable
private fun ToolPermissionRow(name: String, badgeText: String, isRisky: Boolean) {
    val colors = LocalDoraiColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        DoraiStatusBadge(badgeText, if (isRisky) colors.warn else colors.info)
    }
}

@Composable
private fun McpSettingsContent() {
    val colors = LocalDoraiColors.current
    var urlInput by remember { mutableStateOf("") }
    val isValidUrl = urlInput.startsWith("https://") || urlInput.startsWith("http://localhost")

    SettingsSectionCard {
        Text(stringResource(Res.string.settings_mcp_add_server), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            placeholder = { Text(stringResource(Res.string.settings_mcp_url_hint), color = colors.muted) },
            isError = urlInput.isNotEmpty() && !isValidUrl,
            supportingText = {
                if (urlInput.isNotEmpty() && !isValidUrl) {
                    Text(stringResource(Res.string.settings_mcp_invalid_url), color = colors.err)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.info,
                unfocusedBorderColor = colors.line,
            ),
        )

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        Button(
            onClick = {},
            enabled = isValidUrl,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(stringResource(Res.string.settings_mcp_test_connection))
        }
    }
}

@Composable
private fun SandboxSettingsContent() {
    val colors = LocalDoraiColors.current
    SettingsSectionCard {
        Text("Linux Sandbox Status", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Environment: Debian 12 (Bookworm)", color = colors.text, fontSize = 13.sp)
            DoraiStatusBadge("Running", colors.ok)
        }
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Text("Storage: 1.2 GB / 4.0 GB used", color = colors.muted, fontSize = 13.sp)
    }
}

@Composable
private fun AccountSettingsContent() {
    val colors = LocalDoraiColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    SettingsSectionCard {
        Text("Data Control & Export", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Text("All data stays on device when cloud sync is disabled.", color = colors.muted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(DoraiSpacing.MD))

        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.err),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(Res.string.settings_delete_all_chats))
        }

        if (showDeleteConfirm) {
            DoraiModalSheet(onDismissRequest = { showDeleteConfirm = false }) {
                Column(verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD)) {
                    Text("Confirm Deletion", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.settings_delete_chats_confirm), color = colors.muted, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.text),
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                        Button(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.err, contentColor = colors.text),
                        ) {
                            Text("Delete All")
                        }
                    }
                }
            }
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
