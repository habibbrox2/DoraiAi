package com.inspiredandroid.kai.filereview

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FileReviewEngineTest {
    private fun fixture(mover: AtomicFileMover = NioAtomicFileMover): Fixture {
        val root = Files.createTempDirectory("file-review-root").toFile()
        val privateDir = Files.createTempDirectory("file-review-private").toFile()
        val fs = object : FileReviewFileSystem {
            override fun resolve(workspace: FileReviewWorkspace, guestPath: String): File? {
                if (workspace != FileReviewWorkspace.PROJECT) return null
                if (guestPath.isBlank() || guestPath.startsWith("/") || guestPath.contains('\\')) return null
                val parts = guestPath.split('/')
                if (parts.any { it.isBlank() || it == ".." || it == "." }) return null
                val relative = parts.joinToString("/")
                val file = File(root, relative)
                val canonicalRoot = root.canonicalPath
                val canonicalFile = file.canonicalPath
                if (canonicalFile != canonicalRoot && !canonicalFile.startsWith(canonicalRoot + File.separator)) {
                    return null
                }
                var cursor = root
                for (part in relative.split('/')) {
                    cursor = File(cursor, part)
                    if (Files.isSymbolicLink(cursor.toPath())) return null
                }
                return file
            }
        }
        return Fixture(root, privateDir, FileReviewEngine(privateDir, fs, mover))
    }

    private data class Fixture(val root: File, val privateDir: File, val engine: FileReviewEngine)

    @Test
    fun proposeAcceptAndRollbackRestoresOriginalBytes() = runTest {
        val f = fixture()
        try {
            val target = File(f.root, "main.kt").apply { writeText("before") }
            val proposed = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "after"),
            ).proposal
            assertEquals("before", target.readText())
            val accepted = assertIs<FileReviewActionResult.Success>(f.engine.accept(proposed.id)).proposal
            assertEquals(FileReviewState.ACCEPTED, accepted.state)
            assertEquals("after", target.readText())
            val rolledBack = assertIs<FileReviewActionResult.Success>(f.engine.rollback(proposed.id)).proposal
            assertEquals(FileReviewState.ROLLED_BACK, rolledBack.state)
            assertEquals("before", target.readText())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun createAcceptAndRollbackRemovesCreatedFile() = runTest {
        val f = fixture()
        try {
            val proposed = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "new.kt", "new"),
            ).proposal
            assertEquals(FileReviewOperation.CREATE, proposed.operation)
            assertIs<FileReviewActionResult.Success>(f.engine.accept(proposed.id))
            assertEquals("new", File(f.root, "new.kt").readText())
            assertIs<FileReviewActionResult.Success>(f.engine.rollback(proposed.id))
            assertTrue(!File(f.root, "new.kt").exists())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun deleteAcceptAndRollbackRestoresFile() = runTest {
        val f = fixture()
        try {
            val target = File(f.root, "remove.txt").apply { writeText("keep me") }
            val proposed = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeDelete(FileReviewWorkspace.PROJECT, "remove.txt"),
            ).proposal
            assertTrue(target.exists())
            assertIs<FileReviewActionResult.Success>(f.engine.accept(proposed.id))
            assertTrue(!target.exists())
            assertIs<FileReviewActionResult.Success>(f.engine.rollback(proposed.id))
            assertEquals("keep me", target.readText())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun staleHashProducesConflictWithoutOverwritingExternalChange() = runTest {
        val f = fixture()
        try {
            val target = File(f.root, "main.kt").apply { writeText("before") }
            val proposal = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "agent"),
            ).proposal
            target.writeText("user edit")
            val result = assertIs<FileReviewActionResult.Conflict>(f.engine.accept(proposal.id))
            assertEquals(FileReviewState.CONFLICT, result.proposal.state)
            assertEquals("user edit", target.readText())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun symlinkAndTraversalAreRejectedByWorkspaceResolver() = runTest {
        val f = fixture()
        try {
            assumeTrue("host filesystem cannot create symlinks", HostFileSystem.canCreateSymlinks)
            val outside = Files.createTempDirectory("file-review-outside").toFile()
            File(outside, "secret").writeText("secret")
            Files.createSymbolicLink(File(f.root, "link").toPath(), outside.toPath())
            assertIs<FileReviewActionResult.Failure>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "link/secret", "changed"),
            )
            assertIs<FileReviewActionResult.Failure>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "../outside", "changed"),
            )
            outside.deleteRecursively()
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun atomicMoveFailureLeavesOriginalUntouched() = runTest {
        val failingMover = AtomicFileMover { source, target, replace ->
            if (target.name == "main.kt") {
                throw AtomicMoveNotSupportedException(source.path, target.path, "test failure")
            } else {
                NioAtomicFileMover.move(source, target, replace)
            }
        }
        val f = fixture(failingMover)
        try {
            val target = File(f.root, "main.kt").apply { writeText("before") }
            val proposal = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "after"),
            ).proposal
            val result = f.engine.accept(proposal.id)
            assertIs<FileReviewActionResult.Failure>(result)
            assertEquals("before", target.readText())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun rollbackRefusesToOverwriteChangesMadeAfterAccept() = runTest {
        val f = fixture()
        try {
            val target = File(f.root, "main.kt").apply { writeText("before") }
            val proposal = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "accepted"),
            ).proposal
            assertIs<FileReviewActionResult.Success>(f.engine.accept(proposal.id))
            target.writeText("newer user edit")
            assertIs<FileReviewActionResult.Conflict>(f.engine.rollback(proposal.id))
            assertEquals("newer user edit", target.readText())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun pendingProposalSurvivesEngineRestart() = runTest {
        val f = fixture()
        try {
            val proposal = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "staged"),
            ).proposal
            val restarted = FileReviewEngine(
                f.privateDir,
                object : FileReviewFileSystem {
                    override fun resolve(workspace: FileReviewWorkspace, guestPath: String): File? = File(f.root, guestPath)
                },
            )
            restarted.initialize()
            assertEquals(FileReviewState.PROPOSED, restarted.proposals.value.single { it.id == proposal.id }.state)
            assertEquals("staged", restarted.preview(proposal.id)?.proposedText)
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }
    @Test
    fun failureAfterAtomicReplaceRestoresOriginalBytes() = runTest {
        var failAfterFirstReplace = true
        val mover = AtomicFileMover { source, target, replace ->
            if (target.name == "main.kt" && failAfterFirstReplace) {
                failAfterFirstReplace = false
                NioAtomicFileMover.move(source, target, replace)
                throw IOException("simulated post-replace failure")
            }
            NioAtomicFileMover.move(source, target, replace)
        }
        val f = fixture(mover)
        try {
            val target = File(f.root, "main.kt").apply { writeText("before") }
            val proposal = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "after"),
            ).proposal
            assertIs<FileReviewActionResult.Failure>(f.engine.accept(proposal.id))
            assertEquals("before", target.readText())
            assertEquals(FileReviewState.FAILED, f.engine.proposals.value.single().state)
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun startupRecoversApplyInterruptedAfterAtomicReplacement() = runTest {
        val f = fixture()
        try {
            val target = File(f.root, "main.kt").apply { writeText("before") }
            val proposal = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "after"),
            ).proposal
            target.writeText("after")
            File(f.privateDir, "journal.json").writeText(
                Json.encodeToString(listOf(proposal.copy(state = FileReviewState.APPLYING))),
            )

            val restarted = FileReviewEngine(f.privateDir, object : FileReviewFileSystem {
                override fun resolve(workspace: FileReviewWorkspace, guestPath: String): File? =
                    if (workspace == FileReviewWorkspace.PROJECT && guestPath == "main.kt") {
                        target
                    } else {
                        null
                    }
            })
            restarted.initialize()

            val recovered = restarted.proposals.value.single()
            assertEquals(FileReviewState.ACCEPTED, recovered.state)
            assertTrue(recovered.rollbackAvailable)
            assertIs<FileReviewActionResult.Success>(restarted.rollback(proposal.id))
            assertEquals("before", target.readText())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }

    @Test
    fun startupRecoversRollbackInterruptedAfterOriginalWasRestored() = runTest {
        val f = fixture()
        try {
            val target = File(f.root, "main.kt").apply { writeText("before") }
            val proposal = assertIs<FileReviewActionResult.Success>(
                f.engine.proposeTextChange(FileReviewWorkspace.PROJECT, "main.kt", "after"),
            ).proposal
            assertIs<FileReviewActionResult.Success>(f.engine.accept(proposal.id))
            target.writeText("before")
            File(f.privateDir, "journal.json").writeText(
                Json.encodeToString(
                    listOf(
                        f.engine.proposals.value.single().copy(
                            state = FileReviewState.ROLLING_BACK,
                            rollbackAvailable = true,
                        ),
                    ),
                ),
            )

            val restarted = FileReviewEngine(f.privateDir, object : FileReviewFileSystem {
                override fun resolve(workspace: FileReviewWorkspace, guestPath: String): File? =
                    if (workspace == FileReviewWorkspace.PROJECT && guestPath == "main.kt") target else null
            })
            restarted.initialize()

            assertEquals(FileReviewState.ROLLED_BACK, restarted.proposals.value.single().state)
            assertEquals("before", target.readText())
        } finally {
            f.root.deleteRecursively()
            f.privateDir.deleteRecursively()
        }
    }
}

private object HostFileSystem {
    val canCreateSymlinks: Boolean = runCatching {
        val dir = Files.createTempDirectory("symlink-probe")
        Files.createSymbolicLink(dir.resolve("link"), dir.resolve("target"))
        dir.toFile().deleteRecursively()
    }.isSuccess
}
