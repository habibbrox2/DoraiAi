package com.dorai.ai.network

import com.dorai.ai.network.dtos.cloudflare.CloudflareEmbeddingRequestDto
import com.dorai.ai.network.dtos.cloudflare.CloudflareEmbeddingResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject

internal fun parseCloudflareEmbeddingResponse(body: String): List<List<Float>> {
    val json = Json { ignoreUnknownKeys = true }
    val envelope = try {
        json.parseToJsonElement(body).jsonObject
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid JSON response from Cloudflare Embeddings API: ${e.message}", e)
    }

    val success = (envelope["success"] as? JsonPrimitive)?.contentOrNull != "false"
    if (!success) {
        val errorMessage = (envelope["errors"] as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.get("message")?.let { m -> (m as? JsonPrimitive)?.contentOrNull } }
            ?.firstOrNull { it.isNotBlank() }
            ?: "Cloudflare Embeddings API reported failure without details."
        throw IllegalArgumentException(errorMessage)
    }

    val result = envelope["result"] as? JsonObject
        ?: throw IllegalArgumentException("Cloudflare Embeddings response missing 'result' object.")

    val data = result["data"] as? JsonArray
        ?: throw IllegalArgumentException("Cloudflare Embeddings response missing 'data' array.")

    if (data.isEmpty()) return emptyList()

    // Determine if data is 2D array [[0.1, 0.2, ...]] or 1D array [0.1, 0.2, ...]
    val firstItem = data.first()
    return if (firstItem is JsonArray) {
        data.map { row ->
            (row as? JsonArray)?.mapNotNull { item ->
                (item as? JsonPrimitive)?.floatOrNull
                    ?: (item as? JsonPrimitive)?.doubleOrNull?.toFloat()
            } ?: emptyList()
        }
    } else {
        // Single vector wrapped as 1-element list of vectors
        val singleVector = data.mapNotNull { item ->
            (item as? JsonPrimitive)?.floatOrNull
                ?: (item as? JsonPrimitive)?.doubleOrNull?.toFloat()
        }
        listOf(singleVector)
    }
}

class CloudflareEmbeddingClient(
    private val httpClient: HttpClient,
) {
    companion object {
        const val DEFAULT_MODEL = "@cf/baai/bge-small-en-v1.5"
        const val DIMENSIONS = 384
        private const val BASE_URL = "https://api.cloudflare.com/client/v4/accounts"
    }

    suspend fun generateEmbeddings(
        texts: List<String>,
        accountId: String,
        apiToken: String,
        model: String = DEFAULT_MODEL,
        gatewayId: String? = null,
    ): Result<List<List<Float>>> = runCatching {
        val cleanedAccountId = accountId.trim()
        require(isValidCloudflareAccountId(cleanedAccountId)) {
            "Invalid Cloudflare Account ID. Must be 32 hex characters."
        }
        require(apiToken.isNotBlank()) {
            "Cloudflare API token must not be blank."
        }
        require(texts.isNotEmpty()) {
            "Input texts for embedding must not be empty."
        }

        val url = "$BASE_URL/$cleanedAccountId/ai/run/$model"
        val response = httpClient.post(url) {
            bearerAuth(apiToken.trim())
            contentType(ContentType.Application.Json)
            if (!gatewayId.isNullOrBlank()) {
                header("cf-aig-gateway-id", gatewayId)
            }
            setBody(Json.encodeToString(CloudflareEmbeddingRequestDto.serializer(), CloudflareEmbeddingRequestDto(text = texts)))
        }

        val responseBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val errorDetail = parseCloudflareModelSearchError(responseBody) ?: "HTTP ${response.status.value}"
            throw IllegalStateException("Cloudflare Embeddings request failed: $errorDetail")
        }

        parseCloudflareEmbeddingResponse(responseBody)
    }

    suspend fun generateEmbedding(
        text: String,
        accountId: String,
        apiToken: String,
        model: String = DEFAULT_MODEL,
        gatewayId: String? = null,
    ): Result<List<Float>> = runCatching {
        val embeddings = generateEmbeddings(listOf(text), accountId, apiToken, model, gatewayId).getOrThrow()
        embeddings.firstOrNull() ?: throw IllegalStateException("No embedding returned for input text.")
    }
}
