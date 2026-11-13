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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.Version
import com.dorai.ai.data.AppSettings
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
import dorai.composeapp.generated.resources.action_cancel
import dorai.composeapp.generated.resources.action_delete
import dorai.composeapp.generated.resources.agent_activity_timeline
import dorai.composeapp.generated.resources.agent_activity_timeline_desc
import dorai.composeapp.generated.resources.agent_approval_policy
import dorai.composeapp.generated.resources.agent_auto_run_readonly
import dorai.composeapp.generated.resources.agent_auto_run_readonly_desc
import dorai.composeapp.generated.resources.agent_default_mode
import dorai.composeapp.generated.resources.agent_local_only_storage
import dorai.composeapp.generated.resources.agent_local_only_storage_desc
import dorai.composeapp.generated.resources.mode_ask
import dorai.composeapp.generated.resources.mode_build
import dorai.composeapp.generated.resources.mode_plan
import dorai.composeapp.generated.resources.mode_review
import dorai.composeapp.generated.resources.settings_approval_ask_all
import dorai.composeapp.generated.resources.settings_approval_ask_edits
import dorai.composeapp.generated.resources.settings_approval_auto_read
import dorai.composeapp.generated.resources.settings_approval_policy
import dorai.composeapp.generated.resources.settings_build_environment
import dorai.composeapp.generated.resources.settings_cat_account
import dorai.composeapp.generated.resources.settings_cat_agent
import dorai.composeapp.generated.resources.settings_cat_appearance
import dorai.composeapp.generated.resources.settings_cat_general
import dorai.composeapp.generated.resources.settings_cat_github
import dorai.composeapp.generated.resources.settings_cat_mcp
import dorai.composeapp.generated.resources.settings_cat_providers
import dorai.composeapp.generated.resources.settings_cat_tools
import dorai.composeapp.generated.resources.settings_delete_all_chats
import dorai.composeapp.generated.resources.settings_delete_chats_confirm
import dorai.composeapp.generated.resources.settings_github_description
import dorai.composeapp.generated.resources.settings_mcp_add_server
import dorai.composeapp.generated.resources.settings_mcp_invalid_url
import dorai.composeapp.generated.resources.settings_mcp_test_connection
import dorai.composeapp.generated.resources.settings_mcp_url_hint
import dorai.composeapp.generated.resources.settings_screen_title
import dorai.composeapp.generated.resources.settings_theme_presets
import dorai.composeapp.generated.resources.settings_timeline_desc
import dorai.composeapp.generated.resources.settings_timeline_toggle
import dorai.composeapp.generated.resources.status_connected
import dorai.composeapp.generated.resources.tools_group_title
import dorai.composeapp.generated.resources.v2_account_local_title
import dorai.composeapp.generated.resources.v2_account_offline_desc
import dorai.composeapp.generated.resources.v2_api_header
import dorai.composeapp.generated.resources.v2_asks_first
import dorai.composeapp.generated.resources.v2_authorization_system
import dorai.composeapp.generated.resources.v2_auto
import dorai.composeapp.generated.resources.v2_d1_account
import dorai.composeapp.generated.resources.v2_delete_all_chats
import dorai.composeapp.generated.resources.v2_delete_all_chats_confirm_body
import dorai.composeapp.generated.resources.v2_delete_all_chats_confirm_title
import dorai.composeapp.generated.resources.v2_edit_file
import dorai.composeapp.generated.resources.v2_export
import dorai.composeapp.generated.resources.v2_export_data
import dorai.composeapp.generated.resources.v2_https_in_use
import dorai.composeapp.generated.resources.v2_https_required
import dorai.composeapp.generated.resources.v2_local_sqlite_bridge
import dorai.composeapp.generated.resources.v2_local_vault
import dorai.composeapp.generated.resources.v2_mcp_bridge_summary
import dorai.composeapp.generated.resources.v2_mode_ask_desc
import dorai.composeapp.generated.resources.v2_mode_build_desc
import dorai.composeapp.generated.resources.v2_mode_plan_desc
import dorai.composeapp.generated.resources.v2_mode_review_desc
import dorai.composeapp.generated.resources.v2_no_auth
import dorai.composeapp.generated.resources.v2_oauth_2
import dorai.composeapp.generated.resources.v2_private_blocked
import dorai.composeapp.generated.resources.v2_read_file
import dorai.composeapp.generated.resources.v2_redirects_rechecked
import dorai.composeapp.generated.resources.v2_run_commands
import dorai.composeapp.generated.resources.v2_search_code
import dorai.composeapp.generated.resources.v2_skill_code_review_desc
import dorai.composeapp.generated.resources.v2_skill_code_review_title
import dorai.composeapp.generated.resources.v2_skill_compose_tips_desc
import dorai.composeapp.generated.resources.v2_skill_compose_tips_title
import dorai.composeapp.generated.resources.v2_skill_git_commit_desc
import dorai.composeapp.generated.resources.v2_skill_git_commit_title
import dorai.composeapp.generated.resources.v2_skills_title
import dorai.composeapp.generated.resources.v2_sync_desc
import dorai.composeapp.generated.resources.v2_sync_title
import dorai.composeapp.generated.resources.v2_test
import dorai.composeapp.generated.resources.v2_web_search
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
    GitHub("github", Res.string.settings_cat_github, Icons.Default.Code),
    Account("account", Res.string.settings_cat_account, Icons.Default.AccountCircle),
}

/**
 * UI v2 Settings screen:
 * - 9 categories, strictly ordered.
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
    androidUpdateContent: (@Composable () -> Unit)? = null,
    debugBuildStamp: String? = null,
    /** Navigates to the Workspace screen's Linux Sandbox section (skills CTA); null hides the CTA. */
    onNavigateToWorkspace: (() -> Unit)? = null,
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
                    androidUpdateContent = androidUpdateContent,
                    debugBuildStamp = debugBuildStamp,
                    onNavigateToWorkspace = onNavigateToWorkspace,
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
                    androidUpdateContent = androidUpdateContent,
                    debugBuildStamp = debugBuildStamp,
                    onNavigateToWorkspace = onNavigateToWorkspace,
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
 *
 * [onNavigateToWorkspace] is passed through from the caller (App.kt) so the Skills CTA
 * can jump to the Workspace screen, where Linux Sandbox now lives.
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
    onNavigateToWorkspace: (() -> Unit)? = null,
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
        androidUpdateContent = androidUpdateContent,
        debugBuildStamp = "v${Version.appVersion} · uiV2=on",
        onNavigateToWorkspace = onNavigateToWorkspace,
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
            text = stringResource(Res.string.settings_screen_title),
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = DoraiSpacing.SM).semantics { heading() },
        )

        SettingsCategory.entries.forEach { cat ->
            val isSelected = cat == selectedCategory
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (isSelected) colors.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        onClick = { onCategorySelected(cat) },
                        role = Role.Tab,
                    )
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
    androidUpdateContent: (@Composable () -> Unit)? = null,
    debugBuildStamp: String? = null,
    onNavigateToWorkspace: (() -> Unit)? = null,
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
                            Text(stringResource(Res.string.settings_build_environment), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                    // Skills stay visible on every platform: without the sandbox the section
                    // explains what is missing and links to the sandbox setup, so the capability
                    // is never silently hidden.
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
                        onNavigateToSandbox = onNavigateToWorkspace,
                        onBrowseSkills = actions.onBrowseSkills,
                    )
                    // Integrations used to be a separate Settings tab; the request entry
                    // point now closes the Tools page.
                    IntegrationRequestCard()
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
                        onAuthorizeMcpServer = actions.onAuthorizeMcpServer,
                        onRevokeMcpServerOAuth = actions.onRevokeMcpServerOAuth,
                    )
                }

                SettingsCategory.GitHub -> {
                    GitHubSection()
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

                SettingsCategory.GitHub -> GitHubSection()

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
        Text(stringResource(Res.string.settings_theme_presets, ThemePresets.all.size), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        text = stringResource(Res.string.tools_group_title),
        color = colors.muted,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
    SettingsSectionCard {
        val toolList = listOf(
            Triple("read_file", stringResource(Res.string.v2_read_file), "read_file"),
            Triple("search", stringResource(Res.string.v2_search_code), "search"),
            Triple("edit_file", stringResource(Res.string.v2_edit_file), "edit_file"),
            Triple("shell", stringResource(Res.string.v2_run_commands), "shell"),
            Triple("web_search", stringResource(Res.string.v2_web_search), "web_search"),
        )
        toolList.forEachIndexed { idx, (id, label, _) ->
            val isRisky = toolPolicies[id] == true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
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
                    text = if (isRisky) stringResource(Res.string.v2_asks_first) else stringResource(Res.string.v2_auto),
                    color = if (isRisky) colors.warn else colors.ok,
                )
            }
            if (idx < toolList.lastIndex) {
                HorizontalDivider(color = colors.line.copy(alpha = 0.4f), thickness = 1.dp)
            }
        }
    }

    Text(
        text = stringResource(Res.string.v2_skills_title),
        color = colors.muted,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
    SettingsSectionCard {
        val skills = listOf(
            Triple("code_review", stringResource(Res.string.v2_skill_code_review_title), stringResource(Res.string.v2_skill_code_review_desc)),
            Triple("git_commit", stringResource(Res.string.v2_skill_git_commit_title), stringResource(Res.string.v2_skill_git_commit_desc)),
            Triple("compose_tips", stringResource(Res.string.v2_skill_compose_tips_title), stringResource(Res.string.v2_skill_compose_tips_desc)),
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
        modifier = Modifier.fillMaxWidth().height(48.dp),
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
                    Text(stringResource(Res.string.v2_local_sqlite_bridge), color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(Res.string.v2_mcp_bridge_summary), color = colors.muted, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DoraiStatusBadge(stringResource(Res.string.status_connected), colors.ok)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {}) {
                    Text(stringResource(Res.string.v2_test), color = colors.primary, fontSize = 13.sp)
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
            Text(stringResource(Res.string.v2_authorization_system), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "oauth" to stringResource(Res.string.v2_oauth_2),
                    "header" to stringResource(Res.string.v2_api_header),
                    "none" to stringResource(Res.string.v2_no_auth),
                ).forEach { (type, label) ->
                    val isSel = authType == type
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSel) colors.primary else colors.surface2)
                            .border(1.dp, if (isSel) colors.primary else colors.line, CircleShape)
                            .clickable(role = Role.Button) { authType = type }
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
                        label = if (isHttps) {
                            stringResource(Res.string.v2_https_in_use)
                        } else {
                            stringResource(Res.string.v2_https_required)
                        },
                    )
                    SecurityCheckItem(
                        passed = isPrivateBlocked,
                        label = stringResource(Res.string.v2_private_blocked),
                    )
                    SecurityCheckItem(
                        passed = true,
                        label = stringResource(Res.string.v2_redirects_rechecked),
                    )
                }
            }

            Button(
                onClick = onDismiss,
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
                Text(stringResource(Res.string.v2_d1_account), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.v2_local_vault), color = colors.muted, fontSize = 12.5.sp)
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
                Text(stringResource(Res.string.v2_sync_title), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(Res.string.v2_sync_desc), color = colors.muted, fontSize = 12.5.sp)
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
            Text(stringResource(Res.string.v2_export_data), color = colors.text, fontSize = 14.sp)
            TextButton(onClick = {}) {
                Text(stringResource(Res.string.v2_export), color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = colors.line.copy(alpha = 0.4f), thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.v2_delete_all_chats), color = colors.text, fontSize = 14.sp)
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.err.copy(alpha = 0.15f), contentColor = colors.err),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(stringResource(Res.string.action_delete), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteConfirm) {
        DoraiModalSheet(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(DoraiSpacing.LG),
                verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
            ) {
                Text(stringResource(Res.string.v2_delete_all_chats_confirm_title), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.v2_delete_all_chats_confirm_body), color = colors.muted, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.text),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.err, contentColor = colors.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(stringResource(Res.string.action_delete))
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
        Triple("ask", stringResource(Res.string.mode_ask), stringResource(Res.string.v2_mode_ask_desc)),
        Triple("plan", stringResource(Res.string.mode_plan), stringResource(Res.string.v2_mode_plan_desc)),
        Triple("build", stringResource(Res.string.mode_build), stringResource(Res.string.v2_mode_build_desc)),
        Triple("review", stringResource(Res.string.mode_review), stringResource(Res.string.v2_mode_review_desc)),
    )

    val policies = listOf(
        "all" to stringResource(Res.string.settings_approval_ask_all),
        "edits" to stringResource(Res.string.settings_approval_ask_edits),
        "read" to stringResource(Res.string.settings_approval_auto_read),
    )

    // Card 1: Default Mode
    SettingsSectionCard {
        Text(stringResource(Res.string.agent_default_mode), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                        .clickable(role = Role.Button) {
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
        Text(stringResource(Res.string.agent_approval_policy), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                        .clickable(role = Role.Button) {
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
                Text(stringResource(Res.string.agent_activity_timeline), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.agent_activity_timeline_desc), color = colors.muted, fontSize = 12.5.sp)
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
                Text(stringResource(Res.string.agent_auto_run_readonly), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.agent_auto_run_readonly_desc), color = colors.muted, fontSize = 12.5.sp)
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
                    text = stringResource(Res.string.v2_account_local_title),
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.v2_account_offline_desc),
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
                Text(stringResource(Res.string.agent_local_only_storage), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.agent_local_only_storage_desc), color = colors.muted, fontSize = 12.5.sp)
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
