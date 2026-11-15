package com.dorai.ai.network.firebase

import com.dorai.ai.network.dtos.firebase.FcmMessagePayloadDto
import com.dorai.ai.network.dtos.firebase.FcmNotificationPayloadDto
import com.dorai.ai.network.dtos.firebase.FcmSendRequestDto
import com.dorai.ai.network.dtos.firebase.FcmSendResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class FirebaseMessagingClient(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun sendTaskCompletionNotification(
        config: FirebaseConfig,
        oauthAccessToken: String,
        deviceToken: String,
        taskId: String,
        taskTitle: String,
        taskSummary: String,
    ): Result<String?> = runCatching {
        require(config.isValid) { "Invalid Firebase configuration." }
        require(oauthAccessToken.isNotBlank()) { "Firebase OAuth token must not be blank." }
        require(deviceToken.isNotBlank()) { "Target device FCM token must not be blank." }

        val url = "https://fcm.googleapis.com/v1/projects/${config.cleanProjectId}/messages:send"
        val request = FcmSendRequestDto(
            message = FcmMessagePayloadDto(
                token = deviceToken.trim(),
                notification = FcmNotificationPayloadDto(
                    title = "Dorai AI টাস্ক সম্পন্ন: $taskTitle",
                    body = taskSummary,
                ),
                data = mapOf(
                    "taskId" to taskId,
                    "event" to "TASK_COMPLETED",
                ),
            ),
        )

        val response = httpClient.post(url) {
            bearerAuth(oauthAccessToken.trim())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(FcmSendRequestDto.serializer(), request))
        }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("FCM send failed [HTTP ${response.status.value}]: $body")
        }

        val parsed = json.decodeFromString(FcmSendResponseDto.serializer(), body)
        parsed.name
    }
}
