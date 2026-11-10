package com.dorai.ai.github

import android.content.Context
import android.content.SharedPreferences
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject

// Dedicated vault file — deliberately separate from the provider `kai_secure_prefs` file so a
// GitHub vault decrypt failure can wipe only GitHub state and never provider credentials.
private const val VAULT_FILE = "dorai_github_vault_v1"

private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
private const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
private const val KEY_LOGIN = "login"
private const val KEY_INSTALLATION_IDS = "installation_ids"

actual fun createGitHubTokenVault(): GitHubTokenVault = AndroidGitHubTokenVault()

private class AndroidGitHubTokenVault : GitHubTokenVault {

    private val context: Context by inject(Context::class.java)

    // Transient cache only — cleared whenever the vault file is deleted so a fresh
    // EncryptedSharedPreferences instance is built against the recreated file.
    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun prefs(): SharedPreferences = cachedPrefs ?: createVaultPrefs(context).also { cachedPrefs = it }

    private fun createVaultPrefs(context: Context): SharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (_: Exception) {
        // AEADBadTagException occurs when Android Auto Backup restores the encrypted
        // prefs file but the Keystore key is hardware-bound and doesn't transfer.
        // Delete only the vault file and recreate it fresh.
        context.deleteSharedPreferences(VAULT_FILE)
        createEncryptedPrefs(context)
    }

    private fun resetVault() {
        cachedPrefs = null
        runCatching { context.deleteSharedPreferences(VAULT_FILE) }
    }

    override fun readAccessToken(): String? = readString(KEY_ACCESS_TOKEN)
    override fun readRefreshToken(): String? = readString(KEY_REFRESH_TOKEN)
    override fun readLogin(): String? = readString(KEY_LOGIN)

    private fun readString(key: String): String? = try {
        prefs().getString(key, null)
    } catch (_: Exception) {
        // Decrypt failure on first read (corrupted/restored file). Drop only the vault file.
        resetVault()
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
            writeVault(accessToken, refreshToken, expiresAtEpochMs, refreshExpiresAtEpochMs, login, installationIds)
        } catch (_: Exception) {
            resetVault()
            try {
                writeVault(accessToken, refreshToken, expiresAtEpochMs, refreshExpiresAtEpochMs, login, installationIds)
            } catch (e: Exception) {
                throw IllegalStateException("Unable to write GitHub token vault", e)
            }
        }
    }

    private fun writeVault(
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochMs: Long?,
        refreshExpiresAtEpochMs: Long?,
        login: String,
        installationIds: List<Long>,
    ) {
        prefs().edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken) else remove(KEY_REFRESH_TOKEN)
            if (expiresAtEpochMs != null) putString(KEY_ACCESS_EXPIRES_AT, expiresAtEpochMs.toString()) else remove(KEY_ACCESS_EXPIRES_AT)
            if (refreshExpiresAtEpochMs != null) putString(KEY_REFRESH_EXPIRES_AT, refreshExpiresAtEpochMs.toString()) else remove(KEY_REFRESH_EXPIRES_AT)
            putString(KEY_LOGIN, login)
            putString(KEY_INSTALLATION_IDS, Json.encodeToString(installationIds))
            apply()
        }
    }

    override fun clear() {
        try {
            prefs().edit().clear().apply()
        } catch (_: Exception) {
            // Unreadable vault file — deleting it also removes all token state.
            resetVault()
        }
    }
}

private fun createEncryptedPrefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        VAULT_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
