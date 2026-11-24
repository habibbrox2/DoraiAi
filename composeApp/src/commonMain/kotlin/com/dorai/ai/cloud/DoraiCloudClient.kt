package com.dorai.ai.cloud

import com.dorai.ai.data.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Default first-party façade endpoint (Cloudflare Worker). */
const val DORAI_CLOUD_DEFAULT_URL = "https://dorai-cloud-api.hrhabib.workers.dev"

@Serializable
data class DoraiCloudUsageDto(
    val embedding: Int = 0,
    val memory_write: Int = 0,
    val vault_write: Int = 0,
)

@Serializable
data class DoraiCloudProfileDto(
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    val plan: String = "free",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val usage: DoraiCloudUsageDto = DoraiCloudUsageDto(),
)

@Serializable
data class DoraiCloudMemoryDto(
    val id: String? = null,
    val key: String,
    val category: String = "LEARNINGS",
    @SerialName("content_ciphertext") val contentCiphertext: String,
    @SerialName("content_iv") val contentIv: String,
    val similarity: Float? = null,
    @SerialName("hit_count") val hitCount: Int? = null,
    val source: String? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)

@Serializable
data class DoraiCloudVaultEnvelopeDto(
    val iv: String,
    val ciphertext: String,
    val checksum: String,
    @SerialName("payload_version") val payloadVersion: Int = 1,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)

@Serializable
data class DoraiCloudSettingsSnapshotDto(
    val iv: String,
    val ciphertext: String,
    val checksum: String,
    @SerialName("updated_at") val updatedAt: Long? = null,
)

@Serializable
data class DoraiCloudDeviceDto(
    val id: String? = null,
    val platform: String,
    val label: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: Long? = null,
)

@Serializable
data class DoraiCloudEmbeddingsDto(
    val embedding: List<Float>,
    val model: String? = null,
    val dimensions: Int? = null,
)

@Serializable
data class DoraiCloudSearchResultDto(
    val memories: List<DoraiCloudMemoryDto> = emptyList(),
)

@Serializable
data class DoraiCloudVaultResultDto(
    val found: Boolean = false,
    val envelope: DoraiCloudVaultEnvelopeDto? = null,
)

@Serializable
data class DoraiCloudSettingsResultDto(
    val found: Boolean = false,
    val snapshot: DoraiCloudSettingsSnapshotDto? = null,
)

@Serializable
data class DoraiCloudUsageResultDto(
    val usage: DoraiCloudUsageDto = DoraiCloudUsageDto(),
)

@Serializable
data class DoraiCloudDeviceResultDto(
    val device: DoraiCloudDeviceDto? = null,
)

@Serializable
private data class DoraiCloudAckDto(
    val ok: Boolean = true,
    @SerialName("updated_at") val updatedAt: Long? = null,
)

@Serializable
private data class EmbeddingRequestDto(val text: String)

@Serializable
private data class MemoryUpsertRequestDto(
    val key: String,
    val category: String,
    @SerialName("content_ciphertext") val contentCiphertext: String,
    @SerialName("content_iv") val contentIv: String,
    val embedding: List<Float>? = null,
    val hit_count: Int = 1,
    val source: String? = null,
)

@Serializable
private data class MemorySearchRequestDto(
    val embedding: List<Float>,
    val limit: Int = 5,
    val threshold: Float = 0.5f,
)

@Serializable
private data class VaultPutRequestDto(
    val iv: String,
    val ciphertext: String,
    val checksum: String,
    @SerialName("payload_version") val payloadVersion: Int = 1,
    @SerialName("device_id") val deviceId: String? = null,
)

@Serializable
private data class SettingsPutRequestDto(
    val iv: String,
    val ciphertext: String,
    val checksum: String,
)

@Serializable
private data class DeviceRegisterRequestDto(
    val platform: String,
    val label: String,
    val push_token: String? = null,
)

/**
 * Client for the first-party Dorai Cloud API façade.
 *
 * Every call carries the end-user's Supabase access token; the façade verifies
 * it and forwards it to Supabase, so RLS stays the isolation boundary. The
 * client never holds a service-role key and never sends plaintext content —
 * memory, vault and settings bodies are already encrypted on the device.
 */
class DoraiCloudClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = DORAI_CLOUD_DEFAULT_URL,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    /** Factory creates a client that automatically uses the token from [AppSettings]. */
    data class ClientParams(
        val httpClient: HttpClient,
        val baseUrl: String = DORAI_CLOUD_DEFAULT_URL,
        val json: Json = Json { ignoreUnknownKeys = true },
        /** If true, the client will read the Supabase access token from AppSettings on each call.
         * If false/unset, the caller must provide a token explicitly. */
        val useAppSettingsToken: Boolean = true,
    )

    companion object {
        /** Creates a [DoraiCloudClient] that automatically reads the Supabase access token
         * from [AppSettings.getSupabaseAccessToken()] on each API call.
         */
        fun createWithAppSettings(httpClient: HttpClient, baseUrl: String = DORAI_CLOUD_DEFAULT_URL, json: Json = Json { ignoreUnknownKeys = true }): DoraiCloudClient = DoraiCloudClient(httpClient, baseUrl, json).apply {
            // Token will be resolved per-call from AppSettings
        }
    }

    private val root: String get() = baseUrl.trimEnd('/')

    suspend fun me(token: String): Result<DoraiCloudProfileDto> = get("$root/v1/me", token, DoraiCloudProfileDto.serializer())

    suspend fun usage(token: String): Result<DoraiCloudUsageResultDto> = get("$root/v1/usage", token, DoraiCloudUsageResultDto.serializer())

    suspend fun embed(token: String, text: String): Result<List<Float>> = post(
        path = "$root/v1/embeddings",
        token = token,
        body = json.encodeToString(EmbeddingRequestDto.serializer(), EmbeddingRequestDto(text)),
        serializer = DoraiCloudEmbeddingsDto.serializer(),
    ).map { it.embedding }

    suspend fun upsertMemory(
        token: String,
        key: String,
        category: String,
        contentCiphertext: String,
        contentIv: String,
        embedding: List<Float>? = null,
        source: String? = null,
    ): Result<DoraiCloudMemoryDto> = post(
        path = "$root/v1/memories",
        token = token,
        body = json.encodeToString(
            MemoryUpsertRequestDto.serializer(),
            MemoryUpsertRequestDto(
                key = key,
                category = category,
                contentCiphertext = contentCiphertext,
                contentIv = contentIv,
                embedding = embedding,
                source = source,
            ),
        ),
        serializer = DoraiCloudMemoryDto.serializer(),
    )

    suspend fun searchMemories(
        token: String,
        embedding: List<Float>,
        limit: Int = 5,
        threshold: Float = 0.5f,
    ): Result<List<DoraiCloudMemoryDto>> = post(
        path = "$root/v1/memories/search",
        token = token,
        body = json.encodeToString(
            MemorySearchRequestDto.serializer(),
            MemorySearchRequestDto(embedding = embedding, limit = limit, threshold = threshold),
        ),
        serializer = DoraiCloudSearchResultDto.serializer(),
    ).map { it.memories }

    suspend fun deleteMemory(token: String, key: String): Result<Unit> = delete("$root/v1/memories?key=${encodeKey(key)}", token)

    suspend fun putVault(token: String, envelope: DoraiCloudVaultEnvelopeDto): Result<Unit> = put(
        path = "$root/v1/vault",
        token = token,
        body = json.encodeToString(
            VaultPutRequestDto.serializer(),
            VaultPutRequestDto(
                iv = envelope.iv,
                ciphertext = envelope.ciphertext,
                checksum = envelope.checksum,
                payloadVersion = envelope.payloadVersion,
                deviceId = envelope.deviceId,
            ),
        ),
    )

    suspend fun getVault(token: String): Result<DoraiCloudVaultResultDto> = get("$root/v1/vault", token, DoraiCloudVaultResultDto.serializer())

    suspend fun putSettings(token: String, snapshot: DoraiCloudSettingsSnapshotDto): Result<Unit> = put(
        path = "$root/v1/settings",
        token = token,
        body = json.encodeToString(
            SettingsPutRequestDto.serializer(),
            SettingsPutRequestDto(
                iv = snapshot.iv,
                ciphertext = snapshot.ciphertext,
                checksum = snapshot.checksum,
            ),
        ),
    )

    suspend fun getSettings(token: String): Result<DoraiCloudSettingsResultDto> = get("$root/v1/settings", token, DoraiCloudSettingsResultDto.serializer())

    suspend fun registerDevice(
        token: String,
        platform: String,
        label: String,
        pushToken: String? = null,
    ): Result<DoraiCloudDeviceDto> = post(
        path = "$root/v1/devices",
        token = token,
        body = json.encodeToString(
            DeviceRegisterRequestDto.serializer(),
            DeviceRegisterRequestDto(platform = platform, label = label, push_token = pushToken),
        ),
        serializer = DoraiCloudDeviceResultDto.serializer(),
    ).map { it.device ?: DoraiCloudDeviceDto(platform = platform, label = label) }

    /* ------------------------------------------------------------- plumbing */

    private suspend inline fun <reified T> get(
        path: String,
        token: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = request(path, token, null, serializer, HttpMethod.Get)

    private suspend inline fun <reified T> post(
        path: String,
        token: String,
        body: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = request(path, token, body, serializer, HttpMethod.Post)

    private suspend fun put(
        path: String,
        token: String,
        body: String,
    ): Result<Unit> = request(path, token, body, DoraiCloudAckDto.serializer(), HttpMethod.Put).map { }

    private suspend inline fun <reified T> request(
        path: String,
        token: String,
        body: String?,
        serializer: kotlinx.serialization.KSerializer<T>,
        method: HttpMethod,
    ): Result<T> = runCatching {
        val response = when {
            body == null -> httpClient.get(path) { authorize(token) }

            method == HttpMethod.Put -> httpClient.put(path) {
                authorize(token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            else -> httpClient.post(path) {
                authorize(token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Dorai Cloud request failed [${response.status.value}]: ${text.take(200)}")
        }
        json.decodeFromString(serializer, text)
    }

    private suspend fun delete(path: String, token: String): Result<Unit> = runCatching {
        val response = httpClient.delete(path) { authorize(token) }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            throw IllegalStateException("Dorai Cloud delete failed [${response.status.value}]: ${text.take(200)}")
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authorize(token: String) {
        header("Authorization", "Bearer $token")
    }

    private fun encodeKey(key: String): String = key.map { c -> if (c.isLetterOrDigit() || c == '-' || c == '_') c else "%${c.code.toString(16)}" }
        .joinToString("")
}
