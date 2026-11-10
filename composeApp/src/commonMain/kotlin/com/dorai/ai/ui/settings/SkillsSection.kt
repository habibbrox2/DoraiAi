package com.dorai.ai.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.skills.RegistrySkillEntry
import com.dorai.ai.skills.SkillManifest
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiOutlinedTextField
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.components.StatusBadge
import com.dorai.ai.ui.components.VerticalScrollbarForScroll
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_skills
import dorai.composeapp.generated.resources.settings_skills_add
import dorai.composeapp.generated.resources.settings_skills_add_github
import dorai.composeapp.generated.resources.settings_skills_browse
import dorai.composeapp.generated.resources.settings_skills_browse_failed
import dorai.composeapp.generated.resources.settings_skills_browse_loading
import dorai.composeapp.generated.resources.settings_skills_builtin
import dorai.composeapp.generated.resources.settings_skills_cancel
import dorai.composeapp.generated.resources.settings_skills_description
import dorai.composeapp.generated.resources.settings_skills_github_hint
import dorai.composeapp.generated.resources.settings_skills_github_url
import dorai.composeapp.generated.resources.settings_skills_install
import dorai.composeapp.generated.resources.settings_skills_installing
import dorai.composeapp.generated.resources.settings_skills_needs_sandbox
import dorai.composeapp.generated.resources.settings_skills_no_match
import dorai.composeapp.generated.resources.settings_skills_none
import dorai.composeapp.generated.resources.settings_skills_remove
import dorai.composeapp.generated.resources.settings_skills_search
import dorai.composeapp.generated.resources.settings_skills_search_empty
import dorai.composeapp.generated.resources.settings_skills_search_yours
import dorai.composeapp.generated.resources.settings_skills_setup_sandbox
import dorai.composeapp.generated.resources.settings_skills_top
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SkillsSection(
    skills: ImmutableList<SkillManifest>,
    onUninstallSkill: (String) -> Unit,
    showAddDialog: Boolean,
    onShowAddDialog: (Boolean) -> Unit,
    onInstallGitHub: (String) -> Unit,
    onInstallBrowsed: (RegistrySkillEntry) -> Unit,
    isInstalling: Boolean,
    installError: String?,
    browsableSkills: ImmutableList<RegistrySkillEntry>,
    isBrowsing: Boolean,
    browseFailed: Boolean,
    isSandboxInstalled: Boolean,
    onNavigateToSandbox: (() -> Unit)? = null,
    onBrowseSkills: () -> Unit = {},
) {
    var scope by remember { mutableStateOf(ConnectorLibraryScope.Yours) }
    var query by remember { mutableStateOf("") }
    val installedIds = remember(skills) { skills.map { it.id }.toSet() }

    // Discover is backed by the registry; fetch it lazily the first time the user
    // opens that scope instead of waiting for the Add dialog.
    LaunchedEffect(scope, isSandboxInstalled) {
        if (scope == ConnectorLibraryScope.Discover &&
            isSandboxInstalled &&
            browsableSkills.isEmpty() &&
            !isBrowsing &&
            !browseFailed
        ) {
            onBrowseSkills()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.settings_skills),
            style = DoraiTypography.titleLarge,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.settings_skills_description),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                    skills.size.toString()
                } else {
                    browsableSkills.size.toString()
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
                stringResource(Res.string.settings_skills_search_yours)
            } else {
                stringResource(Res.string.settings_skills_search)
            },
            addContentDescription = stringResource(Res.string.settings_skills_add),
            onAdd = { onShowAddDialog(true) },
        )

        Spacer(Modifier.height(16.dp))

        if (!isSandboxInstalled) {
            // Skills live in the Linux sandbox, so it must be installed first.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.settings_skills_needs_sandbox),
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(label = "Needs setup", status = OperationalStatus.NeedsSetup)
            }
            if (onNavigateToSandbox != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onNavigateToSandbox,
                    modifier = Modifier.align(Alignment.CenterHorizontally).handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_skills_setup_sandbox))
                }
            }
        } else {
            when (scope) {
                ConnectorLibraryScope.Yours -> {
                    val visible = if (query.isBlank()) {
                        skills
                    } else {
                        skills.filter { skill ->
                            skill.id.contains(query, ignoreCase = true) ||
                                skill.description.contains(query, ignoreCase = true)
                        }
                    }
                    when {
                        skills.isEmpty() -> CollectionEmptyState(
                            message = stringResource(Res.string.settings_skills_none),
                        )

                        visible.isEmpty() -> CollectionEmptyState(
                            message = stringResource(Res.string.settings_skills_no_match),
                        )

                        else -> {
                            for (skill in visible) {
                                SkillCard(
                                    skill = skill,
                                    onRemove = { onUninstallSkill(skill.id) },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                ConnectorLibraryScope.Discover -> {
                    when {
                        isBrowsing -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.settings_skills_browse_loading),
                                style = DoraiTypography.bodySmall,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }

                        browseFailed -> SettingsStatusBanner(
                            message = stringResource(Res.string.settings_skills_browse_failed),
                            severity = SettingsBannerSeverity.Error,
                        )

                        else -> {
                            val filtered = remember(browsableSkills, query) {
                                if (query.isBlank()) {
                                    browsableSkills
                                } else {
                                    browsableSkills.filter { entry ->
                                        entry.id.contains(query, ignoreCase = true) ||
                                            entry.description.contains(query, ignoreCase = true) ||
                                            entry.sourceName.contains(query, ignoreCase = true)
                                    }
                                }
                            }
                            if (filtered.isEmpty()) {
                                CollectionEmptyState(
                                    message = stringResource(Res.string.settings_skills_no_match),
                                )
                            } else {
                                Text(
                                    text = stringResource(Res.string.settings_skills_top),
                                    style = DoraiTypography.titleSmall,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                )
                                Spacer(Modifier.height(4.dp))
                                for (entry in filtered) {
                                    ConnectorDiscoverRow(
                                        name = entry.id,
                                        description = entry.description,
                                        meta = entry.sourceName,
                                        isAdded = entry.id in installedIds,
                                        addContentDescription = stringResource(Res.string.settings_skills_add),
                                        onAdd = { onInstallBrowsed(entry) },
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && isSandboxInstalled) {
        AddSkillDialog(
            onDismiss = { onShowAddDialog(false) },
            onInstallGitHub = onInstallGitHub,
            onInstallBrowsed = onInstallBrowsed,
            isInstalling = isInstalling,
            installError = installError,
            browsableSkills = browsableSkills,
            isBrowsing = isBrowsing,
            browseFailed = browseFailed,
            installedIds = installedIds,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillCard(
    skill: SkillManifest,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().handCursor(),
        colors = doraiAdaptiveCardColors(),
        border = doraiAdaptiveCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "/${skill.id}",
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (skill.isBuiltIn) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.settings_skills_builtin),
                            style = DoraiTypography.labelSmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = { Text(text = skill.description, style = DoraiTypography.bodyMedium) },
                    state = rememberTooltipState(),
                ) {
                    Text(
                        text = skill.description,
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Built-in skills ship in the app and cannot be uninstalled — hide the remove action for them.
            if (expanded && !skill.isBuiltIn) {
                Spacer(Modifier.height(8.dp))

                TextButton(onClick = { showRemoveConfirm = true }, modifier = Modifier.handCursor()) {
                    Text(
                        text = stringResource(Res.string.settings_skills_remove),
                        color = DangerAlert,
                    )
                }
            }
        }
    }

    if (showRemoveConfirm) {
        // Uninstall now asks first (Plan 4, Phase 5), matching file delete and
        // package uninstall elsewhere in the workspace.
        SettingsConfirmDialog(
            title = "Uninstall this skill?",
            message = "\"/${skill.id}\" will be removed from the sandbox.",
            confirmLabel = "Uninstall",
            onConfirm = {
                showRemoveConfirm = false
                onRemove()
            },
            onDismiss = { showRemoveConfirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSkillDialog(
    onDismiss: () -> Unit,
    onInstallGitHub: (String) -> Unit,
    onInstallBrowsed: (RegistrySkillEntry) -> Unit,
    isInstalling: Boolean,
    installError: String?,
    browsableSkills: ImmutableList<RegistrySkillEntry>,
    isBrowsing: Boolean,
    browseFailed: Boolean,
    installedIds: Set<String>,
) {
    var url by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    // Which browse row the user tapped, so we can show a spinner on just that row.
    var installingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(isInstalling) {
        if (!isInstalling) installingId = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val scrollState = rememberScrollState()
        Box {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = stringResource(Res.string.settings_skills_add),
                    style = DoraiTypography.headlineSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.settings_skills_add_github),
                    style = DoraiTypography.titleSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                DoraiOutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(Res.string.settings_skills_github_url)) },
                    singleLine = true,
                    enabled = !isInstalling,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.settings_skills_github_hint),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )

                if (installError != null) {
                    Spacer(Modifier.height(8.dp))
                    SettingsStatusBanner(
                        message = installError,
                        severity = SettingsBannerSeverity.Error,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.settings_skills_installing),
                            style = DoraiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.handCursor()) {
                        Text(stringResource(Res.string.settings_skills_cancel))
                    }
                    TextButton(
                        onClick = { onInstallGitHub(url) },
                        enabled = url.isNotBlank() && !isInstalling,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(stringResource(Res.string.settings_skills_install))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.settings_skills_browse),
                    style = DoraiTypography.titleSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Spacer(Modifier.height(8.dp))

                when {
                    isBrowsing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.settings_skills_browse_loading),
                                style = DoraiTypography.bodySmall,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }

                    browseFailed -> {
                        SettingsStatusBanner(
                            message = stringResource(Res.string.settings_skills_browse_failed),
                            severity = SettingsBannerSeverity.Error,
                        )
                    }

                    else -> {
                        DoraiOutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            label = { Text(stringResource(Res.string.settings_skills_search)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))

                        val filtered = remember(browsableSkills, search) {
                            val q = search.trim().lowercase()
                            if (q.isEmpty()) {
                                browsableSkills
                            } else {
                                browsableSkills.filter {
                                    it.id.lowercase().contains(q) ||
                                        it.description.lowercase().contains(q) ||
                                        it.sourceName.lowercase().contains(q)
                                }
                            }
                        }

                        if (filtered.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.settings_skills_search_empty),
                                style = DoraiTypography.bodySmall,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        } else {
                            for (entry in filtered) {
                                RegistrySkillRow(
                                    entry = entry,
                                    alreadyInstalled = entry.id in installedIds,
                                    enabled = !isInstalling,
                                    installing = isInstalling && installingId == entry.id,
                                    onInstall = {
                                        installingId = entry.id
                                        onInstallBrowsed(entry)
                                    },
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
            VerticalScrollbarForScroll(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrySkillRow(
    entry: RegistrySkillEntry,
    alreadyInstalled: Boolean,
    enabled: Boolean,
    installing: Boolean,
    onInstall: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DoraiShapes.card)
            .then(if (enabled && !alreadyInstalled) Modifier.clickable { onInstall() }.handCursor() else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceRaised.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "/${entry.id}",
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = { Text(text = entry.description, style = DoraiTypography.bodyMedium) },
                    state = rememberTooltipState(),
                ) {
                    Text(
                        text = entry.description,
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = entry.sourceName,
                    style = DoraiTypography.labelSmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
            if (installing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (alreadyInstalled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
