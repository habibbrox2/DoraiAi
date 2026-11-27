package com.dorai.ai.cloud

import kotlin.time.Clock

/** A signed-in Dorai Cloud session backed by Firebase Auth ID tokens. */
data class DoraiCloudSession(
    val userId: String,
    val email: String?,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochSeconds: Long,
) {
    fun isExpired(nowEpochSeconds: Long = epochSecondsNow(), leewaySeconds: Long = 60): Boolean = expiresAtEpochSeconds - leewaySeconds <= nowEpochSeconds
}

internal fun epochSecondsNow(): Long = Clock.System.now().toEpochMilliseconds() / 1000
