package com.dorai.ai.mcp

internal const val BASE64_URL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
private const val RANDOM_TOKEN_LENGTH = 64

/** Generates a cryptographically random URL-safe token of exactly [length] characters. */
internal fun randomUrlSafeString(length: Int = RANDOM_TOKEN_LENGTH): String {
    val bytes = secureRandomBytes(length)
    val sb = StringBuilder(length)
    for (byte in bytes) {
        sb.append(BASE64_URL_CHARS[byte.toInt() and 0x3f])
    }
    return sb.toString()
}

/** base64url (no padding) encoding — safe for PKCE code_challenge. */
internal fun base64UrlEncode(bytes: ByteArray): String {
    val out = StringBuilder((bytes.size * 4 + 2) / 3)
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i].toInt() and 0xff
        val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xff else 0
        val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xff else 0
        out.append(BASE64_URL_CHARS[b0 ushr 2])
        out.append(BASE64_URL_CHARS[((b0 and 0x03) shl 4) or (b1 ushr 4)])
        if (i + 1 < bytes.size) out.append(BASE64_URL_CHARS[((b1 and 0x0f) shl 2) or (b2 ushr 6)])
        if (i + 2 < bytes.size) out.append(BASE64_URL_CHARS[b2 and 0x3f])
        i += 3
    }
    return out.toString()
}
