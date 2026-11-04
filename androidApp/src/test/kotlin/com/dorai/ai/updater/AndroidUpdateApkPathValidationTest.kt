package com.dorai.ai.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AndroidUpdateApkPathValidationTest {
    private val assetName = "DORAI-AI-v2.0.75-Android.apk"

    @Test
    fun acceptsNonEmptyApkDirectlyInsideUpdatesDirectory() {
        withTemporaryDirectory { root ->
            val updates = File(root, "updates").apply { mkdirs() }
            val apk = File(updates, assetName).apply { writeBytes(byteArrayOf(1, 2, 3)) }
            assertTrue(AndroidUpdateApkPathValidation.isValid(updates, apk, assetName))
        }
    }

    @Test
    fun rejectsApkOutsideUpdatesDirectory() {
        withTemporaryDirectory { root ->
            val updates = File(root, "updates").apply { mkdirs() }
            val apk = File(root, assetName).apply { writeBytes(byteArrayOf(1)) }
            assertFalse(AndroidUpdateApkPathValidation.isValid(updates, apk, assetName))
        }
    }

    @Test
    fun rejectsNestedApkPath() {
        withTemporaryDirectory { root ->
            val updates = File(root, "updates").apply { mkdirs() }
            val nested = File(updates, "nested").apply { mkdirs() }
            val apk = File(nested, assetName).apply { writeBytes(byteArrayOf(1)) }
            assertFalse(AndroidUpdateApkPathValidation.isValid(updates, apk, assetName))
        }
    }

    @Test
    fun rejectsUnexpectedAssetName() {
        withTemporaryDirectory { root ->
            val updates = File(root, "updates").apply { mkdirs() }
            val apk = File(updates, "renamed.apk").apply { writeBytes(byteArrayOf(1)) }
            assertFalse(AndroidUpdateApkPathValidation.isValid(updates, apk, assetName))
        }
    }

    @Test
    fun rejectsEmptyApk() {
        withTemporaryDirectory { root ->
            val updates = File(root, "updates").apply { mkdirs() }
            val apk = File(updates, assetName).apply { createNewFile() }
            assertFalse(AndroidUpdateApkPathValidation.isValid(updates, apk, assetName))
        }
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val root = Files.createTempDirectory("dorai-update-test-").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
