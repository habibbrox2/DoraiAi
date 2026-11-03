package com.inspiredandroid.kai

import android.telephony.TelephonyManager

actual fun getSimOperatorCode(): String? {
    if (!AppContextHolder.isInitialized) return null
    return try {
        val tm = AppContextHolder.context.getSystemService(TelephonyManager::class.java) ?: return null
        val code = tm.simOperator
        if (code.isNullOrBlank()) null else code
    } catch (_: Exception) {
        null
    }
}
