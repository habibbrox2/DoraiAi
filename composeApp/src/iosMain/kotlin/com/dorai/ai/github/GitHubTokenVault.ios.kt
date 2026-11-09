package com.dorai.ai.github

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEYCHAIN_SERVICE = "com.dorai.ai.github.vault"

private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
private const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
private const val KEY_LOGIN = "login"
private const val KEY_INSTALLATION_IDS = "installation_ids"

actual fun createGitHubTokenVault(): GitHubTokenVault = IosGitHubTokenVault()

// KeychainSettings is already on the iOS dependency graph (used by createSecureSettings()),
// so no new dependency is required. Every operation is guarded — a Keychain failure reports
// unsupported/false/null rather than crashing the app.
@OptIn(ExperimentalSettingsImplementation::class)
private class IosGitHubTokenVault : GitHubTokenVault {

    private val settings: Settings by lazy { KeychainSettings(service = KEYCHAIN_SERVICE) }

    override fun readAccessToken(): String? = readString(KEY_ACCESS_TOKEN)
    override fun readRefreshToken(): String? = readString(KEY_REFRESH_TOKEN)
    override fun readLogin(): String? = readString(KEY_LOGIN)

    private fun readString(key: String): String? = try {
        settings.getStringOrNull(key)
    } catch (_: Exception) {
        null
    }

    override fun store(
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochMs: Long?,
        refreshExpiresAtEpochMs: Long?,
        login: String,
        installationIds: List<Long>,
    ) {
        try {
            settings.putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) {
                settings.putString(KEY_REFRESH_TOKEN, refreshToken)
            } else {
                settings.remove(KEY_REFRESH_TOKEN)
            }
            if (expiresAtEpochMs != null) {
                settings.putLong(KEY_ACCESS_EXPIRES_AT, expiresAtEpochMs)
            } else {
                settings.remove(KEY_ACCESS_EXPIRES_AT)
            }
            if (refreshExpiresAtEpochMs != null) {
                settings.putLong(KEY_REFRESH_EXPIRES_AT, refreshExpiresAtEpochMs)
            } else {
                settings.remove(KEY_REFRESH_EXPIRES_AT)
            }
            settings.putString(KEY_LOGIN, login)
            settings.putString(KEY_INSTALLATION_IDS, Json.encodeToString(installationIds))
        } catch (_: Exception) {
            // Keychain unavailable (e.g. missing entitlement) — report unsupported via reads returning null.
        }
    }

    override fun clear() {
        try {
            settings.clear()
        } catch (_: Exception) {
            // Best effort — nothing to clean up if the Keychain is unavailable.
        }
    }

    override fun isConnected(): Boolean = try {
        readAccessToken() != null && readLogin() != null
    } catch (_: Exception) {
        false
    }
}
