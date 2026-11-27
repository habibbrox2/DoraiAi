package com.dorai.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.cloud_sync_backup_button
import dorai.composeapp.generated.resources.cloud_sync_passphrase_label
import dorai.composeapp.generated.resources.cloud_sync_passphrase_min_length
import dorai.composeapp.generated.resources.cloud_sync_restore_button
import dorai.composeapp.generated.resources.cloud_sync_security_notice
import dorai.composeapp.generated.resources.cloud_sync_vault_description
import dorai.composeapp.generated.resources.cloud_sync_vault_title
import dorai.composeapp.generated.resources.sp_hide_password
import dorai.composeapp.generated.resources.sp_show_password
import org.jetbrains.compose.resources.stringResource

@Composable
fun CloudVaultCard(
    onBackup: (passphrase: String) -> Unit,
    onRestore: (passphrase: String) -> Unit,
    isSyncing: Boolean = false,
    syncStatusMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var passphrase by remember { mutableStateOf("") }
    var showPassphrase by remember { mutableStateOf(false) }

    SettingsCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.cloud_sync_vault_title),
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                    Text(
                        text = stringResource(Res.string.cloud_sync_vault_description),
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Zero-knowledge security badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPrimary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.cloud_sync_security_notice),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Passphrase input
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text(stringResource(Res.string.cloud_sync_passphrase_label)) },
                singleLine = true,
                visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(
                        onClick = { showPassphrase = !showPassphrase },
                        modifier = Modifier.handCursor(),
                    ) {
                        Icon(
                            imageVector = if (showPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(
                                if (showPassphrase) Res.string.sp_hide_password else Res.string.sp_show_password,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (passphrase.length < 8) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.cloud_sync_passphrase_min_length),
                    style = DoraiTypography.labelSmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }

            if (!syncStatusMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = syncStatusMessage,
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onBackup(passphrase) },
                    enabled = passphrase.length >= 8 && !isSyncing,
                    modifier = Modifier.weight(1f).handCursor(),
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.cloud_sync_backup_button))
                    }
                }

                OutlinedButton(
                    onClick = { onRestore(passphrase) },
                    enabled = passphrase.length >= 8 && !isSyncing,
                    modifier = Modifier.weight(1f).handCursor(),
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.cloud_sync_restore_button))
                }
            }
        }
    }
}
