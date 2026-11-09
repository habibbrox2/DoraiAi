package com.dorai.ai.github

import com.dorai.ai.data.EncryptedFileSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val VAULT_FILE = "github-vault.aes"
private const val VAULT_KEY_FILE = "github-vault.key"

private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
private const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
private const val KEY_LOGIN = "login"
private const val KEY_INSTALLATION_IDS = "installation_ids"

actual fun createGitHubTokenVault(): GitHubTokenVault = DesktopGitHubTokenVault()

private class DesktopGitHubTokenVault : GitHubTokenVault {

    // Dedicated AES-256-GCM encrypted files under ~/.dorai (github-vault.aes + github-vault.key),
    // reusing the EncryptedFileSettings atomic-write pattern with owner-only permissions on both
    // files. Legacy Preferences migration is disabled so the vault never touches shared state.
    // DPAPI migration required before production release — do not ship without OS-bound key.
    private val settings = EncryptedFileSettings(
        settingsFileName = VAULT_FILE,
        keyFileName = VAULT_KEY_FILE,
        migrateFromLegacyPreferences = false,
    )

    override fun readAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS_TOKEN)
    override fun readRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)
    override fun readLogin(): String? = settings.getStringOrNull(KEY_LOGIN)

    override fun store(
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochMs: Long?,
        refreshExpiresAtEpochMs: Long?,
        login: String,
        installationIds: List<Long>,
    ) {
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
    }

    override fun clear() = settings.clear()
}
