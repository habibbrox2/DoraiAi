package com.dorai.ai.data

import com.dorai.ai.network.CloudflareEmbeddingClient
import com.dorai.ai.network.dtos.supabase.SupabaseMatchMemoryRequestDto
import com.dorai.ai.network.dtos.supabase.SupabaseMatchMemoryResultDto
import com.dorai.ai.network.dtos.supabase.SupabaseMemoryItemDto
import com.dorai.ai.network.supabase.SupabaseConfig
import com.dorai.ai.network.supabase.SupabaseRestClient
import kotlinx.serialization.json.Json

class SupabaseVectorStore(
    private val supabaseRestClient: SupabaseRestClient,
    private val embeddingClient: CloudflareEmbeddingClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    companion object {
        const val MEMORIES_TABLE = "dorai_memories"
        const val MATCH_RPC = "match_memories"
    }

    suspend fun saveMemory(
        config: SupabaseConfig,
        entry: MemoryEntry,
        cloudflareAccountId: String,
        cloudflareToken: String,
    ): Result<Unit> = runCatching {
        if (!config.isValid) return@runCatching

        // Step 1: Generate embedding via Cloudflare Workers AI
        val embedding = embeddingClient.generateEmbedding(
            text = "${entry.key}: ${entry.content}",
            accountId = cloudflareAccountId,
            apiToken = cloudflareToken,
        ).getOrThrow()

        // Step 2: Prepare payload for Supabase pgvector table
        val dto = SupabaseMemoryItemDto(
            key = entry.key,
            content = entry.content,
            category = entry.category.name,
            embedding = embedding,
            hitCount = entry.hitCount,
            source = entry.source,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt,
        )

        val body = json.encodeToString(SupabaseMemoryItemDto.serializer(), dto)
        supabaseRestClient.insertOrUpdate(config, MEMORIES_TABLE, body).getOrThrow()
    }

    suspend fun searchMemories(
        config: SupabaseConfig,
        query: String,
        cloudflareAccountId: String,
        cloudflareToken: String,
        threshold: Float = 0.5f,
        limit: Int = 5,
    ): Result<List<SupabaseMatchMemoryResultDto>> = runCatching {
        if (!config.isValid) return@runCatching emptyList()

        // Step 1: Generate query vector via Cloudflare
        val queryEmbedding = embeddingClient.generateEmbedding(
            text = query,
            accountId = cloudflareAccountId,
            apiToken = cloudflareToken,
        ).getOrThrow()

        // Step 2: Call Supabase match_memories RPC
        val requestDto = SupabaseMatchMemoryRequestDto(
            query_embedding = queryEmbedding,
            match_threshold = threshold,
            match_count = limit,
        )

        val paramsJson = json.encodeToString(SupabaseMatchMemoryRequestDto.serializer(), requestDto)
        val responseJson = supabaseRestClient.rpc(config, MATCH_RPC, paramsJson).getOrThrow()

        json.decodeFromString<List<SupabaseMatchMemoryResultDto>>(responseJson)
    }

    suspend fun forgetMemory(
        config: SupabaseConfig,
        key: String,
    ): Result<Unit> = runCatching {
        if (!config.isValid) return@runCatching
        supabaseRestClient.delete(config, MEMORIES_TABLE, "key", key).getOrThrow()
    }
}
