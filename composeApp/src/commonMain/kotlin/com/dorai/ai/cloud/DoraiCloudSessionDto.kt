package com.dorai.ai.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Serialized shape of the persisted Dorai Cloud session (stored in encrypted settings). */
@Serializable
data class DoraiCloudSessionDto(
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_at") val expiresAt: Long,
)
