package com.dorai.ai.network.dtos.cloudflare

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CloudflareEmbeddingRequestDto(
    val text: List<String>,
)

@Serializable
data class CloudflareEmbeddingResponseDto(
    val success: Boolean,
    val result: CloudflareEmbeddingResultDto? = null,
    val errors: List<CloudflareEmbeddingErrorDto> = emptyList(),
    val messages: List<JsonElement> = emptyList(),
)

@Serializable
data class CloudflareEmbeddingResultDto(
    val shape: List<Int>? = null,
    val data: List<List<Float>> = emptyList(),
)

@Serializable
data class CloudflareEmbeddingErrorDto(
    val code: Int? = null,
    val message: String? = null,
)
