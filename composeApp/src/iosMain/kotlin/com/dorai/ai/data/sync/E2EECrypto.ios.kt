package com.dorai.ai.data.sync

internal actual fun encryptAesGcm(plainText: ByteArray, secretKey: ByteArray, timestamp: Long): EncryptedSyncEnvelope = throw UnsupportedOperationException("E2EE sync is currently supported on Android and Desktop targets.")

internal actual fun decryptAesGcm(envelope: EncryptedSyncEnvelope, secretKey: ByteArray): ByteArray = throw UnsupportedOperationException("E2EE sync is currently supported on Android and Desktop targets.")

internal actual fun deriveKeyFromPassphrase(passphrase: String): ByteArray = throw UnsupportedOperationException("E2EE sync is currently supported on Android and Desktop targets.")
