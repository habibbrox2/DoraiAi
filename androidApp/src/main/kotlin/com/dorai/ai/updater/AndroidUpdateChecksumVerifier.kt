package com.dorai.ai.updater

import java.io.File
import java.security.MessageDigest

/** Re-hashes the private APK immediately before it is handed to Android's installer. */
internal object AndroidUpdateChecksumVerifier {
    private val sha256Pattern = Regex("^[a-fA-F0-9]{64}$")

    sealed interface Result {
        data object Valid : Result
        data class Invalid(val reason: String) : Result
    }

    fun verify(apkFile: File, expectedSha256: String): Result {
        if (!sha256Pattern.matches(expectedSha256)) {
            return Result.Invalid("Invalid expected APK SHA-256")
        }
        if (!apkFile.isFile || apkFile.length() <= 0L) {
            return Result.Invalid("Downloaded APK is missing or empty")
        }

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            apkFile.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val expected = expectedSha256.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            if (MessageDigest.isEqual(expected, digest.digest())) {
                Result.Valid
            } else {
                Result.Invalid("Downloaded APK checksum mismatch")
            }
        } catch (_: Exception) {
            Result.Invalid("Unable to verify downloaded APK checksum")
        }
    }
}
