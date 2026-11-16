package com.dorai.ai.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The PKCE values of a social sign-in that is waiting for its callback. */
data class DoraiCloudOAuthPending(
    val codeVerifier: String,
    val state: String,
    val redirectTo: String,
)

@Serializable
data class DoraiCloudOAuthPendingDto(
    @SerialName("code_verifier") val code_verifier: String,
    @SerialName("state") val state: String,
    @SerialName("redirect_to") val redirect_to: String,
)
