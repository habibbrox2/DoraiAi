package com.dorai.ai.network.firebase

import com.dorai.ai.network.dtos.firebase.FirebaseRemoteConfigRequestDto
import com.dorai.ai.network.dtos.firebase.FirebaseRemoteConfigResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class FirebaseRemoteConfigClient(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchRemoteConfig(
        config: FirebaseConfig,
        appInstanceId: String? = null,
    ): Result<Map<String, String>> = runCatching {
        require(config.isValid) { "Invalid Firebase configuration (projectId or apiKey missing)." }

        val url = "https://firebaseremoteconfig.googleapis.com/v1/projects/${config.cleanProjectId}/namespaces/firebase:fetch?key=${config.apiKey.trim()}"
        val requestDto = FirebaseRemoteConfigRequestDto(
            appId = config.appId,
            appInstanceId = appInstanceId,
        )

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(FirebaseRemoteConfigRequestDto.serializer(), requestDto))
        }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Firebase Remote Config fetch failed [HTTP ${response.status.value}]: $body")
        }

        val parsed = json.decodeFromString(FirebaseRemoteConfigResponseDto.serializer(), body)
        parsed.entries
    }
}
