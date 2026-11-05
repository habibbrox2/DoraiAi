package com.dorai.ai.firecrawl

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

data class FirecrawlProtectedResourceMetadata(
    val resource: String,
    val resourceName: String?,
    val authorizationServers: List<String>,
    val scopesSupported: List<String>,
    val agentAuthUri: String?,
)

data class FirecrawlAgentAuthBlock(
    val registerUri: String,
    val revocationUri: String?,
    val identityTypesSupported: List<String>,
)

data class FirecrawlAuthorizationServerMetadata(
    val issuer: String?,
    val agentAuth: FirecrawlAgentAuthBlock?,
)

data class FirecrawlAgentAuthResult(
    val userId: String?,
    val apiKey: String,
    val registrationId: String?,
    val scopes: List<String>,
)

class FirecrawlAgentAuthException(message: String) : Exception(message)

class FirecrawlAgentAuth {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val client = httpClient {}

    suspend fun discoverProtectedResource(
        url: String = "https://www.firecrawl.dev/.well-known/oauth-protected-resource",
    ): FirecrawlProtectedResourceMetadata {
        val response = client.get(url)
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw FirecrawlAgentAuthException("Protected resource metadata failed: HTTP ${response.status.value}: ${text.take(300)}")
        }
        val obj = json.parseToJsonElement(text) as? JsonObject
            ?: throw FirecrawlAgentAuthException("Invalid protected resource metadata: not a JSON object")
        val resource = obj["resource"]?.jsonPrimitive?.contentOrNull
            ?: throw FirecrawlAgentAuthException("Protected resource metadata missing 'resource'")
        val authorizationServers = obj["authorization_servers"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()
        val scopes = obj["scopes_supported"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()
        return FirecrawlProtectedResourceMetadata(
            resource = resource,
            resourceName = obj["resource_name"]?.jsonPrimitive?.contentOrNull,
            authorizationServers = authorizationServers,
            scopesSupported = scopes,
            agentAuthUri = obj["agent_auth_uri"]?.jsonPrimitive?.contentOrNull,
        )
    }

    suspend fun discoverAuthorizationServer(
        url: String = "https://www.firecrawl.dev/.well-known/oauth-authorization-server",
    ): FirecrawlAuthorizationServerMetadata {
        val response = client.get(url)
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw FirecrawlAgentAuthException("Authorization server metadata failed: HTTP ${response.status.value}: ${text.take(300)}")
        }
        val obj = json.parseToJsonElement(text) as? JsonObject
            ?: throw FirecrawlAgentAuthException("Invalid authorization server metadata: not a JSON object")
        val agentAuthObj = obj["agent_auth"] as? JsonObject
        val agentAuth = agentAuthObj?.let { aa ->
            FirecrawlAgentAuthBlock(
                registerUri = aa["register_uri"]?.jsonPrimitive?.contentOrNull
                    ?: "https://www.firecrawl.dev/agent/auth",
                revocationUri = aa["revocation_uri"]?.jsonPrimitive?.contentOrNull,
                identityTypesSupported = aa["identity_types_supported"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    .orEmpty(),
            )
        }
        return FirecrawlAuthorizationServerMetadata(
            issuer = obj["issuer"]?.jsonPrimitive?.contentOrNull,
            agentAuth = agentAuth,
        )
    }

    suspend fun register(
        idJagJwt: String,
        requestedCredentialType: String = "api_key",
        registerUri: String = "https://www.firecrawl.dev/agent/auth",
    ): FirecrawlAgentAuthResult {
        if (idJagJwt.isBlank()) {
            throw FirecrawlAgentAuthException("ID-JAG assertion must not be blank")
        }
        val body = """{"type":"identity_assertion","assertion_type":"urn:ietf:params:oauth:token-type:id-jag","assertion":"$idJagJwt","requested_credential_type":"$requestedCredentialType"}"""
        val response = client.post(registerUri) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw FirecrawlAgentAuthException("Firecrawl agent registration failed: HTTP ${response.status.value}: ${text.take(400)}")
        }
        val obj = json.parseToJsonElement(text) as? JsonObject
            ?: throw FirecrawlAgentAuthException("Invalid registration response: not a JSON object")
        val apiKey = (obj["api_key"] ?: obj["credential"])?.jsonPrimitive?.contentOrNull
            ?: throw FirecrawlAgentAuthException("Registration response missing 'api_key'/'credential'")
        val scopes = obj["scopes"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()
        return FirecrawlAgentAuthResult(
            userId = obj["user_id"]?.jsonPrimitive?.contentOrNull,
            apiKey = apiKey,
            registrationId = obj["registration_id"]?.jsonPrimitive?.contentOrNull,
            scopes = scopes,
        )
    }
}
