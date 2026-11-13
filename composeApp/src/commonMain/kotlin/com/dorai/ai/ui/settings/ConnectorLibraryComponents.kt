package com.dorai.ai.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.DoraiSearchField
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.components.StatusBadge
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_connectors
import dorai.composeapp.generated.resources.settings_library_added
import dorai.composeapp.generated.resources.settings_library_filter_no_setup
import dorai.composeapp.generated.resources.settings_plugins
import dorai.composeapp.generated.resources.settings_skills
import org.jetbrains.compose.resources.stringResource

/** Primary library surfaces inside Settings → Tools. */
internal enum class ConnectorLibrarySection {
    Connectors,
    Skills,
    Plugins,
}

/**
 * Section heading for a top-level block inside a settings page (Plugins, Skills, Connectors).
 *
 * The page already names the category, so the heading names the *block* and carries its own
 * count — no duplicate title and no floating counter.
 */
@Composable
internal fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    description: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = DoraiTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    style = DoraiTypography.labelMedium,
                    color = LocalDesignColorTokens.current.textTertiary,
                )
            }
        }
        if (description != null) {
            Text(
                text = description,
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
    }
}

/**
 * Sub-list heading inside a section ("Your connectors", "Top skills"): the title carries
 * the count so nothing floats at the far edge of the row.
 */
@Composable
internal fun SettingsSubsectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = DoraiTypography.titleSmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = count.toString(),
                style = DoraiTypography.labelMedium,
                color = LocalDesignColorTokens.current.textTertiary,
            )
        }
    }
}

@Composable
internal fun ConnectorLibraryTabs(
    selected: ConnectorLibrarySection,
    onSelect: (ConnectorLibrarySection) -> Unit,
    modifier: Modifier = Modifier,
    sections: List<ConnectorLibrarySection> = ConnectorLibrarySection.entries,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsShapes.pill)
            .background(LocalDesignColorTokens.current.surfaceElevated)
            .border(BorderStroke(1.dp, LocalDesignColorTokens.current.borderSubtle), SettingsShapes.pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        sections.forEach { section ->
            val isSelected = section == selected
            val label = when (section) {
                ConnectorLibrarySection.Connectors -> stringResource(Res.string.settings_connectors)
                ConnectorLibrarySection.Skills -> stringResource(Res.string.settings_skills)
                ConnectorLibrarySection.Plugins -> stringResource(Res.string.settings_plugins)
            }
            val background by animateColorAsState(
                targetValue = if (isSelected) LocalDesignColorTokens.current.primary.copy(alpha = 0.16f) else Color.Transparent,
                animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 180),
                label = "libraryTabBackground",
            )
            val foreground by animateColorAsState(
                targetValue = if (isSelected) LocalDesignColorTokens.current.primary else LocalDesignColorTokens.current.textSecondary,
                animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 180),
                label = "libraryTabForeground",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(SettingsShapes.pill)
                    .background(background)
                    .handCursor()
                    .selectable(selected = isSelected, onClick = { onSelect(section) }, role = Role.Tab),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = DoraiTypography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Search + optional filter + Add action. The filter narrows the curated catalog down to
 * the entries that need no setup.
 */
@Composable
internal fun ConnectorSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    addContentDescription: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    showFilter: Boolean = false,
    isFilterActive: Boolean = false,
    onToggleFilter: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DoraiSearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
        )
        if (showFilter && onToggleFilter != null) {
            val filterTint = if (isFilterActive) {
                LocalDesignColorTokens.current.primary
            } else {
                LocalDesignColorTokens.current.textSecondary
            }
            IconToggleButton(
                checked = isFilterActive,
                onCheckedChange = { onToggleFilter() },
                modifier = Modifier.size(48.dp).handCursor(),
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(Res.string.settings_library_filter_no_setup),
                    tint = filterTint,
                )
            }
        }
        FilledTonalIconButton(
            onClick = onAdd,
            modifier = Modifier.size(48.dp).handCursor(),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = addContentDescription,
            )
        }
    }
}

private val avatarPalette = listOf(
    DesignTokens.PrimaryCyan,
    DesignTokens.BrandBlue,
    DesignTokens.AgentViolet,
    DesignTokens.Success,
    DesignTokens.Warning,
    DesignTokens.BrandCyan,
)

@Composable
internal fun ConnectorAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    val color = remember(name) {
        avatarPalette[(name.hashCode() and Int.MAX_VALUE) % avatarPalette.size]
    }
    val shape = RoundedCornerShape(size / 3)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.38f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = DoraiTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/**
 * "Top connectors" / "Top skills" row: avatar, name, description, publisher meta,
 * and either an Add (+) action or an Added badge.
 */
@Composable
internal fun ConnectorDiscoverRow(
    name: String,
    description: String,
    meta: String,
    isAdded: Boolean,
    onAdd: () -> Unit,
    addContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsShapes.row)
            .clickable(enabled = !isAdded, role = Role.Button, onClick = onAdd)
            .handCursor()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConnectorAvatar(name = name)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = DoraiTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = description,
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = meta,
                style = DoraiTypography.labelSmall,
                color = LocalDesignColorTokens.current.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isAdded) {
            StatusBadge(
                label = stringResource(Res.string.settings_library_added),
                status = OperationalStatus.Connected,
            )
        } else {
            FilledTonalIconButton(
                onClick = onAdd,
                modifier = Modifier.size(40.dp).handCursor(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = addContentDescription,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
