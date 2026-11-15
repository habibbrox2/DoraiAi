package com.dorai.ai.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A signed-in Dorai Cloud session (Supabase Auth tokens). */
data class DoraiCloudSession(
    val userId: String,
    val email: String?,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long,
) {
    fun isExpired(nowEpochSeconds: Long = epochSecondsNow(), leewaySeconds: Long = 60): Boolean = expiresAtEpochSeconds - leewaySeconds <= nowEpochSeconds
}

internal fun epochSecondsNow(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000

@Serializable
private data class AuthTokenResponseDto(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val expires_at: Long? = null,
    val user: AuthUserDto? = null,
)

@Serializable
private data class AuthUserDto(
    val id: String,
    val email: String? = null,
)

@Serializable
private data class PasswordGrantRequestDto(val email: String, val password: String)

@Serializable
private data class RefreshGrantRequestDto(val refresh_token: String)

/**
 * Minimal Supabase Auth client (password grant + refresh + sign-out).
 *
 * The publishable key is a public client credential; the access/refresh tokens
 * are the only secrets and they live in the app's encrypted settings.
 */
class DoraiAuthClient(
    private val httpClient: HttpClient,
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    private val root: String get() = supabaseUrl.trimEnd('/')

    suspend fun signInWithPassword(email: String, password: String): Result<DoraiCloudSession> = grant(
        "$root/auth/v1/token?grant_type=password",
        json.encodeToString(
            PasswordGrantRequestDto.serializer(),
            PasswordGrantRequestDto(email.trim(), password),
        ),
    )

    suspend fun refresh(refreshToken: String): Result<DoraiCloudSession> = grant(
        "$root/auth/v1/token?grant_type=refresh_token",
        json.encodeToString(RefreshGrantRequestDto.serializer(), RefreshGrantRequestDto(refreshToken)),
    )

    suspend fun signOut(accessToken: String): Result<Unit> = runCatching {
        val response = httpClient.post("$root/auth/v1/logout") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            throw IllegalStateException("Sign-out failed [${response.status.value}]: ${text.take(200)}")
        }
    }

    private suspend inline fun grant(
        path: String,
        body: String,
    ): Result<DoraiCloudSession> = runCatching {
        val response = httpClient.post(path) {
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Authentication failed [${response.status.value}]: ${text.take(200)}")
        }
        val dto = json.decodeFromString(AuthTokenResponseDto.serializer(), text)
        val expiresAt = dto.expires_at
            ?: (epochSecondsNow() + (dto.expires_in ?: 3600L))
        DoraiCloudSession(
            userId = dto.user?.id ?: "",
            email = dto.user?.email,
            accessToken = dto.access_token,
            refreshToken = dto.refresh_token,
            expiresAtEpochSeconds = expiresAt,
        )
    }
}
