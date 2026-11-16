package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.v2.ActionButton
import com.dorai.ai.ui.components.v2.ActionButtonVariant
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.conversations_clear_all
import dorai.composeapp.generated.resources.conversations_clear_all_confirm_action
import dorai.composeapp.generated.resources.conversations_clear_all_confirm_body
import dorai.composeapp.generated.resources.conversations_clear_all_confirm_title
import dorai.composeapp.generated.resources.data_manage_description
import dorai.composeapp.generated.resources.settings_configured_services_format
import dorai.composeapp.generated.resources.settings_tab_data
import dorai.composeapp.generated.resources.settings_version
import org.jetbrains.compose.resources.stringResource

@Composable
fun DataControlContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    var showClearAllConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_tab_data),
            style = DoraiTypography.titleMedium,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        SettingsCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.data_manage_description),
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                ActionButton(
                    text = stringResource(Res.string.conversations_clear_all),
                    onClick = { showClearAllConfirmationDialog = true },
                    variant = ActionButtonVariant.DESTRUCTIVE,
                )
            }
        }
    }

    if (showClearAllConfirmationDialog) {
        SettingsConfirmDialog(
            title = stringResource(Res.string.conversations_clear_all_confirm_title),
            message = stringResource(Res.string.conversations_clear_all_confirm_body),
            confirmLabel = stringResource(Res.string.conversations_clear_all_confirm_action),
            onConfirm = {
                showClearAllConfirmationDialog = false
                actions.onClearAllConversations()
            },
            onDismiss = {
                showClearAllConfirmationDialog = false
            },
        )
    }
}

@Composable
fun AccountPlanContent(
    uiState: SettingsUiState,
) {
    SettingsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "DORAI AI",
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            Text(
                text = stringResource(Res.string.settings_version, com.dorai.ai.Version.appVersion),
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
            Text(
                text = stringResource(Res.string.settings_configured_services_format, uiState.configuredServices.size),
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
    }
}
