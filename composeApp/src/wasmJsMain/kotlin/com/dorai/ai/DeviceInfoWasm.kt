package com.dorai.ai

import com.dorai.ai.Version

actual fun getDeviceInfo(): Map<String, Any> = mapOf(
    "platform" to currentPlatform.displayName,
    "appVersion" to Version.appVersion,
)
