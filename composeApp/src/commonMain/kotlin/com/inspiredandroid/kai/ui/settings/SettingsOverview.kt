package com.inspiredandroid.kai.ui.settings

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
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens

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
                text = "Overview",
                style = KaiTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            val active = uiState.configuredServices.firstOrNull()
            SettingsKeyValueRow(
                label = "Active service",
                value = active?.service?.displayName ?: "None configured",
            )
            SettingsKeyValueRow(
                label = "Active model",
                value = when {
                    active?.useCustomModel == true && active.customModelId.isNotBlank() -> active.customModelId
                    active?.selectedModel != null -> active.selectedModel.id
                    else -> "—"
                },
            )
            SettingsKeyValueRow(
                label = "Tools",
                value = "${uiState.tools.count { it.isEnabled }} enabled of ${uiState.tools.size}",
            )
            SettingsKeyValueRow(
                label = "Integrations",
                value = "${uiState.mcpServers.count { it.isEnabled }} MCP servers",
            )
            SettingsKeyValueRow(
                label = "Sandbox",
                value = if (sandboxInstalled) "Installed" else "Not installed",
            )
            Spacer(Modifier.height(SettingsSpacing.xxs))
        }
    }
}
