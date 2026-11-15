package com.dorai.ai.network.firebase

import com.dorai.ai.network.dtos.firebase.FcmMessagePayloadDto
import com.dorai.ai.network.dtos.firebase.FcmNotificationPayloadDto
import com.dorai.ai.network.dtos.firebase.FcmSendRequestDto
import com.dorai.ai.network.dtos.firebase.FirebaseRemoteConfigResponseDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirebaseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `FirebaseConfig validates non-blank projectId and apiKey`() {
        val valid = FirebaseConfig(
            projectId = "dorai-ai-prod",
            apiKey = "AIzaSyFakeApiKey12345",
        )
        assertTrue(valid.isValid)
        assertEquals("dorai-ai-prod", valid.cleanProjectId)

        val invalidProject = FirebaseConfig(
            projectId = "",
            apiKey = "AIzaSyFakeApiKey12345",
        )
        assertFalse(invalidProject.isValid)

        val invalidKey = FirebaseConfig(
            projectId = "dorai-ai-prod",
            apiKey = "",
        )
        assertFalse(invalidKey.isValid)
    }

    @Test
    fun `parses Firebase Remote Config entries envelope`() {
        val rawResponse = """
            {
              "status": "UPDATE_AVAILABLE",
              "entries": {
                "free_tier_models_override": "[\"deepseek/deepseek-r1:free\",\"meta-llama/llama-3.3-70b-instruct:free\"]",
                "enable_experimental_rag": "true",
                "max_background_tasks": "5"
              },
              "state": "NO_TEMPLATE"
            }
        """.trimIndent()

        val parsed = json.decodeFromString(FirebaseRemoteConfigResponseDto.serializer(), rawResponse)
        assertEquals("UPDATE_AVAILABLE", parsed.status)
        assertEquals(3, parsed.entries.size)
        assertEquals("true", parsed.entries["enable_experimental_rag"])
        assertTrue(parsed.entries["free_tier_models_override"]?.contains("deepseek-r1:free") == true)
    }

    @Test
    fun `serializes FCM send request with Bengali task alert`() {
        val request = FcmSendRequestDto(
            message = FcmMessagePayloadDto(
                token = "fcm_client_device_token_xyz",
                notification = FcmNotificationPayloadDto(
                    title = "Dorai AI টাস্ক সম্পন্ন: কোড অডিট",
                    body = "আপনার এজেন্টের কোড অডিট সফলভাবে সম্পন্ন হয়েছে।",
                ),
                data = mapOf(
                    "taskId" to "audit-001",
                    "event" to "TASK_COMPLETED",
                ),
            ),
        )

        val encoded = json.encodeToString(FcmSendRequestDto.serializer(), request)
        assertTrue(encoded.contains("Dorai AI টাস্ক সম্পন্ন: কোড অডিট"))
        assertTrue(encoded.contains("fcm_client_device_token_xyz"))
        assertTrue(encoded.contains("\"taskId\":\"audit-001\""))
    }
}
