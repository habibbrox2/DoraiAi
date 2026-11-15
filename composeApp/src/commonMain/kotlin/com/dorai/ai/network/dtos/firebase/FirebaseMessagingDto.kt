package com.dorai.ai.network.dtos.firebase

import kotlinx.serialization.Serializable

@Serializable
data class FcmNotificationPayloadDto(
    val title: String,
    val body: String,
)

@Serializable
data class FcmMessagePayloadDto(
    val token: String,
    val notification: FcmNotificationPayloadDto,
    val data: Map<String, String> = emptyMap(),
)

@Serializable
data class FcmSendRequestDto(
    val message: FcmMessagePayloadDto,
)

@Serializable
data class FcmSendResponseDto(
    val name: String? = null,
)
