package com.dorai.ai.network.dtos.supabase

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseMemoryItemDto(
    val id: String? = null,
    val key: String,
    val content: String,
    val category: String,
    val embedding: List<Float>? = null,
    val hitCount: Int = 1,
    val source: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class SupabaseMatchMemoryRequestDto(
    val query_embedding: List<Float>,
    val match_threshold: Float = 0.5f,
    val match_count: Int = 5,
)

@Serializable
data class SupabaseMatchMemoryResultDto(
    val id: String? = null,
    val key: String,
    val content: String,
    val category: String,
    val similarity: Float,
    val source: String? = null,
)
