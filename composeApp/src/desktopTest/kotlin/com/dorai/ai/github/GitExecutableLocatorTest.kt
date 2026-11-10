package com.dorai.ai.github

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitExecutableLocatorTest {

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    @Test
    fun `versionOf parses git version output`() {
        val locator = GitExecutableLocator()
        val tempDir = File(File(java.lang.System.getProperty("java.io.tmpdir")), "dorai-git-locator-test-${java.util.UUID.randomUUID()}")
        tempDir.mkdirs()
        val ext = if (isWindows) ".bat" else ""
        val gitScript = File(tempDir, "git$ext")
        val content = if (isWindows) {
            "@echo off\necho git version 2.47.1\n"
        } else {
            "#!/bin/sh\necho 'git version 2.47.1'\n"
        }
        gitScript.writeText(content)
        if (!isWindows) gitScript.setExecutable(true, false)

        val version = locator.versionOf(gitScript.absolutePath)

        assertNotNull(version)
        assertEquals(2, version.major)
        assertEquals(47, version.minor)
    }

    @Test
    fun `versionOf returns null for non-version output`() {
        val locator = GitExecutableLocator()
        val tempDir = File(File(java.lang.System.getProperty("java.io.tmpdir")), "dorai-git-locator-test-${java.util.UUID.randomUUID()}")
        tempDir.mkdirs()
        val ext = if (isWindows) ".bat" else ""
        val gitScript = File(tempDir, "git$ext")
        val content = if (isWindows) {
            "@echo off\necho hello world\n"
        } else {
            "#!/bin/sh\necho 'hello world'\n"
        }
        gitScript.writeText(content)
        if (!isWindows) gitScript.setExecutable(true, false)

        val version = locator.versionOf(gitScript.absolutePath)

        assertNull(version)
    }

    @Test
    fun `versionOf returns null for missing file`() {
        val locator = GitExecutableLocator()
        val version = locator.versionOf("/nonexistent/path/to/git")
        assertNull(version)
    }
}
