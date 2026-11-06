package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_open_github_issue
import dorai.composeapp.generated.resources.settings_request_integration_description
import dorai.composeapp.generated.resources.settings_request_integration_title
import org.jetbrains.compose.resources.stringResource

/**
 * "Request an integration" card shown at the bottom of the merged Tools tab.
 *
 * Third-party integrations are no longer a separate Settings tab; this is the
 * only remaining integration entry point. Splinterlands stays a runtime feature
 * but is not surfaced in Settings.
 */
@Composable
internal fun IntegrationRequestCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    SettingsCard(modifier = modifier) {
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
                onClick = {
                    uriHandler.openUri(
                        "https://github.com/habibbrox2/DoraiAi/issues/new?template=integration_request.yml",
                    )
                },
                modifier = Modifier.handCursor(),
            ) {
                Text(stringResource(Res.string.settings_open_github_issue))
            }
        }
    }
}
