package com.dorai.ai.network.supabase

import com.dorai.ai.network.dtos.supabase.SupabaseMatchMemoryRequestDto
import com.dorai.ai.network.dtos.supabase.SupabaseMatchMemoryResultDto
import com.dorai.ai.network.dtos.supabase.SupabaseMemoryItemDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupabaseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SupabaseConfig validation handles clean url and auth keys`() {
        val validConfig = SupabaseConfig(
            url = "https://xyzcompany.supabase.co/",
            anonKey = "sample-anon-key",
        )
        assertTrue(validConfig.isValid)
        assertEquals("https://xyzcompany.supabase.co", validConfig.cleanUrl)

        val invalidHttpConfig = SupabaseConfig(
            url = "http://insecure.supabase.co",
            anonKey = "key",
        )
        assertFalse(invalidHttpConfig.isValid)

        val blankKeyConfig = SupabaseConfig(
            url = "https://xyzcompany.supabase.co",
            anonKey = "",
        )
        assertFalse(blankKeyConfig.isValid)
    }

    @Test
    fun `serializes SupabaseMemoryItemDto with vector embeddings`() {
        val dto = SupabaseMemoryItemDto(
            key = "user_preference",
            content = "Prefers dark mode and Kotlin",
            category = "PREFERENCE",
            embedding = listOf(0.12f, -0.45f, 0.89f),
            hitCount = 3,
        )

        val encoded = json.encodeToString(SupabaseMemoryItemDto.serializer(), dto)
        val decoded = json.decodeFromString(SupabaseMemoryItemDto.serializer(), encoded)

        assertEquals("user_preference", decoded.key)
        assertEquals("PREFERENCE", decoded.category)
        assertEquals(3, decoded.hitCount)
        assertEquals(3, decoded.embedding?.size)
        assertEquals(0.12f, decoded.embedding?.get(0))
    }

    @Test
    fun `serializes SupabaseMatchMemoryRequestDto and parses match results`() {
        val request = SupabaseMatchMemoryRequestDto(
            query_embedding = listOf(0.1f, 0.2f, 0.3f),
            match_threshold = 0.7f,
            match_count = 3,
        )

        val encoded = json.encodeToString(SupabaseMatchMemoryRequestDto.serializer(), request)
        assertTrue(encoded.contains("\"match_threshold\":0.7"))

        val mockRpcResponse = """
            [
              {
                "id": "1",
                "key": "user_name",
                "content": "User is Habib",
                "category": "GENERAL",
                "similarity": 0.92
              },
              {
                "id": "2",
                "key": "tech_stack",
                "content": "Uses KMP and Compose Multiplatform",
                "category": "LEARNING",
                "similarity": 0.85
              }
            ]
        """.trimIndent()

        val results = json.decodeFromString<List<SupabaseMatchMemoryResultDto>>(mockRpcResponse)
        assertEquals(2, results.size)
        assertEquals("user_name", results[0].key)
        assertEquals(0.92f, results[0].similarity)
        assertEquals("tech_stack", results[1].key)
    }
}
