package com.dorai.ai.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_tab_account
import dorai.composeapp.generated.resources.settings_tab_agent
import dorai.composeapp.generated.resources.settings_tab_appearance
import dorai.composeapp.generated.resources.settings_tab_data
import dorai.composeapp.generated.resources.settings_tab_general
import dorai.composeapp.generated.resources.settings_tab_github
import dorai.composeapp.generated.resources.settings_tab_sandbox
import dorai.composeapp.generated.resources.settings_tab_services
import dorai.composeapp.generated.resources.settings_tab_tools
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * 2027 Premium AI Agent Workspace Settings Navigation Rail.
 */
@Composable
fun SettingsNavRail(
    tabs: ImmutableList<SettingsTab>,
    currentTab: SettingsTab,
    onSelectTab: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val railBg = SettingsSurfaces.card()
    val railBorder = SettingsSurfaces.border()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(196.dp)
            .padding(12.dp)
            .clip(SettingsShapes.panel)
            .background(railBg)
            .border(BorderStroke(1.dp, railBorder), SettingsShapes.panel),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val group = settingsTabGroup(tab)
                val previousGroup = tabs.getOrNull(index - 1)?.let(::settingsTabGroup)
                if (group != previousGroup) {
                    Text(
                        text = group.uppercase(),
                        style = DoraiTypography.labelSmall,
                        color = LocalDesignColorTokens.current.textTertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, bottom = 4.dp),
                    )
                }
                val isSelected = currentTab == tab
                val icon = when (tab) {
                    SettingsTab.General -> Icons.Outlined.Settings
                    SettingsTab.Appearance -> Icons.Outlined.Palette
                    SettingsTab.Agent -> Icons.Outlined.SmartToy
                    SettingsTab.Services -> Icons.Outlined.Cloud
                    SettingsTab.Tools -> Icons.Outlined.Build
                    SettingsTab.GitHub -> Icons.Outlined.Code
                    SettingsTab.Account -> Icons.Outlined.AccountCircle
                }
                val label = when (tab) {
                    SettingsTab.General -> stringResource(Res.string.settings_tab_general)
                    SettingsTab.Appearance -> stringResource(Res.string.settings_tab_appearance)
                    SettingsTab.Agent -> stringResource(Res.string.settings_tab_agent)
                    SettingsTab.Services -> stringResource(Res.string.settings_tab_services)
                    SettingsTab.Tools -> stringResource(Res.string.settings_tab_tools)
                    SettingsTab.GitHub -> stringResource(Res.string.settings_tab_github)
                    SettingsTab.Account -> stringResource(Res.string.settings_tab_account)
                }

                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryCyan.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 200),
                    label = "railBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryCyan else LocalDesignColorTokens.current.textSecondary,
                    animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 200),
                    label = "railText",
                )
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isFocused -> DesignTokens.PrimaryCyan.copy(alpha = 0.55f)
                        isSelected -> DesignTokens.PrimaryCyan.copy(alpha = 0.42f)
                        else -> Color.Transparent
                    },
                    animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 200),
                    label = "railBorder",
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(SettingsShapes.row)
                        .background(bgColor)
                        .border(
                            width = if (isSelected || isFocused) 1.dp else 0.dp,
                            color = borderColor,
                            shape = SettingsShapes.row,
                        )
                        .handCursor()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            role = Role.Tab,
                        ) { onSelectTab(tab) }
                        .semantics {
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = DoraiTypography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(DesignTokens.PrimaryCyan),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun settingsTabGroup(tab: SettingsTab): String = when (tab) {
    SettingsTab.General, SettingsTab.Appearance, SettingsTab.Agent -> "Assistant"
    SettingsTab.Services -> "Models & services"
    SettingsTab.Tools, SettingsTab.GitHub -> "Connections"
    SettingsTab.Account -> "Account"
}
