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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.firebase_remote_alerts_description
import dorai.composeapp.generated.resources.firebase_remote_alerts_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun FirebaseAlertsCard(
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    deviceToken: String? = null,
    onSendTestAlert: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
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
                            color = LocalDesignColorTokens.current.textPrimary,
                        )
                        Text(
                            text = stringResource(Res.string.firebase_remote_alerts_description),
                            style = DoraiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textSecondary,
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

                if (!deviceToken.isNullOrBlank()) {
                    Text(
                        text = "Device Token: ${deviceToken.take(16)}...",
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = onSendTestAlert,
                    modifier = Modifier.handCursor(),
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send Test Alert")
                }
            }
        }
    }
}
