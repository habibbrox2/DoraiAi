package com.dorai.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.DoraiTypography
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_navigate_back
import org.jetbrains.compose.resources.stringResource

/** The rounded surface card that groups one Settings section. */
@Composable
internal fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDoraiColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            // shadow() must precede background/border: it draws behind this node's
            // content, so applying it last would hide the elevation entirely.
            .shadow(elevation = 2.dp, shape = SettingsShapes.section)
            .background(colors.surface, SettingsShapes.section)
            .border(1.dp, colors.line, SettingsShapes.section)
            .padding(DoraiSpacing.MD),
        content = content,
    )
}

/**
 * Shared header for every Settings detail screen: back navigation, category icon,
 * screen title (semantic heading), optional description, and contextual actions.
 *
 * Formerly named `SettingsPageHeader`; renamed to make the single shared header
 * explicit. All Settings detail screens must use this one header so interaction
 * language, accessibility semantics, and spacing stay consistent.
 */
@Composable
internal fun SettingsDetailHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                androidx.compose.material3.IconButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.a11y_navigate_back),
                        tint = colors.text,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(DoraiSpacing.MD))
            Text(
                text = title,
                color = colors.text,
                style = DoraiTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (actions != null) Row(content = actions)
        }
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = DoraiTypography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(
                    start = 48.dp + 4.dp + 40.dp + DoraiSpacing.MD,
                    top = DoraiSpacing.XS,
                ),
            )
        }
    }
}

/**
 * A labelled group of settings rows: muted section label followed by a
 * [SettingsSectionCard]. [trailing] hosts group-level actions such as an "Add" button.
 *
 * Intentionally carries no `heading()` semantics — [SettingsDetailHeader] owns the one
 * heading per screen, so screen readers announce the title once and groups in order.
 */
@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDoraiColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = DoraiTypography.labelLarge,
                color = colors.muted,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
        SettingsSectionCard(content = content)
    }
}

/**
 * Card containing a single toggle row. The whole row is one switch target
 * ([Role.Switch]) so the label and the control are announced together, with a 48dp
 * minimum touch height.
 */
@Composable
internal fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val interactionSource = remember { MutableInteractionSource() }
    SettingsSectionCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Switch,
                ) { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DoraiTypography.titleSmall,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = DoraiTypography.bodySmall,
                    color = colors.muted,
                )
            }
            Spacer(modifier = Modifier.width(DoraiSpacing.SM))
            Switch(
                checked = checked,
                onCheckedChange = null,
                interactionSource = interactionSource,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.muted,
                    uncheckedTrackColor = colors.surface2,
                ),
            )
        }
    }
}

/** One selectable option in a [SettingsChipRow]. */
internal data class SettingsChip(
    val id: String,
    val label: String,
)

/**
 * Single-select pill chip row. Chips wrap on narrow screens, keep a 48dp minimum
 * height and expose [Role.RadioButton] selection semantics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsChipRow(
    options: List<SettingsChip>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
    ) {
        options.forEach { option ->
            val isSelected = option.id == selectedId
            val shape = CircleShape
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            // Pressed gets its own tone so a fast tap registers before the
            // selection flips; selected always wins over it.
            val chipBackground = when {
                isSelected -> colors.primary
                pressed -> colors.surface
                else -> colors.surface2
            }
            val chipBorder = when {
                isSelected -> colors.primary
                pressed -> colors.primary.copy(alpha = 0.45f)
                else -> colors.line
            }
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(shape)
                    .background(chipBackground)
                    .border(1.dp, chipBorder, shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                    ) { onSelect(option.id) }
                    .semantics { selected = isSelected }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    style = DoraiTypography.labelLarge,
                    color = if (isSelected) colors.onPrimary else colors.text,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}
