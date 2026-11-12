package com.dorai.ai.mcp

internal actual fun sha256(input: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(input)

internal actual fun secureRandomBytes(length: Int): ByteArray {
    val bytes = ByteArray(length)
    java.security.SecureRandom().nextBytes(bytes)
    return bytes
}
