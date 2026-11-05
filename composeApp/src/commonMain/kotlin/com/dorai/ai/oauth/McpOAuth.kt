package com.dorai.ai.oauth

import com.dorai.ai.httpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

data class OAuthMetadata(
    val issuer: String?,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val registrationEndpoint: String?,
    val revocationEndpoint: String?,
    val codeChallengeMethodsSupported: List<String>,
    val scopesSupported: List<String>,
)

data class OAuthClientRegistration(
    val clientId: String,
    val clientSecret: String?,
)

data class OAuthTokenResult(
    val accessToken: String,
    val refreshToken: String?,
    val tokenType: String?,
    val expiresIn: Long?,
    val scope: String?,
)

class McpOAuthException(message: String) : Exception(message)

object McpOAuth {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val client = httpClient {}

    private const val VERIFIER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    fun generateCodeVerifier(length: Int = 64): String {
        val sb = StringBuilder(length)
        repeat(length) { sb.append(VERIFIER_CHARS[Random.nextInt(VERIFIER_CHARS.length)]) }
        return sb.toString()
    }

    fun codeChallengeS256(verifier: String): String {
        val bytes = sha256(verifier.encodeToByteArray())
        return base64UrlEncodeNoPadding(bytes)
    }

    suspend fun discoverMetadata(baseUrl: String): OAuthMetadata {
        val url = baseUrl.trimEnd('/') + "/.well-known/oauth-authorization-server"
        val response = client.get(url)
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw McpOAuthException("OAuth metadata discovery failed: HTTP ${response.status.value}: ${text.take(300)}")
        }
        val obj = json.parseToJsonElement(text) as? JsonObject
            ?: throw McpOAuthException("Invalid OAuth metadata: not a JSON object")
        val authEndpoint = obj["authorization_endpoint"]?.jsonPrimitive?.contentOrNull
            ?: throw McpOAuthException("OAuth metadata missing 'authorization_endpoint'")
        val tokenEndpoint = obj["token_endpoint"]?.jsonPrimitive?.contentOrNull
            ?: throw McpOAuthException("OAuth metadata missing 'token_endpoint'")
        val registrationEndpoint = obj["registration_endpoint"]?.jsonPrimitive?.contentOrNull
        val revocationEndpoint = obj["revocation_endpoint"]?.jsonPrimitive?.contentOrNull
        val methods = obj["code_challenge_methods_supported"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val scopes = obj["scopes_supported"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        return OAuthMetadata(
            issuer = obj["issuer"]?.jsonPrimitive?.contentOrNull,
            authorizationEndpoint = authEndpoint,
            tokenEndpoint = tokenEndpoint,
            registrationEndpoint = registrationEndpoint,
            revocationEndpoint = revocationEndpoint,
            codeChallengeMethodsSupported = methods,
            scopesSupported = scopes,
        )
    }

    suspend fun registerClient(
        registrationEndpoint: String,
        redirectUri: String,
        clientName: String = "DORAI AI",
    ): OAuthClientRegistration {
        val body = """{"client_name":"$clientName","redirect_uris":["$redirectUri"],"grant_types":["authorization_code","refresh_token"],"response_types":["code"],"token_endpoint_auth_method":"none"}"""
        val response = client.post(registrationEndpoint) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw McpOAuthException("Dynamic client registration failed: HTTP ${response.status.value}: ${text.take(400)}")
        }
        val obj = json.parseToJsonElement(text) as? JsonObject
            ?: throw McpOAuthException("Invalid registration response")
        val clientId = obj["client_id"]?.jsonPrimitive?.contentOrNull
            ?: throw McpOAuthException("Registration response missing 'client_id'")
        val clientSecret = obj["client_secret"]?.jsonPrimitive?.contentOrNull
        return OAuthClientRegistration(clientId, clientSecret)
    }

    fun buildAuthorizationUrl(
        metadata: OAuthMetadata,
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
        scope: String? = null,
        state: String? = null,
    ): String {
        val params = mutableListOf(
            "response_type" to "code",
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
        )
        if (!scope.isNullOrBlank()) params.add("scope" to scope)
        if (!state.isNullOrBlank()) params.add("state" to state)
        val query = params.joinToString("&") { (k, v) -> "$k=${urlEncode(v)}" }
        return metadata.authorizationEndpoint + (if (metadata.authorizationEndpoint.contains("?")) "&" else "?") + query
    }

    suspend fun exchangeCodeForToken(
        tokenEndpoint: String,
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String,
        clientSecret: String? = null,
    ): OAuthTokenResult {
        val params = mutableListOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri,
            "client_id" to clientId,
            "code_verifier" to codeVerifier,
        )
        if (!clientSecret.isNullOrBlank()) params.add("client_secret" to clientSecret)
        val body = params.joinToString("&") { (k, v) -> "$k=${urlEncode(v)}" }
        val response = client.post(tokenEndpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw McpOAuthException("Token exchange failed: HTTP ${response.status.value}: ${text.take(400)}")
        }
        val obj = json.parseToJsonElement(text) as? JsonObject
            ?: throw McpOAuthException("Invalid token response")
        val accessToken = obj["access_token"]?.jsonPrimitive?.contentOrNull
            ?: throw McpOAuthException("Token response missing 'access_token'")
        return OAuthTokenResult(
            accessToken = accessToken,
            refreshToken = obj["refresh_token"]?.jsonPrimitive?.contentOrNull,
            tokenType = obj["token_type"]?.jsonPrimitive?.contentOrNull,
            expiresIn = obj["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
            scope = obj["scope"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun urlEncode(value: String): String {
        val sb = StringBuilder()
        for (ch in value) {
            when {
                ch.isLetterOrDigit() || ch in "-_.~" -> sb.append(ch)

                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) {
                        sb.append('%')
                        sb.append(((b.toInt() ushr 4) and 0x0f).toString(16).uppercase())
                        sb.append((b.toInt() and 0x0f).toString(16).uppercase())
                    }
                }
            }
        }
        return sb.toString()
    }
}
