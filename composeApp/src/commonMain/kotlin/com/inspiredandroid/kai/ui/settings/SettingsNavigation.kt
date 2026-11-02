package com.inspiredandroid.kai.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storage
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
import com.inspiredandroid.kai.prefersReducedMotion
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.theme.DesignTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.settings_tab_account
import kai.composeapp.generated.resources.settings_tab_agent
import kai.composeapp.generated.resources.settings_tab_data
import kai.composeapp.generated.resources.settings_tab_general
import kai.composeapp.generated.resources.settings_tab_integrations
import kai.composeapp.generated.resources.settings_tab_sandbox
import kai.composeapp.generated.resources.settings_tab_services
import kai.composeapp.generated.resources.settings_tab_tools
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
            .width(116.dp)
            .padding(12.dp)
            .clip(SettingsShapes.panel)
            .background(railBg)
            .border(BorderStroke(1.dp, railBorder), SettingsShapes.panel),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEach { tab ->
                val isSelected = currentTab == tab
                val icon = when (tab) {
                    SettingsTab.General -> Icons.Outlined.Settings
                    SettingsTab.Appearance -> Icons.Outlined.Palette
                    SettingsTab.Agent -> Icons.Outlined.SmartToy
                    SettingsTab.Services -> Icons.Outlined.Cloud
                    SettingsTab.Tools -> Icons.Outlined.Build
                    SettingsTab.Integrations -> Icons.Outlined.Extension
                    SettingsTab.Sandbox -> Icons.Outlined.Computer
                    SettingsTab.Data -> Icons.Outlined.Storage
                    SettingsTab.Account -> Icons.Outlined.AccountCircle
                }
                val label = when (tab) {
                    SettingsTab.General -> stringResource(Res.string.settings_tab_general)
                    SettingsTab.Appearance -> "Appearance"
                    SettingsTab.Agent -> stringResource(Res.string.settings_tab_agent)
                    SettingsTab.Services -> stringResource(Res.string.settings_tab_services)
                    SettingsTab.Tools -> stringResource(Res.string.settings_tab_tools)
                    SettingsTab.Integrations -> stringResource(Res.string.settings_tab_integrations)
                    SettingsTab.Sandbox -> stringResource(Res.string.settings_tab_sandbox)
                    SettingsTab.Data -> stringResource(Res.string.settings_tab_data)
                    SettingsTab.Account -> stringResource(Res.string.settings_tab_account)
                }

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryCyan.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 200),
                    label = "railBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryCyan else DesignTokens.SecondaryText,
                    animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 200),
                    label = "railText",
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryCyan.copy(alpha = 0.42f) else Color.Transparent,
                    animationSpec = tween(durationMillis = if (prefersReducedMotion()) 0 else 200),
                    label = "railBorder",
                )

                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(74.dp)
                        .clip(SettingsShapes.row)
                        .background(bgColor)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = borderColor,
                            shape = SettingsShapes.row,
                        )
                        .handCursor()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelectTab(tab) }
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = KaiTypography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                        if (isSelected) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(DesignTokens.PrimaryCyan, DesignTokens.PrimaryCyan.copy(alpha = 0.4f)),
                                        ),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
