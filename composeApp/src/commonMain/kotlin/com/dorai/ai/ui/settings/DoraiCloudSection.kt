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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dorai.ai.cloud.DORAI_CLOUD_SUPABASE_PUBLISHABLE_KEY
import com.dorai.ai.cloud.DORAI_CLOUD_SUPABASE_URL
import com.dorai.ai.cloud.DoraiAuthClient
import com.dorai.ai.cloud.DoraiCloudClient
import com.dorai.ai.cloud.DoraiCloudSession
import com.dorai.ai.cloud.DoraiCloudUsageDto
import com.dorai.ai.data.AppSettings
import com.dorai.ai.httpClient
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_dorai_cloud_description
import dorai.composeapp.generated.resources.settings_dorai_cloud_email_label
import dorai.composeapp.generated.resources.settings_dorai_cloud_password_label
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_in
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_in_failed
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_out
import dorai.composeapp.generated.resources.settings_dorai_cloud_signed_in_as
import dorai.composeapp.generated.resources.settings_dorai_cloud_title
import dorai.composeapp.generated.resources.settings_dorai_cloud_usage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Settings card for the first-party Dorai Cloud account.
 *
 * Signing in stores a Supabase session in encrypted settings; the façade then
 * forwards that session to Supabase, so RLS is the only isolation boundary and
 * the app never holds a service-role key.
 */
@Composable
fun DoraiCloudSection(modifier: Modifier = Modifier) {
    val appSettings: AppSettings = koinInject()
    val scope = rememberCoroutineScope()

    var session by remember { mutableStateOf(appSettings.getDoraiCloudSession()) }
    var email by remember { mutableStateOf(session?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var usage by remember { mutableStateOf(DoraiCloudUsageDto()) }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val httpClient = remember { httpClient { } }
    val cloudClient = remember {
        DoraiCloudClient(
            httpClient = httpClient,
            baseUrl = appSettings.getDoraiCloudUrl(),
        )
    }
    val authClient = remember {
        DoraiAuthClient(
            httpClient = httpClient,
            supabaseUrl = appSettings.getSupabaseUrl().ifBlank { DORAI_CLOUD_SUPABASE_URL },
            publishableKey = appSettings.getSupabaseAnonKey().ifBlank { DORAI_CLOUD_SUPABASE_PUBLISHABLE_KEY },
        )
    }

    fun refreshUsage(active: DoraiCloudSession) {
        scope.launch {
            cloudClient.usage(active.accessToken).onSuccess { usage = it.usage }
        }
    }

    SettingsCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (session != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = LocalDesignColorTokens.current.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.settings_dorai_cloud_title),
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                    Text(
                        text = stringResource(Res.string.settings_dorai_cloud_description),
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val activeSession = session
            if (activeSession == null) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.settings_dorai_cloud_email_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(Res.string.settings_dorai_cloud_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.error,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        isBusy = true
                        errorMessage = null
                        scope.launch {
                            authClient.signInWithPassword(email, password)
                                .onSuccess { signedIn ->
                                    appSettings.setDoraiCloudSession(signedIn)
                                    session = signedIn
                                    password = ""
                                    refreshUsage(signedIn)
                                }
                                .onFailure { errorMessage = it.message }
                            isBusy = false
                        }
                    },
                    enabled = !isBusy && email.isNotBlank() && password.length >= 6,
                    modifier = Modifier.fillMaxWidth().handCursor(),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.settings_dorai_cloud_sign_in))
                    }
                }
            } else {
                Text(
                    text = stringResource(Res.string.settings_dorai_cloud_signed_in_as, activeSession.email.orEmpty()),
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        Res.string.settings_dorai_cloud_usage,
                        usage.embedding,
                        usage.memory_write,
                        usage.vault_write,
                    ),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            isBusy = true
                            scope.launch {
                                authClient.signOut(activeSession.accessToken)
                                appSettings.setDoraiCloudSession(null)
                                session = null
                                usage = DoraiCloudUsageDto()
                                isBusy = false
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).handCursor(),
                    ) {
                        Text(stringResource(Res.string.settings_dorai_cloud_sign_out))
                    }
                }
            }
        }
    }
}
