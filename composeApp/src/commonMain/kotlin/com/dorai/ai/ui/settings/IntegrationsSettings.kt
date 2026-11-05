package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_open_github_issue
import dorai.composeapp.generated.resources.settings_request_integration_description
import dorai.composeapp.generated.resources.settings_request_integration_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun IntegrationsContent(
    splinterlandsViewModel: SplinterlandsViewModel = koinViewModel(),
) {
    val splinterlandsState by splinterlandsViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { splinterlandsViewModel.onScreenVisible() }

    IntegrationsContent(splinterlandsState)
}

/** Stateless integration surface for deterministic screenshot and preview rendering. */
@Composable
fun IntegrationsContent(splinterlandsState: SplinterlandsUiState) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (splinterlandsState.showSplinterlandsSection) {
            SettingsCard {
                SplinterlandsSection(
                    isEnabled = splinterlandsState.isSplinterlandsEnabled,
                    accounts = splinterlandsState.splinterlandsAccounts,
                    instanceIds = splinterlandsState.splinterlandsInstanceIds,
                    addStatus = splinterlandsState.splinterlandsAddStatus,
                    battleLog = splinterlandsState.splinterlandsBattleLog,
                    availableServices = splinterlandsState.splinterlandsAvailableServices,
                    onToggle = splinterlandsState.onToggleSplinterlands,
                    onTestAndAddAccount = splinterlandsState.onTestAndAddSplinterlandsAccount,
                    onRemoveAccount = splinterlandsState.onRemoveSplinterlandsAccount,
                    onAddService = splinterlandsState.onAddSplinterlandsService,
                    onRemoveService = splinterlandsState.onRemoveSplinterlandsService,
                    onReorderServices = splinterlandsState.onReorderSplinterlandsServices,
                    onStartBattle = splinterlandsState.onStartSplinterlandsBattle,
                    onStopBattle = splinterlandsState.onStopSplinterlandsBattle,
                    onClearBattleLog = splinterlandsState.onClearSplinterlandsBattleLog,
                )
            }
        }
        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.settings_request_integration_title),
                    style = DoraiTypography.titleMedium,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.settings_request_integration_description),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://github.com/habibbrox2/DoraiAi/issues/new?template=integration_request.yml") },
                    modifier = Modifier.handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_open_github_issue))
                }
            }
        }
    }
}
