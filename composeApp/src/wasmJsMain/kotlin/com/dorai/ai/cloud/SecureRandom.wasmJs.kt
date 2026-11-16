package com.dorai.ai.cloud

import kotlinx.browser.window
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

private external interface WebCrypto {
    fun getRandomValues(array: Int8Array): Int8Array
}

private external interface WindowCrypto {
    val crypto: WebCrypto
}

internal actual fun secureRandomBytes(size: Int): ByteArray {
    val array = Int8Array(size)
    (window as WindowCrypto).crypto.getRandomValues(array)
    return ByteArray(size) { index -> array.get(index) }
}
