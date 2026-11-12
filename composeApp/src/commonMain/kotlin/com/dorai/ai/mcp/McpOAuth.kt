package com.dorai.ai.mcp

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Internal OAuth plumbing for MCP servers: discovery, registration and the two token grants. */
internal object McpOAuth {
    private val json = Json { ignoreUnknownKeys = true }

    /** RFC 9728 — `{origin}/.well-known/oauth-protected-resource{path}`. */
    fun protectedResourceMetadataUrl(resource: String): String {
        val schemeEnd = resource.indexOf("://")
        val originEnd = resource.indexOf('/', schemeEnd + 3)
        var path = resource.substring(originEnd)
        if (path == "/") path = "" // root maps to well-known without trailing slash
        return resource.substring(0, originEnd) + "/.well-known/oauth-protected-resource" + path
    }

    /** Authorization server metadata document URL (RFC 8414). */
    fun authorizationServerMetadataUrl(issuer: String): String = issuer.trimEnd('/') + "/.well-known/oauth-authorization-server"

    /** Walk a resource URL → protected-resource metadata → authorization-server metadata. */
    suspend fun discover(resource: String, client: HttpClient): OAuthEndpoints {
        val resourceResponse = httpGet(client, protectedResourceMetadataUrl(resource))?.getOrNull()
            ?: throw McpException("OAuth discovery failed for $resource")
        val resourceMeta = runCatching { json.parseToJsonElement(resourceResponse).jsonObject }.getOrElse {
            throw McpException("The protected resource metadata was not valid JSON")
        }
        val authServer = resourceMeta["authorization_servers"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
            ?: throw McpException("The server did not advertise an authorization server for $resource")

        val asUrl = authorizationServerMetadataUrl(authServer)
        val asResponse = httpGet(client, asUrl)?.getOrNull()
            ?: throw McpException("Authorization server metadata was not found: $asUrl")
        val asMeta = runCatching { json.parseToJsonElement(asResponse).jsonObject }.getOrElse {
            throw McpException("The authorization server metadata was not valid JSON")
        }
        val authorizationEndpoint = asMeta["authorization_endpoint"]?.jsonPrimitive?.contentOrNull
        val tokenEndpoint = asMeta["token_endpoint"]?.jsonPrimitive?.contentOrNull
        if (authorizationEndpoint.isNullOrBlank() || tokenEndpoint.isNullOrBlank()) {
            throw McpException("The server's OAuth metadata is missing required endpoints")
        }
        return OAuthEndpoints(
            resource = resourceMeta["resource"]?.jsonPrimitive?.contentOrNull ?: resource,
            authorizationEndpoint = authorizationEndpoint,
            tokenEndpoint = tokenEndpoint,
            registrationEndpoint = asMeta["registration_endpoint"]?.jsonPrimitive?.contentOrNull,
            scopes = resourceMeta["scopes_supported"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
        )
    }

    /** RFC 7591 client registration. Requires no credentials ("none" auth method). */
    suspend fun registerClient(endpoint: String, clientName: String, redirectUri: String, scopes: List<String>, client: HttpClient): String {
        val body = buildJsonObject {
            put("client_name", clientName)
            put("redirect_uris", JsonArray(listOf(JsonPrimitive(redirectUri))))
            put("grant_types", JsonArray(listOf(JsonPrimitive("authorization_code"), JsonPrimitive("refresh_token"))))
            put("token_endpoint_auth_method", "none")
        }.toString()
        val response = httpPost(client, endpoint, body, ContentType.Application.Json)?.getOrNull()
            ?: throw McpException("Dynamic Client Registration was rejected by the server")
        return json.parseToJsonElement(response).jsonObject["client_id"]?.jsonPrimitive?.contentOrNull
            ?: throw McpException("The server did not return a client_id during registration")
    }

    /** Authorize URL for the authorization endpoint. */
    fun buildAuthorizationUrl(
        authorizationEndpoint: String,
        clientId: String,
        redirectUri: String,
        state: String,
        codeChallenge: String,
        scopes: List<String>,
        resource: String,
    ): String {
        val query = buildString {
            fun param(key: String, value: String) {
                if (isNotEmpty()) append('&')
                append(key.encodeURLParameter())
                append('=')
                append(value.encodeURLParameter())
            }
            param("response_type", "code")
            param("client_id", clientId)
            param("redirect_uri", redirectUri)
            param("state", state)
            param("code_challenge", codeChallenge)
            param("code_challenge_method", "S256")
            if (scopes.isNotEmpty()) param("scope", scopes.joinToString(" "))
            param("resource", resource)
        }
        val separator = if ('?' in authorizationEndpoint) '&' else '?'
        return "$authorizationEndpoint$separator$query"
    }

    /** Exchange an authorization code for tokens. */
    suspend fun exchangeCode(
        tokenEndpoint: String,
        clientId: String,
        code: String,
        codeVerifier: String,
        redirectUri: String,
        client: HttpClient,
    ): TokenPair {
        val body = formParams {
            param("grant_type", "authorization_code")
            param("client_id", clientId)
            param("code", code)
            param("code_verifier", codeVerifier)
            param("redirect_uri", redirectUri)
        }
        val response = httpPost(client, tokenEndpoint, body, ContentType.Application.FormUrlEncoded)?.getOrNull()
            ?: throw McpException("The authorization server rejected the token request")
        return parseTokenResponse(response)
    }

    /** Refresh an access token. Throws McpException when the server refuses it. */
    suspend fun refreshAccessToken(tokenEndpoint: String, clientId: String, refreshToken: String, client: HttpClient): TokenPair {
        val body = formParams {
            param("grant_type", "refresh_token")
            param("client_id", clientId)
            param("refresh_token", refreshToken)
        }
        val response = httpPost(client, tokenEndpoint, body, ContentType.Application.FormUrlEncoded)?.getOrNull()
            ?: throw McpException("The authorization server rejected the refresh request")
        return parseTokenResponse(response, fallbackRefreshToken = refreshToken)
    }

    private fun parseTokenResponse(body: String, fallbackRefreshToken: String? = null): TokenPair {
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrElse {
            throw McpException("The authorization server returned an invalid token response")
        }
        return TokenPair(
            accessToken = obj["access_token"]?.jsonPrimitive?.contentOrNull
                ?: throw McpException("The authorization server did not return an access_token"),
            refreshToken = obj["refresh_token"]?.jsonPrimitive?.contentOrNull ?: fallbackRefreshToken,
            expiresInSeconds = obj["expires_in"]?.jsonPrimitive?.longOrNull,
        )
    }

    private fun formParams(block: FormScope.() -> Unit): String = buildString {
        val scope = FormScope().apply(block)
        scope.parts.forEachIndexed { index, (key, value) ->
            if (index > 0) append('&')
            append(key.encodeURLParameter())
            append('=')
            append(value.encodeURLParameter())
        }
    }

    private class FormScope {
        val parts = mutableListOf<Pair<String, String>>()
        fun param(key: String, value: String) {
            parts += key to value
        }
    }

    /** PKCE pair: `verifier` (kept secret) and `S256(verifier)` (sent in the authorize URL). */
    fun pkcePair(): Pair<String, String> {
        val verifier = randomUrlSafeString(64)
        val challenge = base64UrlEncode(sha256(verifier.encodeToByteArray()))
        return verifier to challenge
    }
}

private suspend fun httpGet(client: HttpClient, url: String): Result<String> = runCatching {
    val response = client.get(url) {
        header(HttpHeaders.UserAgent, "Mozilla/5.0 (compatible; DORAI-AI/2.1)")
    }
    if (response.status.isSuccess()) response.bodyAsText() else throw McpException("HTTP ${response.status.value}")
}

private suspend fun httpPost(client: HttpClient, url: String, body: String, contentType: ContentType): Result<String> = runCatching {
    val response = client.post(url) {
        header(HttpHeaders.UserAgent, "Mozilla/5.0 (compatible; DORAI-AI/2.1)")
        setBody(body)
        contentType(contentType)
        header(HttpHeaders.Accept, "application/json")
    }
    if (response.status.isSuccess()) response.bodyAsText() else throw McpException("HTTP ${response.status.value}")
}

data class OAuthEndpoints(
    val resource: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val registrationEndpoint: String?,
    val scopes: List<String>,
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long?,
)
