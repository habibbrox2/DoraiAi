package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_overview_active_model
import dorai.composeapp.generated.resources.settings_overview_active_service
import dorai.composeapp.generated.resources.settings_overview_connectors_format
import dorai.composeapp.generated.resources.settings_overview_connectors_label
import dorai.composeapp.generated.resources.settings_overview_no_service
import dorai.composeapp.generated.resources.settings_overview_sandbox
import dorai.composeapp.generated.resources.settings_overview_sandbox_installed
import dorai.composeapp.generated.resources.settings_overview_sandbox_not_installed
import dorai.composeapp.generated.resources.settings_overview_title
import dorai.composeapp.generated.resources.settings_overview_tools_format
import dorai.composeapp.generated.resources.settings_overview_tools_label
import org.jetbrains.compose.resources.stringResource

/**
 * Concise settings overview surface (Plan 2, Phase 3). Summarizes the current
 * configuration so users get an at-a-glance status before diving into sections.
 * All values are derived from [SettingsUiState]; no new state is introduced.
 *
 * @param sandboxInstalled sandbox availability comes from the sandbox view model,
 * not [SettingsUiState], so it is passed in explicitly.
 */
@Composable
fun SettingsOverview(
    uiState: SettingsUiState,
    sandboxInstalled: Boolean,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sm)) {
            Text(
                text = stringResource(Res.string.settings_overview_title),
                style = DoraiTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            val active = uiState.configuredServices.firstOrNull()
            SettingsKeyValueRow(
                label = stringResource(Res.string.settings_overview_active_service),
                value = active?.service?.displayName ?: stringResource(Res.string.settings_overview_no_service),
            )
            SettingsKeyValueRow(
                label = stringResource(Res.string.settings_overview_active_model),
                value = when {
                    active?.useCustomModel == true && active.customModelId.isNotBlank() -> active.customModelId
                    active?.selectedModel != null -> active.selectedModel.id
                    else -> "—"
                },
            )
            SettingsKeyValueRow(
                label = stringResource(Res.string.settings_overview_tools_label),
                value = stringResource(Res.string.settings_overview_tools_format, uiState.tools.count { it.isEnabled }, uiState.tools.size),
            )
            SettingsKeyValueRow(
                label = stringResource(Res.string.settings_overview_connectors_label),
                value = stringResource(Res.string.settings_overview_connectors_format, uiState.mcpServers.count { it.isEnabled }),
            )
            SettingsKeyValueRow(
                label = stringResource(Res.string.settings_overview_sandbox),
                value = stringResource(
                    if (sandboxInstalled) {
                        Res.string.settings_overview_sandbox_installed
                    } else {
                        Res.string.settings_overview_sandbox_not_installed
                    },
                ),
            )
            Spacer(Modifier.height(SettingsSpacing.xxs))
        }
    }
}
