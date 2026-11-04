package com.dorai.ai.updater

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class AndroidUpdateChecksumVerifierTest {
    @Test
    fun acceptsFileWithExpectedSha256() {
        withTemporaryApk("verified-apk") { apk ->
            assertEquals(
                AndroidUpdateChecksumVerifier.Result.Valid,
                AndroidUpdateChecksumVerifier.verify(apk, sha256("verified-apk")),
            )
        }
    }

    @Test
    fun rejectsChecksumMismatch() {
        withTemporaryApk("tampered-apk") { apk ->
            assertEquals(
                AndroidUpdateChecksumVerifier.Result.Invalid("Downloaded APK checksum mismatch"),
                AndroidUpdateChecksumVerifier.verify(apk, sha256("original-apk")),
            )
        }
    }

    @Test
    fun rejectsMalformedExpectedChecksum() {
        withTemporaryApk("apk") { apk ->
            assertEquals(
                AndroidUpdateChecksumVerifier.Result.Invalid("Invalid expected APK SHA-256"),
                AndroidUpdateChecksumVerifier.verify(apk, "not-a-sha256"),
            )
        }
    }

    @Test
    fun rejectsMissingFile() {
        val missing = File(Files.createTempDirectory("kai-update-checksum-").toFile(), "missing.apk")
        try {
            assertEquals(
                AndroidUpdateChecksumVerifier.Result.Invalid("Downloaded APK is missing or empty"),
                AndroidUpdateChecksumVerifier.verify(missing, sha256("apk")),
            )
        } finally {
            missing.parentFile?.deleteRecursively()
        }
    }

    private fun withTemporaryApk(content: String, block: (File) -> Unit) {
        val directory = Files.createTempDirectory("kai-update-checksum-").toFile()
        try {
            val apk = File(directory, "update.apk").apply { writeText(content) }
            block(apk)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
