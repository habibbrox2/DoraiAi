package com.dorai.ai.data.sync

import com.dorai.ai.network.dtos.supabase.SupabaseSyncRecordDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class E2EESyncTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `encrypt and decrypt roundtrip matches original plaintext`() {
        val passphrase = "SuperSecureSyncPassphrase2026!"
        val key = deriveKeyFromPassphrase(passphrase)
        val originalText = "Hello from Dorai AI! Settings and chats are end-to-end encrypted."
        val timestamp = 1731715200000L

        val envelope = encryptAesGcm(originalText.encodeToByteArray(), key, timestamp)

        assertEquals(1, envelope.version)
        assertEquals(timestamp, envelope.updatedAt)
        assertTrue(envelope.iv.isNotBlank())
        assertTrue(envelope.ciphertext.isNotBlank())
        assertTrue(envelope.checksum.isNotBlank())

        // Decrypt
        val decryptedBytes = decryptAesGcm(envelope, key)
        val decryptedText = decryptedBytes.decodeToString()

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun `fails to decrypt with incorrect passphrase or key`() {
        val originalKey = deriveKeyFromPassphrase("CorrectPassphrase123")
        val wrongKey = deriveKeyFromPassphrase("WrongPassphrase456")
        val plaintext = "Sensitive user API keys: sk-12345".encodeToByteArray()

        val envelope = encryptAesGcm(plaintext, originalKey, 1000L)

        assertFailsWith<Exception> {
            decryptAesGcm(envelope, wrongKey)
        }
    }

    @Test
    fun `detects tampered ciphertext or checksum mismatch`() {
        val key = deriveKeyFromPassphrase("TestPassphrase123")
        val plaintext = "Data to protect".encodeToByteArray()
        val envelope = encryptAesGcm(plaintext, key, 1000L)

        val tamperedEnvelope = envelope.copy(
            checksum = kotlin.io.encoding.Base64.encode(ByteArray(32)),
        )

        assertFailsWith<IllegalStateException> {
            decryptAesGcm(tamperedEnvelope, key)
        }
    }

    @Test
    fun `short passphrase is rejected by key derivation`() {
        assertFailsWith<IllegalArgumentException> {
            deriveKeyFromPassphrase("short")
        }
    }

    @Test
    fun `SupabaseSyncRecordDto serialization preserves sync metadata`() {
        val record = SupabaseSyncRecordDto(
            id = "record-1",
            device_id = "device-android-001",
            payload_version = 1,
            iv = "abcd",
            ciphertext = "efgh",
            checksum = "ijkl",
            updated_at = 123456789L,
        )

        val encoded = json.encodeToString(SupabaseSyncRecordDto.serializer(), record)
        val decoded = json.decodeFromString(SupabaseSyncRecordDto.serializer(), encoded)

        assertEquals("device-android-001", decoded.device_id)
        assertEquals("efgh", decoded.ciphertext)
        assertEquals(123456789L, decoded.updated_at)
    }
}
