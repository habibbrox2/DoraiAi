package com.inspiredandroid.kai.filereview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AndroidProjectFileReviewFileSystemTest {
    @Test
    fun resolvesOnlyProjectRelativePaths() {
        val root = Files.createTempDirectory("android-file-review-root").toFile()
        try {
            val fileSystem = AndroidProjectFileReviewFileSystem(root)
            assertEquals(
                File(root, "src/Main.kt"),
                fileSystem.resolve(FileReviewWorkspace.PROJECT, "src/Main.kt"),
            )
            listOf(
                "../outside.txt",
                "/root/projects/src/Main.kt",
                "src\\..\\outside.txt",
                "src//Main.kt",
                "src/./Main.kt",
                "bad\u0000path",
            ).forEach { path ->
                assertNull(fileSystem.resolve(FileReviewWorkspace.PROJECT, path))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsSymlinkWorkspaceRootAndChild() {
        val parent = Files.createTempDirectory("android-file-review-symlink").toFile()
        val root = File(parent, "root").apply { mkdirs() }
        val outside = File(parent, "outside").apply { mkdirs() }
        val rootLink = File(parent, "root-link")
        val childLink = File(root, "linked")
        try {
            val rootLinkCreated = runCatching {
                Files.createSymbolicLink(rootLink.toPath(), root.toPath())
            }.isSuccess
            assumeTrue("host filesystem cannot create symlinks", rootLinkCreated)
            assertNull(AndroidProjectFileReviewFileSystem(rootLink).resolve(FileReviewWorkspace.PROJECT, "file.txt"))

            val childLinkCreated = runCatching {
                Files.createSymbolicLink(childLink.toPath(), outside.toPath())
            }.isSuccess
            assumeTrue("host filesystem cannot create child symlinks", childLinkCreated)
            assertNull(AndroidProjectFileReviewFileSystem(root).resolve(FileReviewWorkspace.PROJECT, "linked/secret.txt"))
        } finally {
            parent.deleteRecursively()
        }
    }
}
