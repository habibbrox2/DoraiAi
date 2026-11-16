package com.dorai.ai.cloud

/** Cryptographically secure random bytes (used for the OAuth PKCE verifier and state). */
internal expect fun secureRandomBytes(size: Int): ByteArray

/** Base64url encoding without padding, as required by RFC 7636. */
internal fun base64UrlEncode(bytes: ByteArray): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    val builder = StringBuilder()
    var index = 0
    while (index < bytes.size) {
        val b0 = bytes[index].toInt() and 0xFF
        val b1 = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xFF else 0
        val b2 = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else 0
        builder.append(alphabet[b0 ushr 2])
        builder.append(alphabet[((b0 and 0x03) shl 4) or (b1 ushr 4)])
        if (index + 1 < bytes.size) builder.append(alphabet[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
        if (index + 2 < bytes.size) builder.append(alphabet[b2 and 0x3F])
        index += 3
    }
    return builder.toString()
}
