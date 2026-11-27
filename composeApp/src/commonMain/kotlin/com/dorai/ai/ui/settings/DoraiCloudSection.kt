package com.dorai.ai.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.DeepLinks
import com.dorai.ai.cloud.DoraiCloudClient
import com.dorai.ai.cloud.DoraiCloudOAuthPending
import com.dorai.ai.cloud.DoraiCloudSession
import com.dorai.ai.cloud.DoraiCloudUsageDto
import com.dorai.ai.cloud.DoraiFirebaseAuth
import com.dorai.ai.data.AppSettings
import com.dorai.ai.data.DataRepository
import com.dorai.ai.httpClient
import com.dorai.ai.openUrl
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.facebook_mark
import dorai.composeapp.generated.resources.github_mark
import dorai.composeapp.generated.resources.google_mark
import dorai.composeapp.generated.resources.settings_dorai_cloud_description
import dorai.composeapp.generated.resources.settings_dorai_cloud_email_label
import dorai.composeapp.generated.resources.settings_dorai_cloud_oauth_failed
import dorai.composeapp.generated.resources.settings_dorai_cloud_or_email
import dorai.composeapp.generated.resources.settings_dorai_cloud_password_label
import dorai.composeapp.generated.resources.settings_dorai_cloud_provider_facebook
import dorai.composeapp.generated.resources.settings_dorai_cloud_provider_github
import dorai.composeapp.generated.resources.settings_dorai_cloud_provider_google
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_in
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_in_failed
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_in_with_email
import dorai.composeapp.generated.resources.settings_dorai_cloud_sign_out
import dorai.composeapp.generated.resources.settings_dorai_cloud_signed_in_as
import dorai.composeapp.generated.resources.settings_dorai_cloud_social_title
import dorai.composeapp.generated.resources.settings_dorai_cloud_title
import dorai.composeapp.generated.resources.settings_dorai_cloud_usage
import dorai.composeapp.generated.resources.sp_hide_password
import dorai.composeapp.generated.resources.sp_show_password
import dorai.composeapp.generated.resources.status_checking
import dorai.composeapp.generated.resources.status_connected
import dorai.composeapp.generated.resources.status_needs_attention
import dorai.composeapp.generated.resources.status_not_set_up
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private enum class CloudSocialProvider(val id: String) {
    Google("google"),
    Facebook("facebook"),
    GitHub("github"),
}

/**
 * Settings card for the first-party Dorai Cloud account.
 *
 * Firebase Auth owns the user session. The resulting ID token is forwarded to the
 * Dorai Cloud worker and Supabase; RLS is the only isolation boundary and the app
 * never holds a service-role key.
 *
 * Social sign-in uses the Firebase Auth redirect handler: the app opens the
 * provider's consent screen in the system browser and finishes the flow through
 * the `dorai://cloud/auth` deep link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoraiCloudSection(modifier: Modifier = Modifier) {
    val appSettings: AppSettings = koinInject()
    val inspectionMode = LocalInspectionMode.current
    // Screenshot tests run without a DataRepository in their Koin graph, so the
    // OAuth callback listener is only wired in real app composition.
    val dataRepository: DataRepository? = if (inspectionMode) null else koinInject()
    val scope = rememberCoroutineScope()
    val tokens = LocalDesignColorTokens.current

    var session by remember { mutableStateOf(appSettings.getDoraiCloudSession()) }
    var email by remember { mutableStateOf(session?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(DoraiCloudUsageDto()) }
    var isBusy by remember { mutableStateOf(false) }
    var isRestoringSession by remember { mutableStateOf(true) }
    var isRefreshingUsage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmailSignIn by remember { mutableStateOf(false) }

    val httpClient = remember { httpClient { } }
    val authClient = remember { DoraiFirebaseAuth() }
    val cloudClient = remember {
        DoraiCloudClient(
            httpClient = httpClient,
            baseUrl = appSettings.getDoraiCloudUrl(),
            tokenRefresher = {
                authClient.getSession(forceRefresh = true).getOrNull()?.let { refreshed ->
                    appSettings.setDoraiCloudSession(refreshed)
                    session = refreshed
                    email = refreshed.email.orEmpty()
                    refreshed.accessToken
                }
            },
        )
    }

    val oauthFailedMessage = stringResource(Res.string.settings_dorai_cloud_oauth_failed)
    val signInFailedMessage = stringResource(Res.string.settings_dorai_cloud_sign_in_failed)

    fun refreshUsage(active: DoraiCloudSession) {
        scope.launch {
            isRefreshingUsage = true
            cloudClient.usage(active.accessToken).onSuccess { usage = it.usage }
            isRefreshingUsage = false
        }
    }

    fun signOut() {
        scope.launch {
            isBusy = true
            errorMessage = null
            authClient.signOut()
            appSettings.setDoraiCloudSession(null)
            session = null
            email = ""
            password = ""
            showPassword = false
            usage = DoraiCloudUsageDto()
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        authClient.getSession(forceRefresh = false)
            .onSuccess { refreshed ->
                if (refreshed != null) {
                    appSettings.setDoraiCloudSession(refreshed)
                    session = refreshed
                    email = refreshed.email.orEmpty()
                    refreshUsage(refreshed)
                } else if (session != null && session?.isExpired() == true) {
                    appSettings.setDoraiCloudSession(null)
                    session = null
                }
            }
            .onFailure { errorMessage = signInFailedMessage }
        isRestoringSession = false
    }

    fun startSocialSignIn(providerId: String) {
        isBusy = true
        errorMessage = null
        scope.launch {
            val result = runCatching {
                val authUri = buildFirebaseProviderAuthUri(providerId)
                appSettings.setDoraiCloudOAuthPending(
                    DoraiCloudOAuthPending(
                        codeVerifier = providerId,
                        state = providerId,
                        redirectTo = DeepLinks.CLOUD_AUTH_RETURN_URI,
                    ),
                )
                if (!openUrl(authUri)) error(oauthFailedMessage)
            }
            result.onFailure { errorMessage = oauthFailedMessage }
            isBusy = false
        }
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
        val providerError = parsed.parameters["error_description"] ?: parsed.parameters["error"]
        val pending = appSettings.getDoraiCloudOAuthPending()
        appSettings.setDoraiCloudOAuthPending(null)
        if (providerError != null) {
            errorMessage = oauthFailedMessage
            return@LaunchedEffect
        }
        val fragmentParams = parseUrlFragmentParams(parsed.fragment)
        val idToken = parsed.parameters["id_token"] ?: fragmentParams["id_token"]
        val accessToken = parsed.parameters["access_token"] ?: fragmentParams["access_token"]
        val provider = pending?.state ?: parsed.parameters["provider"]
        if (provider.isNullOrBlank()) return@LaunchedEffect
        isBusy = true
        errorMessage = null
        val signInResult = when (provider) {
            "google" -> {
                if (idToken.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Google sign-in missing id_token"))
                } else {
                    authClient.signInWithGoogle(idToken, accessToken)
                }
            }

            "facebook" -> {
                if (accessToken.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Facebook sign-in missing access_token"))
                } else {
                    authClient.signInWithFacebook(accessToken)
                }
            }

            "github" -> {
                if (accessToken.isNullOrBlank()) {
                    Result.failure(IllegalStateException("GitHub sign-in missing access_token"))
                } else {
                    authClient.signInWithGitHub(accessToken)
                }
            }

            else -> Result.failure(IllegalStateException("Unsupported provider: $provider"))
        }
        signInResult
            .onSuccess { signedIn ->
                appSettings.setDoraiCloudSession(signedIn)
                session = signedIn
                email = signedIn.email.orEmpty()
                refreshUsage(signedIn)
            }
            .onFailure { errorMessage = oauthFailedMessage }
        isBusy = false
    }

    val activeSession = session
    val status = when {
        isBusy || isRestoringSession -> stringResource(Res.string.status_checking) to tokens.warning
        errorMessage != null -> stringResource(Res.string.status_needs_attention) to tokens.error
        activeSession != null -> stringResource(Res.string.status_connected) to tokens.success
        else -> stringResource(Res.string.status_not_set_up) to tokens.textSecondary
    }

    SettingsSectionCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (activeSession != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                contentDescription = null,
                tint = tokens.textPrimary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(SettingsSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_dorai_cloud_title),
                    style = DoraiTypography.titleMedium,
                    color = tokens.textPrimary,
                )
                Text(
                    text = stringResource(Res.string.settings_dorai_cloud_description),
                    style = DoraiTypography.bodySmall,
                    color = tokens.textSecondary,
                )
            }
            AuthStatusChip(label = status.first, color = status.second)
        }

        Spacer(Modifier.height(SettingsSpacing.md))

        if (!errorMessage.isNullOrBlank()) {
            SettingsErrorState(
                message = errorMessage.orEmpty(),
                onRetry = { errorMessage = null },
            )
            Spacer(Modifier.height(SettingsSpacing.sm))
        }

        when {
            isRestoringSession -> {
                SettingsLoadingState(message = stringResource(Res.string.status_checking))
            }

            activeSession == null -> {
                Text(
                    text = stringResource(Res.string.settings_dorai_cloud_social_title),
                    style = DoraiTypography.bodyMedium,
                    color = tokens.textPrimary,
                )
                Spacer(Modifier.height(SettingsSpacing.sm))
                Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sm)) {
                    SocialSignInButton(
                        icon = Res.drawable.google_mark,
                        label = stringResource(Res.string.settings_dorai_cloud_provider_google),
                        enabled = !isBusy,
                        onClick = { startSocialSignIn(CloudSocialProvider.Google.id) },
                    )
                    SocialSignInButton(
                        icon = Res.drawable.facebook_mark,
                        label = stringResource(Res.string.settings_dorai_cloud_provider_facebook),
                        enabled = !isBusy,
                        onClick = { startSocialSignIn(CloudSocialProvider.Facebook.id) },
                    )
                    SocialSignInButton(
                        icon = Res.drawable.github_mark,
                        label = stringResource(Res.string.settings_dorai_cloud_provider_github),
                        enabled = !isBusy,
                        onClick = { startSocialSignIn(CloudSocialProvider.GitHub.id) },
                    )
                }

                Spacer(Modifier.height(SettingsSpacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = tokens.line)
                    Text(
                        text = stringResource(Res.string.settings_dorai_cloud_or_email),
                        style = DoraiTypography.bodySmall,
                        color = tokens.textSecondary,
                        modifier = Modifier.padding(horizontal = SettingsSpacing.sm),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = tokens.line)
                }
                Spacer(Modifier.height(SettingsSpacing.sm))
                OutlinedButton(
                    onClick = { showEmailSignIn = true },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SettingsSpacing.minTouchTarget)
                        .handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_dorai_cloud_sign_in_with_email))
                }
            }

            else -> {
                SettingsStatusBanner(
                    message = stringResource(
                        Res.string.settings_dorai_cloud_signed_in_as,
                        activeSession.email.orElseIfBlank { activeSession.userId },
                    ),
                    severity = SettingsBannerSeverity.Info,
                )
                Spacer(Modifier.height(SettingsSpacing.sm))
                SettingsStatusBanner(
                    message = stringResource(
                        Res.string.settings_dorai_cloud_usage,
                        usage.embedding,
                        usage.memory_write,
                        usage.vault_write,
                    ),
                    severity = if (isRefreshingUsage) SettingsBannerSeverity.Progress else SettingsBannerSeverity.Info,
                )
                Spacer(Modifier.height(SettingsSpacing.md))
                OutlinedButton(
                    onClick = ::signOut,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SettingsSpacing.minTouchTarget)
                        .handCursor(),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(Res.string.settings_dorai_cloud_sign_out))
                    }
                }
            }
        }
    }

    if (showEmailSignIn) {
        ModalBottomSheet(
            onDismissRequest = {
                showEmailSignIn = false
                password = ""
                showPassword = false
                errorMessage = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsSpacing.lg, vertical = SettingsSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SettingsSpacing.md),
            ) {
                Text(
                    text = stringResource(Res.string.settings_dorai_cloud_sign_in_with_email),
                    style = DoraiTypography.titleMedium,
                    color = tokens.textPrimary,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.settings_dorai_cloud_email_label)) },
                    singleLine = true,
                    enabled = !isBusy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(Res.string.settings_dorai_cloud_password_label)) },
                    singleLine = true,
                    enabled = !isBusy,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.defaultMinSize(
                                minWidth = SettingsSpacing.minTouchTarget,
                                minHeight = SettingsSpacing.minTouchTarget,
                            ),
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(
                                    if (showPassword) Res.string.sp_hide_password else Res.string.sp_show_password,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!errorMessage.isNullOrBlank()) {
                    SettingsErrorState(
                        message = errorMessage.orEmpty(),
                        onRetry = { errorMessage = null },
                    )
                }
                Button(
                    onClick = {
                        isBusy = true
                        errorMessage = null
                        scope.launch {
                            authClient.signInWithEmail(email.trim(), password)
                                .onSuccess { signedIn ->
                                    appSettings.setDoraiCloudSession(signedIn)
                                    session = signedIn
                                    email = signedIn.email.orEmpty()
                                    password = ""
                                    showPassword = false
                                    refreshUsage(signedIn)
                                    showEmailSignIn = false
                                }
                                .onFailure { errorMessage = signInFailedMessage }
                            isBusy = false
                        }
                    },
                    enabled = !isBusy && email.contains('@') && password.length >= 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SettingsSpacing.minTouchTarget)
                        .handCursor(),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(Res.string.settings_dorai_cloud_sign_in))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthStatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(SettingsShapes.pill)
            .background(color.copy(alpha = 0.14f), SettingsShapes.pill),
    ) {
        Text(
            text = label,
            style = DoraiTypography.labelSmall,
            color = color,
            modifier = Modifier.padding(
                horizontal = SettingsSpacing.sm,
                vertical = SettingsSpacing.xxs,
            ),
        )
    }
}

@Composable
private fun SocialSignInButton(
    icon: DrawableResource,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingsSpacing.minTouchTarget)
            .handCursor(),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(SettingsSpacing.sm))
        Text(
            text = label,
            style = DoraiTypography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun String?.orElseIfBlank(fallback: () -> String): String = takeUnless { it.isNullOrBlank() } ?: fallback()

private fun parseUrlFragmentParams(fragment: String): Map<String, String> {
    if (fragment.isBlank()) return emptyMap()
    return fragment.split('&').mapNotNull { part ->
        val idx = part.indexOf('=')
        if (idx <= 0) null else part.substring(0, idx) to part.substring(idx + 1)
    }.toMap()
}

private fun buildFirebaseProviderAuthUri(providerId: String): String {
    val provider = when (providerId) {
        "google" -> "google.com"
        "facebook" -> "facebook.com"
        "github" -> "github.com"
        else -> error("Unsupported provider: $providerId")
    }
    val scope = when (providerId) {
        "google" -> "openid%20email%20profile"
        "facebook" -> "email%20public_profile"
        "github" -> "read:user%20user:email"
        else -> "openid%20email%20profile"
    }
    // Uses Firebase Auth handler; provider must be enabled in Firebase Console.
    return "https://dorai-ai-app.firebaseapp.com/__/auth/handler" +
        "?apiKey=${com.dorai.ai.cloud.DORAI_FIREBASE_WEB_API_KEY}" +
        "&appName=%5BDEFAULT%5D" +
        "&authType=signInViaRedirect" +
        "&providerId=$provider" +
        "&scopes=$scope" +
        "&v=10.14.1" +
        "&redirectUrl=${DeepLinks.CLOUD_AUTH_RETURN_URI}"
}
