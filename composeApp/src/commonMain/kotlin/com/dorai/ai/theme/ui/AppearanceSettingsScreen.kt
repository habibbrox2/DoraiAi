package com.dorai.ai.theme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.theme.ThemeViewModel
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.ui.components.CreateThemeDialog
import com.dorai.ai.theme.ui.components.ThemeSwatchCard
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.settings.SettingsCard
import com.dorai.ai.ui.settings.SettingsSpacing
import com.dorai.ai.ui.settings.ToggleableHeadline
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.appearance_create_theme
import dorai.composeapp.generated.resources.appearance_delete_confirm_action
import dorai.composeapp.generated.resources.appearance_delete_confirm_body
import dorai.composeapp.generated.resources.appearance_delete_confirm_title
import dorai.composeapp.generated.resources.appearance_match_device
import dorai.composeapp.generated.resources.appearance_match_device_desc
import dorai.composeapp.generated.resources.appearance_presets
import dorai.composeapp.generated.resources.appearance_style_3d
import dorai.composeapp.generated.resources.appearance_style_flat
import dorai.composeapp.generated.resources.appearance_surface_style
import dorai.composeapp.generated.resources.appearance_surface_style_desc
import dorai.composeapp.generated.resources.create_theme_cancel
import dorai.composeapp.generated.resources.Res
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Appearance tab content for the 2027 premium theme engine: Match Device switch, Flat/3D
 * surface style, the built-in preset grid (custom themes appended with a remove badge) and the
 * Create theme builder. All mutations persist instantly and recompose the whole app — no
 * restart.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DoraiAppearanceSettings(
    viewModel: ThemeViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var customThemeToDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.cardGap),
    ) {
        SettingsCard {
            ToggleableHeadline(
                title = stringResource(Res.string.appearance_match_device),
                description = stringResource(Res.string.appearance_match_device_desc),
                checked = state.matchDevice,
                onCheckedChange = viewModel::setMatchDevice,
            )
        }

        SettingsCard {
            Text(
                text = stringResource(Res.string.appearance_surface_style),
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.appearance_surface_style_desc),
                style = DoraiTypography.bodyMedium,
                color = LocalDesignColorTokens.current.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SurfaceStyleButton(
                    label = stringResource(Res.string.appearance_style_flat),
                    isSelected = state.activeStyle == ThemeStyle.Flat,
                    onClick = { viewModel.setThemeStyle(ThemeStyle.Flat) },
                )
                SurfaceStyleButton(
                    label = stringResource(Res.string.appearance_style_3d),
                    isSelected = state.activeStyle == ThemeStyle.ThreeD,
                    onClick = { viewModel.setThemeStyle(ThemeStyle.ThreeD) },
                )
            }
        }

        SettingsCard {
            Text(
                text = stringResource(Res.string.appearance_presets),
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.availablePresets.forEach { palette ->
                    ThemeSwatchCard(
                        palette = palette,
                        selected = palette.id == state.selectedThemeId,
                        onClick = { viewModel.selectTheme(palette.id) },
                        onDelete = if (palette.isCustom) {
                            { customThemeToDelete = palette.id }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        Button(
            onClick = { showCreateDialog = true },
            shape = DoraiShapes.pill,
            modifier = Modifier.height(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.appearance_create_theme), fontWeight = FontWeight.SemiBold)
        }
    }

    if (showCreateDialog) {
        CreateThemeDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, background, surface, primary ->
                if (viewModel.saveCustomTheme(name, background, surface, primary)) {
                    showCreateDialog = false
                }
            },
        )
    }

    val deleteId = customThemeToDelete
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { customThemeToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(Res.string.appearance_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.appearance_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomTheme(deleteId)
                    customThemeToDelete = null
                }) {
                    Text(
                        stringResource(Res.string.appearance_delete_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { customThemeToDelete = null }) {
                    Text(stringResource(Res.string.create_theme_cancel))
                }
            },
        )
    }
}

@Composable
private fun SurfaceStyleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalDesignColorTokens.current
    val shape = DoraiShapes.pill
    Box(
        modifier = Modifier
            .clip(shape)
            .handCursor()
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else tokens.surfaceElevated, shape)
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else tokens.borderSubtle,
                shape,
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = DoraiTypography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                tokens.textPrimary
            },
        )
    }
}
