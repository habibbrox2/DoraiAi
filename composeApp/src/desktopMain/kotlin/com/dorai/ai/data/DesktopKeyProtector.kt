package com.dorai.ai.data

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * OS-provided data protection for the desktop AES settings key, so the raw key never sits on
 * disk in the clear next to `settings.aes`.
 *
 * Windows: DPAPI (`CryptProtectData`/`CryptUnprotectData`) in user scope — the blob can only
 * be decrypted by the same Windows user on the same machine.
 * Other platforms: the caller falls back to the owner-only key file (as before); protection is
 * a no-op there and the raw key file remains the single source of truth.
 */
object DesktopKeyProtector {

    /** Magic prefix marking a DPAPI-protected key blob (vs. the legacy 32-byte raw key file). */
    private val MAGIC = "DORAI-DPAPI1\n".toByteArray()

    /** True when [bytes] is a protected blob rather than a raw legacy key. */
    fun isProtectedBlob(bytes: ByteArray): Boolean = bytes.size > MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    /**
     * Protects [raw] on Windows (DPAPI user scope). On non-Windows platforms the raw bytes are
     * returned unchanged, and callers keep the legacy owner-only key-file behavior.
     */
    fun protect(raw: ByteArray): ByteArray = if (Platform.isWindows()) {
        MAGIC + Crypt32Wrapper.protect(raw)
    } else {
        raw
    }

    /**
     * Reverses [protect]. Expects the exact prefix produced by [protect]; callers verify with
     * [isProtectedBlob] first. Throws on failure (unreadable / foreign machine or user).
     */
    fun unprotect(protected: ByteArray): ByteArray {
        require(isProtectedBlob(protected)) { "not a protected key blob" }
        return Crypt32Wrapper.unprotect(protected.copyOfRange(MAGIC.size, protected.size))
    }
}

@Structure.FieldOrder("cbData", "pbData")
private open class DataBlob(var cbData: Int = 0, @JvmField var pbData: Pointer? = null) : Structure() {
    fun toByteArray(): ByteArray = pbData?.getByteArray(0, cbData) ?: ByteArray(0)

    fun dispose() {
        pbData?.let { Kernel32Wrapper.localFree(it) }
    }
}

private fun blobOf(data: ByteArray): DataBlob {
    // JNA's `Memory` handles the allocation and release; `Native.toByteArray`/`Pointer`
    // juggling is avoided on purpose.
    val ptr = Memory(data.size.toLong())
    ptr.write(0, data, 0, data.size)
    return DataBlob(data.size, ptr)
}

private interface Crypt32 : Library {
    fun CryptProtectData(
        dataIn: DataBlob,
        dataDescr: String?,
        entropy: DataBlob?,
        reserved: Pointer?,
        prompt: Pointer?,
        flags: Int,
        dataOut: DataBlob,
    ): Boolean

    fun CryptUnprotectData(
        dataIn: DataBlob,
        description: Pointer?,
        entropy: DataBlob?,
        reserved: Pointer?,
        prompt: Pointer?,
        flags: Int,
        dataOut: DataBlob,
    ): Boolean
}

private interface Kernel32 : Library {
    fun LocalFree(hMem: Pointer): Pointer?
}

private object Kernel32Wrapper {
    private val kernel32: Kernel32 = Native.load("Kernel32", Kernel32::class.java)

    fun localFree(ptr: Pointer) {
        kernel32.LocalFree(ptr)
    }
}

private object Crypt32Wrapper {
    private val crypt32: Crypt32 = Native.load("Crypt32", Crypt32::class.java)

    fun protect(raw: ByteArray): ByteArray {
        val inBlob = blobOf(raw)
        val outBlob = DataBlob()
        val ok = crypt32.CryptProtectData(inBlob, "DORAI AI settings key", null, null, null, 0, outBlob)
        check(ok) { "CryptProtectData failed" }
        val result = outBlob.toByteArray()
        outBlob.dispose()
        return result
    }

    fun unprotect(protected: ByteArray): ByteArray {
        val inBlob = blobOf(protected)
        val outBlob = DataBlob()
        val ok = crypt32.CryptUnprotectData(inBlob, null, null, null, null, 0, outBlob)
        check(ok) { "CryptUnprotectData failed" }
        val result = outBlob.toByteArray()
        outBlob.dispose()
        return result
    }
}
