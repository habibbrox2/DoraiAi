package com.inspiredandroid.kai.filereview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.UUID

interface FileReviewFileSystem {
    fun resolve(workspace: FileReviewWorkspace, guestPath: String): File?
}

fun interface AtomicFileMover {
    fun move(source: File, target: File, replaceExisting: Boolean)
}

object NioAtomicFileMover : AtomicFileMover {
    override fun move(source: File, target: File, replaceExisting: Boolean) {
        if (replaceExisting) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } else {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }
    }
}

/**
 * Durable, compare-before-write transaction engine. Staged bytes and backups stay in
 * app-private storage; only metadata is exposed through [proposals].
 */
class FileReviewEngine(
    private val stagingRoot: File,
    private val fileSystem: FileReviewFileSystem,
    private val mover: AtomicFileMover = NioAtomicFileMover,
    private val clock: () -> Long = System::currentTimeMillis,
) : FileReviewController {

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val journal = File(stagingRoot, "journal.json")
    private val _proposals = MutableStateFlow<List<FileReviewProposal>>(emptyList())
    override val proposals: StateFlow<List<FileReviewProposal>> = _proposals
    private var initialized = false

    override suspend fun initialize() {
        mutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                if (!stagingRoot.exists() && !stagingRoot.mkdirs()) error("Cannot create private File Review storage")
                if (!stagingRoot.isDirectory) error("File Review storage is not a directory")
                if (journal.exists()) {
                    _proposals.value = json.decodeFromString(journal.readText(Charsets.UTF_8))
                }
                recoverApplyingTransactions()
                cleanupExpired()
                persistJournal()
                initialized = true
            }
        }
    }

    override suspend fun proposeTextChange(
        workspace: FileReviewWorkspace,
        path: String,
        content: String,
    ): FileReviewActionResult {
        initialize()
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val proposed = encodeStrictUtf8(content)
                    if (proposed.size.toLong() > FILE_REVIEW_MAX_BYTES) {
                        return@withContext FileReviewActionResult.Failure("Proposed text exceeds the 512 KiB limit")
                    }
                    createProposal(workspace, path, proposed, delete = false)
                } catch (e: Exception) {
                    FileReviewActionResult.Failure(e.message ?: "Could not stage file change")
                }
            }
        }
    }

    override suspend fun proposeDelete(workspace: FileReviewWorkspace, path: String): FileReviewActionResult {
        initialize()
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    createProposal(workspace, path, proposed = null, delete = true)
                } catch (e: Exception) {
                    FileReviewActionResult.Failure(e.message ?: "Could not stage file deletion")
                }
            }
        }
    }

    private fun createProposal(
        workspace: FileReviewWorkspace,
        path: String,
        proposed: ByteArray?,
        delete: Boolean,
    ): FileReviewActionResult {
        val target = fileSystem.resolve(workspace, path)
            ?: return FileReviewActionResult.Failure(
                "Path is outside the selected workspace or contains an unsafe path component",
            )
        val parent = target.parentFile ?: return FileReviewActionResult.Failure("Target has no parent directory")
        if (!parent.isDirectory || Files.isSymbolicLink(parent.toPath())) {
            return FileReviewActionResult.Failure("Parent directory is missing or unsafe")
        }
        val original = readTarget(target)
        if (original != null && !isStrictUtf8(original)) {
            return FileReviewActionResult.Failure("Binary or invalid UTF-8 files cannot be staged")
        }
        if (delete && original == null) {
            return FileReviewActionResult.Failure("Cannot delete a file that does not exist")
        }
        if (!delete && proposed == null) return FileReviewActionResult.Failure("Missing proposed content")
        val totalStaged = stagingRoot.walkTopDown().filter { it.isFile && it != journal }.sumOf { it.length() }
        val requested = (original?.size ?: 0).toLong() + (proposed?.size ?: 0).toLong()
        if (totalStaged + requested > MAX_STAGING_BYTES) {
            return FileReviewActionResult.Failure("File Review staging quota exceeded")
        }
        val operation = when {
            delete -> FileReviewOperation.DELETE
            original == null -> FileReviewOperation.CREATE
            else -> FileReviewOperation.UPDATE
        }
        val id = UUID.randomUUID().toString()
        val now = clock()
        val proposal = FileReviewProposal(
            id = id,
            workspace = workspace,
            path = path,
            operation = operation,
            originalSha256 = original?.let(::sha256),
            proposedSha256 = proposed?.let(::sha256),
            originalBytes = original?.size?.toLong() ?: 0L,
            proposedBytes = proposed?.size?.toLong() ?: 0L,
            state = FileReviewState.PROPOSED,
            createdAtMs = now,
            updatedAtMs = now,
        )
        return try {
            original?.let { writeAtomic(originalFile(id), it, replaceExisting = false) }
            proposed?.let { writeAtomic(proposedFile(id), it, replaceExisting = false) }
            _proposals.value = _proposals.value + proposal
            persistJournal()
            FileReviewActionResult.Success(proposal)
        } catch (e: Exception) {
            _proposals.value = _proposals.value.filterNot { it.id == id }
            originalFile(id).delete()
            proposedFile(id).delete()
            FileReviewActionResult.Failure(e.message ?: "Could not persist staged proposal")
        }
    }

    override suspend fun preview(proposalId: String): FileReviewPreview? {
        initialize()
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val proposal = find(proposalId) ?: return@withContext null
                val original = originalFile(proposalId).takeIf(File::isFile)?.readBytes()?.let(::decodeStrictUtf8)
                val proposed = proposedFile(proposalId).takeIf(File::isFile)?.readBytes()?.let(::decodeStrictUtf8)
                FileReviewPreview(originalText = original, proposedText = proposed)
            }
        }
    }

    override suspend fun accept(proposalId: String): FileReviewActionResult {
        initialize()
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val proposal = find(proposalId) ?: return@withContext FileReviewActionResult.NotFound
                if (proposal.state != FileReviewState.PROPOSED) {
                    return@withContext FileReviewActionResult.Failure("Proposal is not pending review")
                }
                val target = fileSystem.resolve(proposal.workspace, proposal.path)
                    ?: return@withContext FileReviewActionResult.Failure("Workspace path is no longer safe")
                val current = try {
                    readTarget(target)
                } catch (e: Exception) {
                    return@withContext FileReviewActionResult.Failure(e.message ?: "Could not inspect target")
                }
                if (current?.let(::sha256) != proposal.originalSha256) {
                    return@withContext markConflict(proposal, "File changed after the proposal was created")
                }
                val applying = proposal.copy(state = FileReviewState.APPLYING, updatedAtMs = clock())
                replace(applying)
                persistJournal()
                try {
                    when (proposal.operation) {
                        FileReviewOperation.DELETE -> Files.delete(target.toPath())

                        FileReviewOperation.CREATE, FileReviewOperation.UPDATE -> {
                            val bytes = proposedFile(proposalId).readBytes()
                            writeTargetAtomically(
                                target,
                                bytes,
                                replaceExisting = proposal.operation == FileReviewOperation.UPDATE,
                            )
                        }
                    }
                    val after = readTarget(target)
                    if (after?.let(::sha256) != proposal.proposedSha256) {
                        throw IllegalStateException("Post-write SHA-256 verification failed")
                    }
                    val accepted = applying.copy(
                        state = FileReviewState.ACCEPTED,
                        updatedAtMs = clock(),
                        rollbackAvailable = true,
                    )
                    replace(accepted)
                    persistJournal()
                    FileReviewActionResult.Success(accepted)
                } catch (e: Exception) {
                    val restored = runCatching { restoreOriginal(proposal, target) }.isSuccess
                    val failed = applying.copy(
                        state = if (restored) FileReviewState.FAILED else FileReviewState.RECOVERY_REQUIRED,
                        updatedAtMs = clock(),
                    )
                    replace(failed)
                    persistJournal()
                    FileReviewActionResult.Failure(e.message ?: "Atomic file update failed")
                }
            }
        }
    }

    override suspend fun reject(proposalId: String): FileReviewActionResult {
        initialize()
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val proposal = find(proposalId) ?: return@withContext FileReviewActionResult.NotFound
                if (proposal.state != FileReviewState.PROPOSED && proposal.state != FileReviewState.CONFLICT) {
                    return@withContext FileReviewActionResult.Failure("Only a pending proposal can be rejected")
                }
                originalFile(proposalId).delete()
                proposedFile(proposalId).delete()
                val rejected = proposal.copy(
                    state = FileReviewState.REJECTED,
                    updatedAtMs = clock(),
                    rollbackAvailable = false,
                )
                replace(rejected)
                persistJournal()
                FileReviewActionResult.Success(rejected)
            }
        }
    }

    override suspend fun rollback(proposalId: String): FileReviewActionResult {
        initialize()
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val proposal = find(proposalId) ?: return@withContext FileReviewActionResult.NotFound
                if (proposal.state != FileReviewState.ACCEPTED || !proposal.rollbackAvailable) {
                    return@withContext FileReviewActionResult.Failure("Rollback is unavailable for this proposal")
                }
                val target = fileSystem.resolve(proposal.workspace, proposal.path)
                    ?: return@withContext FileReviewActionResult.Failure("Workspace path is no longer safe")
                val current = readTarget(target)
                if (current?.let(::sha256) != proposal.proposedSha256) {
                    return@withContext markConflict(
                        proposal,
                        "File changed after Accept; rollback would overwrite newer work",
                    )
                }
                val rollingBack = proposal.copy(state = FileReviewState.ROLLING_BACK, updatedAtMs = clock())
                replace(rollingBack)
                persistJournal()
                try {
                    restoreOriginal(proposal, target)
                    val rolledBack = rollingBack.copy(
                        state = FileReviewState.ROLLED_BACK,
                        updatedAtMs = clock(),
                        rollbackAvailable = false,
                    )
                    replace(rolledBack)
                    persistJournal()
                    FileReviewActionResult.Success(rolledBack)
                } catch (e: Exception) {
                    val recovery = rollingBack.copy(state = FileReviewState.RECOVERY_REQUIRED, updatedAtMs = clock())
                    replace(recovery)
                    persistJournal()
                    FileReviewActionResult.Failure(e.message ?: "Rollback failed")
                }
            }
        }
    }

    private fun restoreOriginal(proposal: FileReviewProposal, target: File) {
        val current = readTarget(target)
        val currentHash = current?.let(::sha256)
        if (currentHash == proposal.originalSha256) return
        if (currentHash != proposal.proposedSha256) {
            throw IllegalStateException("Target changed during recovery; backup preserved")
        }
        val backup = originalFile(proposal.id)
        if (proposal.originalSha256 == null) {
            if (target.exists()) Files.delete(target.toPath())
        } else {
            val bytes = backup.readBytes()
            if (sha256(bytes) != proposal.originalSha256) throw IllegalStateException("Staged original hash is invalid")
            writeTargetAtomically(target, bytes, replaceExisting = target.exists())
        }
    }

    private fun writeTargetAtomically(target: File, bytes: ByteArray, replaceExisting: Boolean) {
        val parent = target.parentFile ?: error("Target has no parent")
        if (!parent.isDirectory || Files.isSymbolicLink(parent.toPath())) error("Target parent is unsafe")
        val temp = File(parent, ".${target.name}.${UUID.randomUUID()}.kai-tmp")
        val permissions = if (target.exists()) {
            runCatching {
                Files.getPosixFilePermissions(target.toPath(), LinkOption.NOFOLLOW_LINKS)
            }.getOrNull()
        } else {
            null
        }
        try {
            writeSynced(temp, bytes)
            if (sha256(temp.readBytes()) != sha256(bytes)) error("Temporary file verification failed")
            if (permissions != null) runCatching { Files.setPosixFilePermissions(temp.toPath(), permissions) }
            mover.move(temp, target, replaceExisting)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun writeAtomic(destination: File, bytes: ByteArray, replaceExisting: Boolean) {
        val parent = destination.parentFile ?: error("Staging path has no parent")
        if (!parent.exists() && !parent.mkdirs()) error("Cannot create staging directory")
        val temp = File(parent, ".${destination.name}.${UUID.randomUUID()}.tmp")
        try {
            writeSynced(temp, bytes)
            mover.move(temp, destination, replaceExisting)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun writeSynced(file: File, bytes: ByteArray) {
        FileOutputStream(file).use { stream ->
            stream.write(bytes)
            stream.flush()
            stream.fd.sync()
        }
    }

    private fun readTarget(file: File): ByteArray? {
        if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return null
        val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        if (attrs.isSymbolicLink || !attrs.isRegularFile) {
            throw IllegalArgumentException("Target is not a regular non-symlink file")
        }
        if (attrs.size() > FILE_REVIEW_MAX_BYTES) throw IllegalArgumentException("Target exceeds the 512 KiB limit")
        return Files.readAllBytes(file.toPath())
    }

    private fun recoverApplyingTransactions() {
        var changed = false
        _proposals.value = _proposals.value.map { proposal ->
            if (proposal.state != FileReviewState.APPLYING && proposal.state != FileReviewState.ROLLING_BACK) {
                return@map proposal
            }
            val target = fileSystem.resolve(proposal.workspace, proposal.path)
            val currentResult = target?.let { runCatching { readTarget(it) } }
            val currentHash = if (currentResult?.isSuccess == true) {
                currentResult.getOrNull()?.let(::sha256)
            } else {
                "<unsafe-target>"
            }
            val recovered = when (proposal.state) {
                FileReviewState.APPLYING -> when {
                    currentHash == proposal.proposedSha256 -> proposal.copy(
                        state = FileReviewState.ACCEPTED,
                        rollbackAvailable = true,
                        updatedAtMs = clock(),
                    )

                    currentHash == proposal.originalSha256 -> proposal.copy(
                        state = FileReviewState.FAILED,
                        updatedAtMs = clock(),
                    )

                    else -> proposal.copy(state = FileReviewState.CONFLICT, updatedAtMs = clock())
                }

                FileReviewState.ROLLING_BACK -> when {
                    currentHash == proposal.originalSha256 -> proposal.copy(
                        state = FileReviewState.ROLLED_BACK,
                        rollbackAvailable = false,
                        updatedAtMs = clock(),
                    )

                    currentHash == proposal.proposedSha256 -> proposal.copy(
                        state = FileReviewState.ACCEPTED,
                        rollbackAvailable = true,
                        updatedAtMs = clock(),
                    )

                    else -> proposal.copy(state = FileReviewState.CONFLICT, updatedAtMs = clock())
                }

                else -> proposal
            }
            changed = true
            recovered
        }
        if (changed) persistJournal()
    }

    private fun cleanupExpired() {
        val now = clock()
        var changed = false
        _proposals.value = _proposals.value.map { proposal ->
            when {
                proposal.state == FileReviewState.PROPOSED && now - proposal.createdAtMs > PENDING_TTL_MS -> {
                    originalFile(proposal.id).delete()
                    proposedFile(proposal.id).delete()
                    changed = true
                    proposal.copy(
                        state = FileReviewState.REJECTED,
                        updatedAtMs = now,
                        rollbackAvailable = false,
                    )
                }

                proposal.state == FileReviewState.ACCEPTED &&
                    proposal.rollbackAvailable &&
                    now - proposal.updatedAtMs > BACKUP_TTL_MS -> {
                    originalFile(proposal.id).delete()
                    proposedFile(proposal.id).delete()
                    changed = true
                    proposal.copy(rollbackAvailable = false)
                }

                else -> proposal
            }
        }
        if (changed) persistJournal()
    }

    private fun markConflict(proposal: FileReviewProposal, reason: String): FileReviewActionResult.Conflict {
        val conflict = proposal.copy(state = FileReviewState.CONFLICT, updatedAtMs = clock())
        replace(conflict)
        persistJournal()
        return FileReviewActionResult.Conflict(conflict, reason)
    }

    private fun find(id: String): FileReviewProposal? = _proposals.value.firstOrNull { it.id == id }

    private fun replace(proposal: FileReviewProposal) {
        _proposals.value = _proposals.value.map { if (it.id == proposal.id) proposal else it }
    }

    private fun originalFile(id: String) = File(stagingRoot, "$id.original")
    private fun proposedFile(id: String) = File(stagingRoot, "$id.proposed")

    private fun persistJournal() {
        writeAtomic(
            journal,
            json.encodeToString(_proposals.value).toByteArray(Charsets.UTF_8),
            replaceExisting = journal.exists(),
        )
    }

    private fun encodeStrictUtf8(value: String): ByteArray {
        val buffer = StandardCharsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(java.nio.CharBuffer.wrap(value))
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        return result
    }

    private fun decodeStrictUtf8(bytes: ByteArray): String = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

    private fun isStrictUtf8(bytes: ByteArray): Boolean = runCatching { decodeStrictUtf8(bytes) }.isSuccess

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }

    companion object {
        private const val MAX_STAGING_BYTES = 10L * 1024 * 1024
        private const val PENDING_TTL_MS = 24L * 60 * 60 * 1000
        private const val BACKUP_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
