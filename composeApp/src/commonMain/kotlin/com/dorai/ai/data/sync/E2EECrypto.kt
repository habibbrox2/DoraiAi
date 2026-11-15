package com.dorai.ai.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedSyncEnvelope(
    val version: Int = 1,
    val iv: String,
    val ciphertext: String,
    val checksum: String,
    val updatedAt: Long,
)

internal expect fun encryptAesGcm(plainText: ByteArray, secretKey: ByteArray, timestamp: Long): EncryptedSyncEnvelope

internal expect fun decryptAesGcm(envelope: EncryptedSyncEnvelope, secretKey: ByteArray): ByteArray

internal expect fun deriveKeyFromPassphrase(passphrase: String): ByteArray
