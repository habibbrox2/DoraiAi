package com.dorai.ai.source

import com.dorai.ai.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

/**
 * GitHub OAuth 2.0 authorization-code flow for the Workspace integration.
 *
 * Default (preferred) sign-in method. The user is redirected to GitHub in the system
 * browser, authorises DORAI AI, and GitHub redirects back with a code which is exchanged
 * for an access token. Falls through to [GitHubDeviceFlow] when the OAuth client ID is
 * missing or the web flow cannot complete.
 */
open class GitHubOAuthFlow(
    private val clientId: String?,
    private val redirectUri: String,
    private val httpClient: HttpClient = httpClient(),
) {
    /** True only when an OAuth client ID is configured for this build. */
    val isConfigured: Boolean get() = !clientId.isNullOrBlank()

    private val json = Json { ignoreUnknownKeys = true }

    /** URL the user must visit in a browser to grant DORAI AI access. */
    fun authorizationUrl(state: String, scope: String = "read:user repo"): String {
        val clientId = clientId ?: throw IllegalStateException("OAuth client ID not configured")
        return "https://github.com/login/oauth/authorize?" +
            "client_id=$clientId" +
            "&redirect_uri=${urlEncode(redirectUri)}" +
            "&scope=${urlEncode(scope)}" +
            "&state=${urlEncode(state)}"
    }

    /** Exchanges the authorization code for an access token + GitHub login. */
    open suspend fun exchangeCode(code: String): GitHubAuthResult {
        val clientId = clientId ?: return GitHubAuthResult.Error(GitHubAuthError.MissingClientId)
        return runCatching {
            val response = httpClient.post("https://github.com/login/oauth/access_token") {
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    buildFormBody(
                        "client_id" to clientId,
                        "redirect_uri" to redirectUri,
                        "code" to code,
                        "grant_type" to "authorization_code",
                    ),
                )
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) throw GitHubAuthException(githubErrorFromBody(body))
            val token = runCatching { json.decodeFromString(GitHubTokenResponse.serializer(), body) }.getOrNull()
            val hasError = runCatching { json.parseToJsonElement(body).jsonObject.containsKey("error") }.getOrDefault(false)
            if (!hasError && token?.accessToken != null) {
                val login = fetchLogin(token.accessToken)
                GitHubAuthResult.Success(login, token.accessToken, token.refreshToken)
            } else {
                GitHubAuthResult.Error(githubErrorFromBody(body))
            }
        }.getOrElse { GitHubAuthResult.Error(GitHubAuthError.Network(it.message ?: "request failed")) }
    }

    /** Refreshes an expired OAuth access token using the refresh token. */
    suspend fun refreshToken(refreshToken: String): Result<GitHubTokenResponse> {
        val clientId = clientId ?: return Result.failure(GitHubAuthException(GitHubAuthError.MissingClientId))
        return runCatching {
            val response = httpClient.post("https://github.com/login/oauth/access_token") {
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    buildFormBody(
                        "client_id" to clientId,
                        "refresh_token" to refreshToken,
                        "grant_type" to "refresh_token",
                    ),
                )
            }
            val body = response.bodyAsText()
            if (body.contains("\"error\"")) {
                val error = githubErrorFromBody(body)
                throw GitHubAuthException(error)
            }
            if (!response.status.isSuccess()) throw GitHubAuthException(githubErrorFromBody(body))
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
        if (!response.status.isSuccess()) throw GitHubAuthException(GitHubAuthError.Network(body.take(200)))
        val user = json.parseToJsonElement(body).jsonObject
        return user["login"]?.jsonPrimitive?.contentOrNull ?: throw GitHubAuthException(GitHubAuthError.Network("missing login"))
    }

    private fun githubErrorFromBody(body: String): GitHubAuthError = runCatching {
        val error = json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.contentOrNull
        when (error) {
            "access_denied" -> GitHubAuthError.AccessDenied
            "unsupported_response_type" -> GitHubAuthError.Network("unsupported_response_type")
            "invalid_request" -> GitHubAuthError.Network("invalid_request")
            "unauthorized_client" -> GitHubAuthError.Network("unauthorized_client")
            "unsupported_grant_type" -> GitHubAuthError.Network("unsupported_grant_type")
            "invalid_grant" -> GitHubAuthError.Network("invalid_grant")
            else -> GitHubAuthError.Network(body.take(200))
        }
    }.getOrDefault(GitHubAuthError.Network(body.take(200)))

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

/** Result of an OAuth code exchange. */
sealed interface GitHubAuthResult {
    data class Success(val login: String, val accessToken: String, val refreshToken: String?) : GitHubAuthResult
    data class Error(val error: GitHubAuthError) : GitHubAuthResult
}

/** OAuth auth errors. */
sealed interface GitHubAuthError {
    data object AuthorizationPending : GitHubAuthError
    data object AccessDenied : GitHubAuthError
    data object ExpiredToken : GitHubAuthError
    data object BadRefreshToken : GitHubAuthError
    data object MissingClientId : GitHubAuthError
    data class Network(val message: String) : GitHubAuthError
}

class GitHubAuthException(val error: GitHubAuthError) : Exception(error.toString())
