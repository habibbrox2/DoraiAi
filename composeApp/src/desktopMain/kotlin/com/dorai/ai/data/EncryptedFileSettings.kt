package com.dorai.ai.data

import com.dorai.ai.getAppFilesDirectory
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val SETTINGS_FILE = "settings.aes"
private const val KEY_FILE = "settings.key"
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 128

/**
 * AES-256-GCM encrypted file-backed Settings for desktop.
 * Replaces Java Preferences which has an 8KB per-value limit.
 *
 * Stores call into this from many coroutines at once, so every mutation and its [persist] run
 * under one lock (reads stay lock-free on the concurrent map). The file is replaced atomically,
 * and a file that fails to decrypt is set aside rather than overwritten — it holds every API key.
 *
 * [settingsFileName]/[keyFileName] default to the app-wide `settings.aes`/`settings.key` pair;
 * callers with a dedicated secret store (e.g. the GitHub token vault) pass their own file names.
 * [migrateFromLegacyPreferences] must be false for dedicated vault files so they never drain the
 * shared legacy Preferences node.
 */
class EncryptedFileSettings(
    private val settingsFileName: String = SETTINGS_FILE,
    private val keyFileName: String = KEY_FILE,
    private val migrateFromLegacyPreferences: Boolean = true,
) : Settings {

    private val json = Json { encodeDefaults = true }
    private val map: MutableMap<String, String> = ConcurrentHashMap()
    private val writeLock = Any()
    private val key: SecretKey by lazy { getOrCreateKey() }

    init {
        load()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyFile = File(getAppFilesDirectory(), keyFileName)
        if (keyFile.exists()) {
            val bytes = keyFile.readBytes()
            // New format: OS-protected blob (DPAPI on Windows). Legacy format: raw 32 bytes.
            if (DesktopKeyProtector.isProtectedBlob(bytes)) {
                runCatching { DesktopKeyProtector.unprotect(bytes) }.getOrNull()?.let { raw ->
                    restrictToOwner(keyFile)
                    return SecretKeySpec(raw, "AES")
                }
                // Can't decrypt (different user/machine) — keep the old blob aside, like an
                // unreadable settings file.
                keyFile.renameTo(File(keyFile.parentFile, "$keyFileName.unreadable-${System.currentTimeMillis()}"))
            } else if (bytes.size == 32) {
                restrictToOwner(keyFile)
                // Transparently migrate the legacy raw key to the protected format.
                runCatching { writeAtomically(keyFile, DesktopKeyProtector.protect(bytes)) }
                return SecretKeySpec(bytes, "AES")
            } else {
                keyFile.renameTo(File(keyFile.parentFile, "$keyFileName.unreadable-${System.currentTimeMillis()}"))
            }
        }
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        writeAtomically(keyFile, DesktopKeyProtector.protect(keyBytes))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun load() {
        val file = File(getAppFilesDirectory(), settingsFileName)
        if (!file.exists()) {
            if (migrateFromLegacyPreferences) migrateFromPreferences()
            return
        }
        try {
            val encrypted = file.readBytes()
            require(encrypted.size >= GCM_IV_LENGTH) { "settings file truncated" }
            val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decrypted = cipher.doFinal(ciphertext).decodeToString()
            val loaded: Map<String, String> = json.decodeFromString(decrypted)
            map.putAll(loaded)
        } catch (e: Exception) {
            // Unreadable (wrong/missing key, truncated, tampered). Start fresh, but move the file
            // aside first so the next write can't destroy the only copy of the user's settings.
            val backup = File(file.parentFile, "$settingsFileName.unreadable-${System.currentTimeMillis()}")
            println("EncryptedFileSettings: cannot read settings (${e.message}); kept as ${backup.name}")
            file.renameTo(backup)
        }
    }

    private fun migrateFromPreferences() {
        try {
            val prefs = java.util.prefs.Preferences.userRoot().node("com.dorai.ai")
            for (key in prefs.keys()) {
                map[key] = prefs.get(key, "")
            }
            if (map.isNotEmpty()) {
                persist()
                prefs.clear()
                prefs.flush()
            }
        } catch (_: Exception) {
            // Migration is best-effort
        }
    }

    /** Callers hold [writeLock] (or are still in init), so snapshots are written in order. */
    private fun persist() {
        val plaintext = json.encodeToString(map.toMap())
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encrypted = cipher.doFinal(plaintext.encodeToByteArray())
        writeAtomically(File(getAppFilesDirectory(), settingsFileName), iv + encrypted)
    }

    /** Writes to a sibling temp file and renames it over [target], so a crash mid-write never leaves a truncated file. */
    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeBytes(bytes)
        restrictToOwner(tmp)
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun restrictToOwner(file: File) {
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)
    }

    private inline fun mutate(block: () -> Unit) = synchronized(writeLock) {
        block()
        persist()
    }

    override val keys: Set<String> get() = map.keys.toSet()
    override val size: Int get() = map.size

    override fun clear() = mutate { map.clear() }

    override fun remove(key: String) = mutate { map.remove(key) }

    override fun hasKey(key: String): Boolean = key in map

    override fun putInt(key: String, value: Int) = mutate { map[key] = value.toString() }
    override fun getInt(key: String, defaultValue: Int): Int = map[key]?.toIntOrNull() ?: defaultValue
    override fun getIntOrNull(key: String): Int? = map[key]?.toIntOrNull()

    override fun putLong(key: String, value: Long) = mutate { map[key] = value.toString() }
    override fun getLong(key: String, defaultValue: Long): Long = map[key]?.toLongOrNull() ?: defaultValue
    override fun getLongOrNull(key: String): Long? = map[key]?.toLongOrNull()

    override fun putString(key: String, value: String) = mutate { map[key] = value }
    override fun getString(key: String, defaultValue: String): String = map[key] ?: defaultValue
    override fun getStringOrNull(key: String): String? = map[key]

    override fun putFloat(key: String, value: Float) = mutate { map[key] = value.toString() }
    override fun getFloat(key: String, defaultValue: Float): Float = map[key]?.toFloatOrNull() ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = map[key]?.toFloatOrNull()

    override fun putDouble(key: String, value: Double) = mutate { map[key] = value.toString() }
    override fun getDouble(key: String, defaultValue: Double): Double = map[key]?.toDoubleOrNull() ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = map[key]?.toDoubleOrNull()

    override fun putBoolean(key: String, value: Boolean) = mutate { map[key] = value.toString() }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key]?.toBooleanStrictOrNull() ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = map[key]?.toBooleanStrictOrNull()
}
