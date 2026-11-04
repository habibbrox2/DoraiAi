package com.dorai.ai

import platform.CoreTelephony.CTCarrier
import platform.CoreTelephony.CTTelephonyNetworkInfo

actual fun getSimOperatorCode(): String? = try {
    val info = CTTelephonyNetworkInfo()
    val carriers = info.serviceSubscriberCellularProviders
    val first = carriers?.values?.firstOrNull() as? CTCarrier
    val mcc = first?.mobileCountryCode
    val mnc = first?.mobileNetworkCode
    if (mcc.isNullOrBlank() || mnc.isNullOrBlank()) null else "$mcc$mnc"
} catch (_: Exception) {
    null
}
