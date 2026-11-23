package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.firebase_config_missing
import dorai.composeapp.generated.resources.firebase_device_token_label
import dorai.composeapp.generated.resources.firebase_project_id_label
import dorai.composeapp.generated.resources.firebase_remote_alerts_description
import dorai.composeapp.generated.resources.firebase_remote_alerts_title
import dorai.composeapp.generated.resources.firebase_send_test_alert
import dorai.composeapp.generated.resources.firebase_server_token_label
import dorai.composeapp.generated.resources.firebase_test_alert_failed
import dorai.composeapp.generated.resources.firebase_test_alert_sending
import dorai.composeapp.generated.resources.firebase_test_alert_sent
import org.jetbrains.compose.resources.stringResource

@Composable
fun FirebaseAlertsCard(
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    deviceToken: String? = null,
    projectId: String = "",
    apiKey: String = "",
    onChangeProjectId: (String) -> Unit = {},
    onChangeApiKey: (String) -> Unit = {},
    onSendTestAlert: () -> Unit = {},
    isSending: Boolean = false,
    testResult: Result<String?>? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDesignColorTokens.current
    SettingsCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(Res.string.firebase_remote_alerts_title),
                            style = DoraiTypography.titleMedium,
                            color = tokens.textPrimary,
                        )
                        Text(
                            text = stringResource(Res.string.firebase_remote_alerts_description),
                            style = DoraiTypography.bodySmall,
                            color = tokens.textSecondary,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.handCursor(),
                )
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = projectId,
                    onValueChange = onChangeProjectId,
                    label = { Text(stringResource(Res.string.firebase_project_id_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onChangeApiKey,
                    label = { Text(stringResource(Res.string.firebase_server_token_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!deviceToken.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.firebase_device_token_label, "${deviceToken.take(16)}…"),
                        style = DoraiTypography.bodySmall,
                        color = tokens.textSecondary,
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onSendTestAlert,
                    enabled = !isSending,
                    modifier = Modifier.handCursor(),
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.firebase_send_test_alert))
                }

                val statusText = when {
                    isSending -> stringResource(Res.string.firebase_test_alert_sending)

                    testResult?.isSuccess == true -> stringResource(Res.string.firebase_test_alert_sent)

                    testResult?.exceptionOrNull() != null -> stringResource(
                        Res.string.firebase_test_alert_failed,
                        testResult.exceptionOrNull()?.message?.take(120).orEmpty(),
                    )

                    projectId.isBlank() || apiKey.isBlank() || deviceToken.isNullOrBlank() ->
                        stringResource(Res.string.firebase_config_missing)

                    else -> null
                }
                if (statusText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = DoraiTypography.bodySmall,
                        color = if (testResult?.isFailure == true) DangerAlert else tokens.textSecondary,
                    )
                }
            }
        }
    }
}
