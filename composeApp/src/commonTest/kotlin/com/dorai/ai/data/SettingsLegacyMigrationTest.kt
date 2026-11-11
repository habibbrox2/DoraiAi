package com.dorai.ai.data

import com.dorai.ai.data.AppSettings.Companion.KEY_EMAIL_ACCOUNTS
import com.dorai.ai.data.AppSettings.Companion.KEY_ENCRYPTION_KEY
import com.dorai.ai.data.AppSettings.Companion.KEY_MIGRATION_COMPLETE
import com.dorai.ai.data.AppSettings.Companion.KEY_SPLINTERLANDS_ACCOUNT
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression coverage for the legacy → encrypted settings migration.
 *
 * The migration must be *verify-then-clear*: plaintext legacy secrets are only
 * removed from the legacy store after the encrypted destination is confirmed to
 * hold every migrated value, and the removal is per-key (never a wholesale
 * `clear()` — on iOS the legacy store may share UserDefaults with the system).
 */
class SettingsLegacyMigrationTest {

    private fun legacyWithSecrets(): MapSettings = MapSettings().apply {
        putString(Service.OpenAI.apiKeyKey, "sk-openai-legacy")
        putString(Service.OpenAI.modelIdKey, "gpt-4o")
        putString(Service.OpenAICompatible.baseUrlKey, "https://gateway.example.com/v1")
        putString(KEY_ENCRYPTION_KEY, "enc-key-legacy")
        putInt(AppSettings.KEY_APP_OPENS, 42)
    }

    @Test
    fun `legacy secrets are copied verified then removed from the plaintext store`() {
        val legacy = legacyWithSecrets()
        val encryptedStore = MapSettings()
        val appSettings = AppSettings(encryptedStore)

        appSettings.migrateFromLegacyIfNeeded(legacy)

        // Destination holds every migrated value.
        assertEquals("sk-openai-legacy", encryptedStore.getString(Service.OpenAI.apiKeyKey, ""))
        assertEquals("gpt-4o", encryptedStore.getString(Service.OpenAI.modelIdKey, ""))
        assertEquals("https://gateway.example.com/v1", encryptedStore.getString(Service.OpenAICompatible.baseUrlKey, ""))
        assertEquals("enc-key-legacy", encryptedStore.getString(KEY_ENCRYPTION_KEY, ""))
        assertEquals(42, encryptedStore.getInt(AppSettings.KEY_APP_OPENS, 0))

        // Legacy plaintext originals are gone, per-key — the store itself survives.
        assertFalse(legacy.hasKey(Service.OpenAI.apiKeyKey))
        assertFalse(legacy.hasKey(Service.OpenAI.modelIdKey))
        assertFalse(legacy.hasKey(KEY_ENCRYPTION_KEY))
        assertFalse(legacy.hasKey(AppSettings.KEY_APP_OPENS))
        assertTrue(encryptedStore.getBoolean(KEY_MIGRATION_COMPLETE, false))
    }

    @Test
    fun `destination values win and the legacy copy is still cleared`() {
        val legacy = legacyWithSecrets()
        val encryptedStore = MapSettings()
        encryptedStore.putString(Service.OpenAI.apiKeyKey, "sk-already-migrated")
        val appSettings = AppSettings(encryptedStore)

        appSettings.migrateFromLegacyIfNeeded(legacy)

        assertEquals("sk-already-migrated", encryptedStore.getString(Service.OpenAI.apiKeyKey, ""))
        assertFalse(legacy.hasKey(Service.OpenAI.apiKeyKey))
    }

    @Test
    fun `migration runs only once`() {
        val legacy = legacyWithSecrets()
        val encryptedStore = MapSettings()
        val appSettings = AppSettings(encryptedStore)
        appSettings.migrateFromLegacyIfNeeded(legacy)

        // A second run (e.g. after restore wrote new legacy values) must not touch anything.
        legacy.putString(Service.OpenAI.apiKeyKey, "sk-second-run")
        appSettings.migrateFromLegacyIfNeeded(legacy)

        assertEquals("sk-openai-legacy", encryptedStore.getString(Service.OpenAI.apiKeyKey, ""))
    }

    @Test
    fun `account-scoped secret keys are discovered from account json`() {
        val legacy = MapSettings().apply {
            putString(KEY_EMAIL_ACCOUNTS, """[{"id":"user@example.com","host":"imap.example.com"}]""")
            putString("email_password_user@example.com", "mail-secret")
            putString(KEY_SPLINTERLANDS_ACCOUNT, """[{"id":"alice"}]""")
            putString("splinterlands_posting_key_alice", "posting-secret")
        }
        val encryptedStore = MapSettings()
        val appSettings = AppSettings(encryptedStore)

        appSettings.migrateFromLegacyIfNeeded(legacy)

        assertEquals("mail-secret", encryptedStore.getString("email_password_user@example.com", ""))
        assertEquals("posting-secret", encryptedStore.getString("splinterlands_posting_key_alice", ""))
        assertFalse(legacy.hasKey("email_password_user@example.com"))
        assertFalse(legacy.hasKey("splinterlands_posting_key_alice"))
    }

    @Test
    fun `migration is skipped when no legacy store exists`() {
        val store = MapSettings()
        val appSettings = AppSettings(store)
        // Must not throw and must not mark migration complete (nothing was migrated).
        appSettings.migrateFromLegacyIfNeeded(null)
        assertFalse(store.getBoolean(KEY_MIGRATION_COMPLETE, false))
    }

    @Test
    fun `typed account setters still clear the plaintext copy when the destination already has a value`() {
        // The account JSON and its scoped secrets are migrated through typed setters
        // (setEmailAccountsJson / setEmailPassword / setSplinterlandsPostingKey). Even when
        // the encrypted store already holds a newer value, the plaintext legacy copy must
        // still be removed so secrets don't survive unencrypted on disk.
        val legacy = MapSettings().apply {
            putString(KEY_EMAIL_ACCOUNTS, """[{"id":"user@example.com","host":"imap.example.com"}]""")
            putString("email_password_user@example.com", "mail-secret")
            putString(KEY_SPLINTERLANDS_ACCOUNT, """[{"id":"alice"}]""")
            putString("splinterlands_posting_key_alice", "posting-secret")
        }
        val encryptedStore = MapSettings()
        encryptedStore.putString(KEY_EMAIL_ACCOUNTS, """[{"id":"user@example.com","host":"imap.new.example.com"}]""")
        val appSettings = AppSettings(encryptedStore)

        appSettings.migrateFromLegacyIfNeeded(legacy)

        // Destination keeps the newer value.
        assertEquals(
            """[{"id":"user@example.com","host":"imap.new.example.com"}]""",
            encryptedStore.getString(KEY_EMAIL_ACCOUNTS, ""),
        )
        // Plaintext copies are gone regardless.
        assertFalse(legacy.hasKey(KEY_EMAIL_ACCOUNTS))
        assertFalse(legacy.hasKey("email_password_user@example.com"))
        assertFalse(legacy.hasKey(KEY_SPLINTERLANDS_ACCOUNT))
        assertFalse(legacy.hasKey("splinterlands_posting_key_alice"))
    }
}
