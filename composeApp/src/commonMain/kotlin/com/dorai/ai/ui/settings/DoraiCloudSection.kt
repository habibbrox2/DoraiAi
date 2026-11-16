package com.dorai.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dorai.ai.DeepLinks
import com.dorai.ai.cloud.DORAI_CLOUD_SUPABASE_PUBLISHABLE_KEY
import com.dorai.ai.cloud.DORAI_CLOUD_SUPABASE_URL
import com.dorai.ai.cloud.DoraiAuthClient
import com.dorai.ai.cloud.DoraiCloudClient
import com.dorai.ai.cloud.DoraiCloudOAuthPending
import com.dorai.ai.cloud.DoraiCloudSession
import com.dorai.ai.cloud.DoraiCloudUsageDto
import com.dorai.ai.data.AppSettings
import com.dorai.ai.data.DataRepository
import com.dorai.ai.httpClient
import com.dorai.ai.openUrl
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_dorai_cloud_description
import dorai.composeapp.generated.resources.settings_dorai_cloud_email_label
import dorai.composeapp.generated.resources.settings_dorai_cloud_oauth_failed
import dorai.composeapp.generated.resources.settings_dorai_cloud_or_email
import dorai.composeapp.generated.resources.settings_dorai_cloud_password_label
import dorai.composeapp.generated.resources.settings_dorai_cloud_provider_facebook
import dorai.composeapp.generated.resources.settings_dorai_cloud_provider_github
import dorai.composeapp.generated.resources.settings_dorai_cloud_provider_google
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_in
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_out
import dorai.composeapp.generated.resources.settings_dorai_cloud_signed_in_as
import dorai.composeapp.generated.resources.settings_dorai_cloud_social_title
import dorai.composeapp.generated.resources.settings_dorai_cloud_title
import dorai.composeapp.generated.resources.settings_dorai_cloud_usage
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private enum class CloudSocialProvider(val id: String) {
    Google("google"),
    Facebook("facebook"),
    GitHub("github"),
}

private val GoogleBlue = Color(0xFF4285F4)
private val FacebookBlue = Color(0xFF1877F2)
private val GitHubBlack = Color(0xFF24292F)

/**
 * Settings card for the first-party Dorai Cloud account.
 *
 * Signing in stores a Supabase session in encrypted settings; the façade then
 * forwards that session to Supabase, so RLS is the only isolation boundary and
 * the app never holds a service-role key.
 *
 * Social sign-in uses OAuth with PKCE: the device generates the code verifier,
 * opens the provider's consent screen in the system browser, and redeems the
 * authorization code that comes back through the `dorai://cloud/auth` deep link.
 */
@Composable
fun DoraiCloudSection(modifier: Modifier = Modifier) {
    val appSettings: AppSettings = koinInject()
    val inspectionMode = LocalInspectionMode.current
    // Screenshot tests run without a DataRepository in their Koin graph, so the
    // OAuth callback listener is only wired in real app composition.
    val dataRepository: DataRepository? = if (inspectionMode) null else koinInject()
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

    val oauthFailedMessage = stringResource(Res.string.settings_dorai_cloud_oauth_failed)

    fun startSocialSignIn(providerId: String) {
        isBusy = true
        errorMessage = null
        authClient.socialSignInStart(
            provider = providerId,
            redirectTo = DeepLinks.CLOUD_AUTH_RETURN_URI,
        ).onSuccess { start ->
            appSettings.setDoraiCloudOAuthPending(
                DoraiCloudOAuthPending(
                    codeVerifier = start.codeVerifier,
                    state = start.state,
                    redirectTo = DeepLinks.CLOUD_AUTH_RETURN_URI,
                ),
            )
            if (!openUrl(start.url)) {
                errorMessage = oauthFailedMessage
            }
        }.onFailure { errorMessage = it.message }
        isBusy = false
    }

    val pendingAuthUriState = if (dataRepository != null) {
        dataRepository.pendingCloudAuthUri.collectAsState()
    } else {
        remember { mutableStateOf(null) }
    }
    val pendingAuthUri = pendingAuthUriState.value

    LaunchedEffect(pendingAuthUri) {
        val uri = pendingAuthUri ?: return@LaunchedEffect
        dataRepository?.consumeCloudAuthCallback()
        val parsed = Url(uri)
        val code = parsed.parameters["code"]
        val returnedState = parsed.parameters["state"]
        val providerError = parsed.parameters["error_description"] ?: parsed.parameters["error"]
        val pending = appSettings.getDoraiCloudOAuthPending()
        appSettings.setDoraiCloudOAuthPending(null)

        if (providerError != null) {
            errorMessage = providerError
            return@LaunchedEffect
        }
        if (code.isNullOrBlank() || pending == null) {
            errorMessage = null
            return@LaunchedEffect
        }
        if (pending.state.isNotBlank() && returnedState != null && returnedState != pending.state) {
            errorMessage = null
            return@LaunchedEffect
        }
        isBusy = true
        authClient.exchangePkce(code, pending.codeVerifier, pending.redirectTo)
            .onSuccess { signedIn ->
                appSettings.setDoraiCloudSession(signedIn)
                session = signedIn
                refreshUsage(signedIn)
            }
            .onFailure { errorMessage = it.message }
        isBusy = false
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

            val activeSession = session
            if (activeSession == null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.settings_dorai_cloud_social_title),
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SocialProviderButton(
                        monogram = "G",
                        background = GoogleBlue,
                        label = stringResource(Res.string.settings_dorai_cloud_provider_google),
                        enabled = !isBusy,
                        onClick = { startSocialSignIn(CloudSocialProvider.Google.id) },
                    )
                    SocialProviderButton(
                        monogram = "f",
                        background = FacebookBlue,
                        label = stringResource(Res.string.settings_dorai_cloud_provider_facebook),
                        enabled = !isBusy,
                        onClick = { startSocialSignIn(CloudSocialProvider.Facebook.id) },
                    )
                    SocialProviderButton(
                        monogram = "GH",
                        background = GitHubBlack,
                        label = stringResource(Res.string.settings_dorai_cloud_provider_github),
                        enabled = !isBusy,
                        onClick = { startSocialSignIn(CloudSocialProvider.GitHub.id) },
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.settings_dorai_cloud_or_email),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Spacer(Modifier.height(8.dp))

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
                Spacer(Modifier.height(12.dp))
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

@Composable
private fun SocialProviderButton(
    monogram: String,
    background: Color,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) background else background.copy(alpha = 0.4f))
            .border(1.dp, LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.25f), CircleShape)
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, onClick = onClick)
            .handCursor(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogram,
            style = DoraiTypography.titleMedium,
            color = Color.White,
        )
    }
}
