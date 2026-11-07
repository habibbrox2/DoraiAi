package com.dorai.ai.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class AndroidUpdateArchiveValidationTest {

    @Test
    fun acceptsValidApkArchive() {
        withTempFile { file ->
            createZip(file, listOf("AndroidManifest.xml", "classes.dex", "resources.arsc"))
            assertTrue(isValidApk(file))
        }
    }

    @Test
    fun rejectsArchiveMissingManifest() {
        withTempFile { file ->
            createZip(file, listOf("classes.dex", "resources.arsc"))
            assertFalse(isValidApk(file))
        }
    }

    @Test
    fun rejectsArchiveMissingClassesDex() {
        withTempFile { file ->
            createZip(file, listOf("AndroidManifest.xml", "resources.arsc"))
            assertFalse(isValidApk(file))
        }
    }

    @Test
    fun rejectsCorruptedOrNonZipFile() {
        withTempFile { file ->
            file.writeText("not a zip file content")
            assertFalse(isValidApk(file))
        }
    }

    @Test
    fun rejectsEmptyFile() {
        withTempFile { file ->
            assertFalse(isValidApk(file))
        }
    }

    private fun isValidApk(file: File): Boolean = runCatching {
        if (!file.isFile || file.length() <= 0L) return@runCatching false
        ZipFile(file).use { zip ->
            zip.getEntry("AndroidManifest.xml") != null &&
                zip.getEntry("classes.dex") != null
        }
    }.getOrDefault(false)

    private fun createZip(destination: File, entries: List<String>) {
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            for (entryName in entries) {
                zip.putNextEntry(ZipEntry(entryName))
                zip.write("dummy-data".toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun withTempFile(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("dorai-update-archive-test-").toFile()
        try {
            val file = File(dir, "test.apk")
            block(file)
        } finally {
            dir.deleteRecursively()
        }
    }
}
