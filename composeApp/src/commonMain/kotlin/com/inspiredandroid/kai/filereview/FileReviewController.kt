package com.inspiredandroid.kai.filereview

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

const val FILE_REVIEW_MAX_BYTES: Long = 512_000L

@Serializable
enum class FileReviewWorkspace {
    PROJECT,
}

@Serializable
enum class FileReviewOperation {
    CREATE,
    UPDATE,
    DELETE,
}

@Serializable
enum class FileReviewState {
    PROPOSED,
    APPLYING,
    ROLLING_BACK,
    ACCEPTED,
    REJECTED,
    CONFLICT,
    FAILED,
    RECOVERY_REQUIRED,
    ROLLED_BACK,
}

@Serializable
data class FileReviewProposal(
    val id: String,
    val workspace: FileReviewWorkspace,
    val path: String,
    val operation: FileReviewOperation,
    val originalSha256: String?,
    val proposedSha256: String?,
    val originalBytes: Long,
    val proposedBytes: Long,
    val state: FileReviewState,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val rollbackAvailable: Boolean = false,
)

data class FileReviewPreview(
    val originalText: String?,
    val proposedText: String?,
)

sealed interface FileReviewActionResult {
    data class Success(val proposal: FileReviewProposal) : FileReviewActionResult
    data class Conflict(val proposal: FileReviewProposal, val reason: String) : FileReviewActionResult
    data class Failure(val reason: String) : FileReviewActionResult
    data object NotFound : FileReviewActionResult
}

interface FileReviewController {
    val proposals: StateFlow<List<FileReviewProposal>>

    suspend fun initialize()
    suspend fun proposeTextChange(
        workspace: FileReviewWorkspace,
        path: String,
        content: String,
    ): FileReviewActionResult
    suspend fun proposeDelete(workspace: FileReviewWorkspace, path: String): FileReviewActionResult
    suspend fun preview(proposalId: String): FileReviewPreview?
    suspend fun accept(proposalId: String): FileReviewActionResult
    suspend fun reject(proposalId: String): FileReviewActionResult
    suspend fun rollback(proposalId: String): FileReviewActionResult
}

class NoOpFileReviewController : FileReviewController {
    override val proposals: StateFlow<List<FileReviewProposal>> = MutableStateFlow(emptyList())
    override suspend fun initialize() {}
    override suspend fun proposeTextChange(workspace: FileReviewWorkspace, path: String, content: String) = FileReviewActionResult.Failure("File Review is unavailable on this platform")
    override suspend fun proposeDelete(workspace: FileReviewWorkspace, path: String) = FileReviewActionResult.Failure("File Review is unavailable on this platform")
    override suspend fun preview(proposalId: String): FileReviewPreview? = null
    override suspend fun accept(proposalId: String) = FileReviewActionResult.Failure("File Review is unavailable on this platform")
    override suspend fun reject(proposalId: String) = FileReviewActionResult.Failure("File Review is unavailable on this platform")
    override suspend fun rollback(proposalId: String) = FileReviewActionResult.Failure("File Review is unavailable on this platform")
}




