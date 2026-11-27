package com.dorai.ai.cloud

import kotlin.test.Test
import kotlin.test.assertEquals

class PkceCryptoTest {

    @Test
    fun `sha256 matches the published abc vector`() {
        val digest = sha256("abc".encodeToByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            digest.toHex(),
        )
    }

    @Test
    fun `sha256 matches the empty-input vector`() {
        val digest = sha256(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            digest.toHex(),
        )
    }

    @Test
    fun `sha256 handles inputs across block boundaries`() {
        val digest = sha256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray())
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            digest.toHex(),
        )
    }

    @Test
    fun `base64url is unpadded and url-safe`() {
        // RFC 7636 style: bytes 0xFB 0xFF 0xBF encode to "-_-_" without padding.
        assertEquals("-_-_", base64UrlEncode(byteArrayOf(0xFB.toByte(), 0xFF.toByte(), 0xBF.toByte())))
        assertEquals("AAAA", base64UrlEncode(byteArrayOf(0, 0, 0)))
    }
}

private fun ByteArray.toHex(): String = joinToString("") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }
