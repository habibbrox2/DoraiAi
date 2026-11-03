package com.inspiredandroid.kai.network

import com.inspiredandroid.kai.network.dtos.openaicompatible.OpenAICompatibleModelResponseDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

internal data class CloudflareModelSearchPage(
    val models: List<OpenAICompatibleModelResponseDto.Model>,
    val totalPages: Int?,
)

internal fun isValidCloudflareAccountId(value: String): Boolean = value.trim().matches(Regex("[a-fA-F0-9]{32}"))

/** Reads Cloudflare's standard v4 errors array, including non-2xx responses. */
internal fun parseCloudflareModelSearchError(body: String): String? = try {
    val envelope = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
    (envelope["errors"] as? JsonArray)
        ?.firstNotNullOfOrNull { item ->
            val error = item as? JsonObject ?: return@firstNotNullOfOrNull null
            sequenceOf("message", "code")
                .mapNotNull { key -> (error[key] as? JsonPrimitive)?.contentOrNull }
                .firstOrNull { it.isNotBlank() }
        }
} catch (_: Exception) {
    null
}

/** Parses Cloudflare's v4 envelope without assuming the OpenAI { data: [...] } schema. */
internal fun parseCloudflareModelSearchPage(body: String): CloudflareModelSearchPage {
    val envelope = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
    if ((envelope["success"] as? JsonPrimitive)?.contentOrNull == "false") {
        val message = (envelope["errors"] as? JsonArray)
            ?.firstOrNull()
            ?.let { (it as? JsonObject)?.get("message") as? JsonPrimitive }
            ?.contentOrNull
        throw IllegalArgumentException(message ?: "Cloudflare model search failed.")
    }

    val result = envelope["result"] as? JsonArray
        ?: throw IllegalArgumentException("Cloudflare model search returned an invalid result.")
    val models = result.mapNotNull { item ->
        val model = item as? JsonObject ?: return@mapNotNull null
        val id = sequenceOf("name", "id", "model_id", "model")
            .mapNotNull { key -> (model[key] as? JsonPrimitive)?.contentOrNull }
            .firstOrNull { it.isNotBlank() }
            ?: return@mapNotNull null
        val description = sequenceOf("description", "task")
            .mapNotNull { key -> (model[key] as? JsonPrimitive)?.contentOrNull }
            .firstOrNull { it.isNotBlank() }
        val owner = sequenceOf("source", "provider")
            .mapNotNull { key -> (model[key] as? JsonPrimitive)?.contentOrNull }
            .firstOrNull { it.isNotBlank() }
        OpenAICompatibleModelResponseDto.Model(
            id = id,
            name = id,
            owned_by = owner,
            description = description,
        )
    }
    val totalPages = ((envelope["result_info"] as? JsonObject)?.get("total_pages") as? JsonPrimitive)
        ?.contentOrNull?.toIntOrNull()
    return CloudflareModelSearchPage(models = models, totalPages = totalPages)
}
