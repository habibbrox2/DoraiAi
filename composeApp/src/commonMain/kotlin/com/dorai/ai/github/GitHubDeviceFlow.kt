package com.dorai.ai.github

import com.dorai.ai.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

sealed interface GitHubDeviceFlowState {
    data object Idle : GitHubDeviceFlowState

    data class AwaitingUser(
        val userCode: String,
        val verificationUri: String,
        val expiresIn: Long,
        val interval: Long,
    ) : GitHubDeviceFlowState

    data class Polling(
        val userCode: String,
        val interval: Long,
    ) : GitHubDeviceFlowState

    data class Success(
        val login: String,
        val accessToken: String? = null,
        val refreshToken: String? = null,
    ) : GitHubDeviceFlowState

    data class Error(
        val error: GitHubDeviceFlowError,
    ) : GitHubDeviceFlowState
}

sealed interface GitHubDeviceFlowError {
    data object AuthorizationPending : GitHubDeviceFlowError
    data object SlowDown : GitHubDeviceFlowError
    data object ExpiredToken : GitHubDeviceFlowError
    data object AccessDenied : GitHubDeviceFlowError
    data object DeviceFlowDisabled : GitHubDeviceFlowError
    data object BadRefreshToken : GitHubDeviceFlowError
    data class Network(val message: String) : GitHubDeviceFlowError
    data object MissingClientId : GitHubDeviceFlowError
}

class GitHubDeviceFlowException(val error: GitHubDeviceFlowError) : Exception(error.toString())

open class GitHubDeviceFlow(
    private val clientId: String?,
    private val httpClient: HttpClient = httpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    open suspend fun requestDeviceCode(): Result<GitHubDeviceFlowState.AwaitingUser> {
        val id = clientId ?: return Result.failure(GitHubDeviceFlowException(GitHubDeviceFlowError.MissingClientId))
        return runCatching {
            val response = httpClient.post("https://github.com/login/device/code") {
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("client_id=$id")
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) throw GitHubDeviceFlowException(githubErrorFromBody(body))
            val parsed = json.decodeFromString(DeviceCodeResponse.serializer(), body)
            GitHubDeviceFlowState.AwaitingUser(
                userCode = parsed.userCode,
                verificationUri = parsed.verificationUri,
                expiresIn = parsed.expiresIn,
                interval = parsed.interval,
            )
        }
    }

    open suspend fun pollForToken(
        deviceCode: String,
        intervalSeconds: Long,
        expiresAtEpochMs: Long,
    ): GitHubDeviceFlowState {
        if (Clock.System.now().toEpochMilliseconds() >= expiresAtEpochMs) {
            return GitHubDeviceFlowState.Error(GitHubDeviceFlowError.ExpiredToken)
        }
        val id = clientId ?: return GitHubDeviceFlowState.Error(GitHubDeviceFlowError.MissingClientId)
        return runCatching {
            val response = httpClient.post("https://github.com/login/oauth/access_token") {
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    buildFormBody(
                        "client_id" to id,
                        "device_code" to deviceCode,
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    ),
                )
            }
            val body = response.bodyAsText()
            val token = runCatching { json.decodeFromString(GitHubTokenResponse.serializer(), body) }.getOrNull()
            if (!body.isErrorResponse() && token?.accessToken != null) {
                val login = fetchLogin(token.accessToken)
                return GitHubDeviceFlowState.Success(login, token.accessToken, token.refreshToken)
            }
            val error = githubErrorFromBody(body)
            when (error) {
                GitHubDeviceFlowError.AuthorizationPending -> GitHubDeviceFlowState.Polling(userCode = "", interval = intervalSeconds)

                GitHubDeviceFlowError.SlowDown -> {
                    val slowDownInterval = errorIntervalFromBody(body) ?: (intervalSeconds + 5)
                    GitHubDeviceFlowState.Polling(userCode = "", interval = slowDownInterval)
                }

                else -> GitHubDeviceFlowState.Error(error)
            }
        }.getOrElse { GitHubDeviceFlowState.Error(GitHubDeviceFlowError.Network(it.message ?: "request failed")) }
    }

    open suspend fun refreshToken(refreshToken: String): Result<GitHubTokenResponse> {
        val id = clientId ?: return Result.failure(GitHubDeviceFlowException(GitHubDeviceFlowError.MissingClientId))
        return runCatching {
            val response = httpClient.post("https://github.com/login/oauth/access_token") {
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    buildFormBody(
                        "client_id" to id,
                        "refresh_token" to refreshToken,
                        "grant_type" to "refresh_token",
                    ),
                )
            }
            val body = response.bodyAsText()
            if (body.isErrorResponse()) {
                val error = githubErrorFromBody(body)
                throw GitHubDeviceFlowException(error)
            }
            if (!response.status.isSuccess()) throw GitHubDeviceFlowException(githubErrorFromBody(body))
            json.decodeFromString(GitHubTokenResponse.serializer(), body)
        }
    }

    private suspend fun fetchLogin(accessToken: String): String {
        val response = httpClient.get("https://api.github.com/user") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", GitHubAppConfig.API_VERSION)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw GitHubDeviceFlowException(GitHubDeviceFlowError.Network(body.take(200)))
        val user = json.parseToJsonElement(body).jsonObject
        return user["login"]?.jsonPrimitive?.contentOrNull ?: throw GitHubDeviceFlowException(GitHubDeviceFlowError.Network("missing login"))
    }

    private fun errorIntervalFromBody(body: String): Long? = runCatching {
        (json.parseToJsonElement(body).jsonObject["interval"]?.jsonPrimitive?.contentOrNull)?.toLongOrNull()
    }.getOrNull()

    private fun String.isErrorResponse(): Boolean = runCatching { json.parseToJsonElement(this).jsonObject.containsKey("error") }.getOrDefault(false)

    private fun githubErrorFromBody(body: String): GitHubDeviceFlowError = runCatching {
        val error = json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.contentOrNull
        when (error) {
            "authorization_pending" -> GitHubDeviceFlowError.AuthorizationPending
            "slow_down" -> GitHubDeviceFlowError.SlowDown
            "expired_token" -> GitHubDeviceFlowError.ExpiredToken
            "access_denied" -> GitHubDeviceFlowError.AccessDenied
            "device_flow_disabled" -> GitHubDeviceFlowError.DeviceFlowDisabled
            "bad_refresh_token" -> GitHubDeviceFlowError.BadRefreshToken
            else -> GitHubDeviceFlowError.Network(body.take(200))
        }
    }.getOrDefault(GitHubDeviceFlowError.Network(body.take(200)))

    private fun buildFormBody(vararg parts: Pair<String, String>): String = parts.joinToString("&") {
        val (key, value) = it
        "$key=${urlEncode(value)}"
    }

    private fun urlEncode(value: String): String = buildString {
        for (byte in value.encodeToByteArray()) {
            val char = byte.toInt() and 0xff
            val unreserved = (char in 0x41..0x5a) || (char in 0x61..0x7a) || (char in 0x30..0x39) || char == 0x2d || char == 0x2e || char == 0x5f || char == 0x7e
            if (unreserved) append(char.toChar()) else append('%').append(char.toString(16).uppercase().padStart(2, '0'))
        }
    }
}
