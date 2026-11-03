package com.inspiredandroid.kai

import com.inspiredandroid.kai.Version
import java.lang.Runtime

actual fun getDeviceInfo(): Map<String, Any> {
    val runtime = Runtime.getRuntime()
    return mapOf(
        "platform" to currentPlatform.displayName,
        "appVersion" to Version.appVersion,
        "osName" to System.getProperty("os.name", "unknown"),
        "osVersion" to System.getProperty("os.version", "unknown"),
        "osArch" to System.getProperty("os.arch", "unknown"),
        "availableProcessors" to runtime.availableProcessors(),
        "freeMemoryBytes" to runtime.freeMemory(),
        "totalMemoryBytes" to runtime.totalMemory(),
        "maxMemoryBytes" to runtime.maxMemory(),
    )
}
