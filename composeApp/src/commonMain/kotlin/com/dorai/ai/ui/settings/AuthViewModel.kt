package com.dorai.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dorai.ai.cloud.DORAI_CLOUD_SUPABASE_PUBLISHABLE_KEY
import com.dorai.ai.cloud.DORAI_CLOUD_SUPABASE_URL
import com.dorai.ai.cloud.DoraiAuthClient
import com.dorai.ai.data.AppSettings
import com.dorai.ai.httpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication state backed by shared preferences, with real Supabase Auth calls.
 *
 * The app is usable without signing in (auth is optional). When a user does sign in,
 * [signIn] stores the [com.dorai.ai.cloud.DoraiCloudSession] through [AppSettings] so
 * the Dorai Cloud client, the settings UI and the legacy auth fields all see one
 * consistent session that survives restarts.
 *
 * An [authClientOverride] can be supplied by tests (e.g. a Ktor MockEngine client);
 * production builds create the client lazily from the stored Supabase configuration.
 */
class AuthViewModel(
    private val settings: AppSettings,
    private val authClientOverride: DoraiAuthClient? = null,
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(settings.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(settings.getUserEmail())
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userDisplayName = MutableStateFlow(settings.getUserDisplayName())
    val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private fun authClient(): DoraiAuthClient = authClientOverride ?: DoraiAuthClient(
        httpClient = httpClient(),
        supabaseUrl = settings.getSupabaseUrl().ifBlank { DORAI_CLOUD_SUPABASE_URL },
        publishableKey = settings.getSupabaseAnonKey().ifBlank { DORAI_CLOUD_SUPABASE_PUBLISHABLE_KEY },
    )

    /** True when a persisted session exists and its access token has expired. */
    fun isSessionExpired(): Boolean = settings.getDoraiCloudSession()?.isExpired() ?: false

    /** Signs in with Supabase and persists the resulting session. */
    suspend fun signIn(email: String, password: String, displayName: String): Result<Unit> {
        _isBusy.value = true
        return try {
            val session = authClient().signInWithPassword(email, password).getOrThrow()
            applySession(session, email, displayName)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            _isBusy.value = false
        }
    }

    /** Refreshes an expired session using its persisted refresh token. */
    suspend fun refreshSession(): Result<Unit> {
        val refreshToken = settings.getDoraiCloudSession()?.refreshToken
        if (refreshToken.isNullOrBlank()) return Result.failure(IllegalStateException("No refresh token"))
        _isBusy.value = true
        return try {
            val session = authClient().refresh(refreshToken).getOrThrow()
            applySession(session, settings.getUserEmail(), settings.getUserDisplayName())
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            _isBusy.value = false
        }
    }

    /**
     * Signs out remotely, then clears the local session even when the network call
     * fails — an offline user must still be able to leave the account.
     */
    suspend fun signOut(): Result<Unit> {
        _isBusy.value = true
        val token = settings.getSupabaseAccessToken()
        val remote = if (token.isBlank()) Result.success(Unit) else authClient().signOut(token)
        clearSession()
        _isBusy.value = false
        return remote
    }

    /** Persists an edited display name (the email itself is read-only). */
    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        settings.setUserDisplayName(trimmed)
        _userDisplayName.value = trimmed
    }

    private fun applySession(
        session: com.dorai.ai.cloud.DoraiCloudSession,
        email: String,
        displayName: String,
    ) {
        val resolvedEmail = session.email?.ifBlank { email } ?: email
        val resolvedName = displayName.trim()
        settings.setDoraiCloudSession(session)
        settings.setSupabaseAccessToken(session.accessToken)
        settings.setUserLoggedIn(true)
        settings.setUserId(session.userId)
        settings.setUserEmail(resolvedEmail)
        settings.setUserDisplayName(resolvedName)
        _isLoggedIn.value = true
        _userEmail.value = resolvedEmail
        _userDisplayName.value = resolvedName
    }

    private fun clearSession() {
        settings.setDoraiCloudSession(null)
        settings.setSupabaseAccessToken("")
        settings.setUserLoggedIn(false)
        settings.setUserId("")
        settings.setUserEmail("")
        settings.setUserDisplayName("")
        _isLoggedIn.value = false
        _userEmail.value = ""
        _userDisplayName.value = ""
    }
}
