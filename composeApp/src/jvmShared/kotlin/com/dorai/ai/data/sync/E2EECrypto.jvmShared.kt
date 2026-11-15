package com.dorai.ai.data.sync

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal actual fun encryptAesGcm(plainText: ByteArray, secretKey: ByteArray, timestamp: Long): EncryptedSyncEnvelope {
    val iv = ByteArray(12)
    SecureRandom().nextBytes(iv)

    val keySpec = SecretKeySpec(secretKey, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
    val encrypted = cipher.doFinal(plainText)

    val digest = MessageDigest.getInstance("SHA-256").digest(plainText)

    return EncryptedSyncEnvelope(
        version = 1,
        iv = Base64.encode(iv),
        ciphertext = Base64.encode(encrypted),
        checksum = Base64.encode(digest),
        updatedAt = timestamp,
    )
}

@OptIn(ExperimentalEncodingApi::class)
internal actual fun decryptAesGcm(envelope: EncryptedSyncEnvelope, secretKey: ByteArray): ByteArray {
    val iv = Base64.decode(envelope.iv)
    val ciphertext = Base64.decode(envelope.ciphertext)

    val keySpec = SecretKeySpec(secretKey, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
    val decrypted = cipher.doFinal(ciphertext)

    val calculatedDigest = MessageDigest.getInstance("SHA-256").digest(decrypted)
    val expectedDigest = try {
        Base64.decode(envelope.checksum)
    } catch (e: Exception) {
        throw IllegalStateException("Invalid checksum format in sync envelope", e)
    }

    if (!calculatedDigest.contentEquals(expectedDigest)) {
        throw IllegalStateException("Decrypted payload checksum mismatch; data may be tampered or corrupted.")
    }

    return decrypted
}

internal actual fun deriveKeyFromPassphrase(passphrase: String): ByteArray {
    require(passphrase.length >= 8) { "Sync passphrase must be at least 8 characters long." }
    return MessageDigest.getInstance("SHA-256").digest(passphrase.encodeToByteArray())
}
