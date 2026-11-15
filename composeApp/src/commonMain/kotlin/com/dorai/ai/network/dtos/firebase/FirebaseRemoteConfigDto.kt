package com.dorai.ai.network.dtos.firebase

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseRemoteConfigRequestDto(
    val appId: String? = null,
    val appInstanceId: String? = null,
)

@Serializable
data class FirebaseRemoteConfigResponseDto(
    val status: String? = null,
    val entries: Map<String, String> = emptyMap(),
    val state: String? = null,
)
