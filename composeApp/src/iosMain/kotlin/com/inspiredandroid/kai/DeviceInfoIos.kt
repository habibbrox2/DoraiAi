package com.inspiredandroid.kai

import com.inspiredandroid.kai.Version
import platform.UIKit.UIDevice

actual fun getDeviceInfo(): Map<String, Any> {
    val device = UIDevice.currentDevice
    return mapOf(
        "platform" to currentPlatform.displayName,
        "appVersion" to Version.appVersion,
        "deviceModel" to (device.model ?: "unknown"),
        "systemName" to (device.systemName ?: "unknown"),
        "systemVersion" to (device.systemVersion ?: "unknown"),
        "name" to (device.name ?: "unknown"),
    )
}
