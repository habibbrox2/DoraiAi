package com.dorai.ai.data.sync

import com.dorai.ai.data.AppSettings
import com.dorai.ai.data.Conversation
import com.dorai.ai.data.ImportSection
import com.dorai.ai.data.exportToJson
import com.dorai.ai.data.importFromJson
import com.dorai.ai.network.dtos.supabase.SupabaseSyncRecordDto
import com.dorai.ai.network.supabase.SupabaseConfig
import com.dorai.ai.network.supabase.SupabaseRestClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

class CloudSyncService(
    private val supabaseRestClient: SupabaseRestClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    companion object {
        const val SYNC_TABLE = "dorai_sync_vault"
    }

    suspend fun backupSettings(
        config: SupabaseConfig,
        passphrase: String,
        deviceId: String,
        appSettings: AppSettings,
        toolIds: List<String> = emptyList(),
        sections: Set<ImportSection> = ImportSection.entries.toSet(),
        conversations: List<Conversation> = emptyList(),
    ): Result<Long> = runCatching {
        require(config.isValid) { "Invalid Supabase configuration." }
        require(deviceId.isNotBlank()) { "Device identifier must not be blank." }

        // 1. Export settings & conversations to JSON
        val exportedJson = appSettings.exportToJson(
            toolIds = toolIds,
            sections = sections,
            conversations = conversations,
        )
        val plaintext = json.encodeToString(JsonObject.serializer(), exportedJson).encodeToByteArray()

        // 2. Derive key from user passphrase and encrypt with AES-GCM
        val key = deriveKeyFromPassphrase(passphrase)
        val now = Clock.System.now().toEpochMilliseconds()
        val envelope = encryptAesGcm(plaintext, key, now)

        // 3. Upsert record to Supabase
        val record = SupabaseSyncRecordDto(
            device_id = deviceId.trim(),
            payload_version = envelope.version,
            iv = envelope.iv,
            ciphertext = envelope.ciphertext,
            checksum = envelope.checksum,
            updated_at = envelope.updatedAt,
        )

        val body = json.encodeToString(SupabaseSyncRecordDto.serializer(), record)
        supabaseRestClient.insertOrUpdate(config, SYNC_TABLE, body).getOrThrow()

        envelope.updatedAt
    }

    suspend fun restoreSettings(
        config: SupabaseConfig,
        passphrase: String,
        deviceId: String,
        appSettings: AppSettings,
        toolIds: List<String> = emptyList(),
        sections: Set<ImportSection> = ImportSection.entries.toSet(),
        replace: Boolean = true,
    ): Result<Int> = runCatching {
        require(config.isValid) { "Invalid Supabase configuration." }
        require(deviceId.isNotBlank()) { "Device identifier must not be blank." }

        // 1. Fetch encrypted record from Supabase
        val responseText = supabaseRestClient.select(
            config = config,
            table = SYNC_TABLE,
            queryParameters = mapOf("device_id" to "eq.${deviceId.trim()}"),
        ).getOrThrow()

        val records = json.decodeFromString<List<SupabaseSyncRecordDto>>(responseText)
        val latestRecord = records.maxByOrNull { it.updated_at }
            ?: throw NoSuchElementException("No sync backup found for device $deviceId.")

        // 2. Decrypt envelope using passphrase
        val key = deriveKeyFromPassphrase(passphrase)
        val envelope = EncryptedSyncEnvelope(
            version = latestRecord.payload_version,
            iv = latestRecord.iv,
            ciphertext = latestRecord.ciphertext,
            checksum = latestRecord.checksum,
            updatedAt = latestRecord.updated_at,
        )

        val decryptedBytes = decryptAesGcm(envelope, key)
        val decryptedJson = json.decodeFromString(JsonObject.serializer(), decryptedBytes.decodeToString())

        // 3. Import into AppSettings
        appSettings.importFromJson(
            json = decryptedJson,
            toolIds = toolIds,
            sections = sections,
            replace = replace,
        )
    }
}
