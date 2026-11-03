package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.filereview.FileReviewActionResult
import com.inspiredandroid.kai.filereview.FileReviewController
import com.inspiredandroid.kai.filereview.FileReviewWorkspace
import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolSchema

/**
 * Model-facing tools can only stage a proposal. They deliberately expose no
 * accept/reject/rollback operation; those transitions require a user UI action.
 */
object FileReviewTools {
    fun getTools(controller: FileReviewController): List<Tool> = listOf(
        object : Tool {
            override val schema = ToolSchema(
                name = "propose_file_change",
                description = "Stage a text file creation or update in the shared project workspace for user review. This does not modify the target file. Use a workspace-relative path such as src/Main.kt. Never use absolute paths.",
                parameters = mapOf(
                    "path" to ParameterSchema("string", "Project-workspace-relative file path.", required = true),
                    "content" to ParameterSchema("string", "Complete proposed UTF-8 text content.", required = true),
                ),
            )

            override suspend fun execute(args: Map<String, Any>): Any {
                val path = args["path"] as? String
                    ?: return failure("Missing path")
                val content = args["content"] as? String
                    ?: return failure("Missing content")
                return result(controller.proposeTextChange(FileReviewWorkspace.PROJECT, path, content))
            }
        },
        object : Tool {
            override val schema = ToolSchema(
                name = "propose_file_delete",
                description = "Stage deletion of a file in the shared project workspace for user review. This does not delete the target file. Use a workspace-relative path. Never use absolute paths.",
                parameters = mapOf(
                    "path" to ParameterSchema("string", "Project-workspace-relative file path.", required = true),
                ),
            )

            override suspend fun execute(args: Map<String, Any>): Any {
                val path = args["path"] as? String
                    ?: return failure("Missing path")
                return result(controller.proposeDelete(FileReviewWorkspace.PROJECT, path))
            }
        },
    )

    private fun result(action: FileReviewActionResult): Map<String, Any?> = when (action) {
        is FileReviewActionResult.Success -> mapOf(
            "success" to true,
            "proposal_id" to action.proposal.id,
            "path" to action.proposal.path,
            "operation" to action.proposal.operation.name,
            "state" to action.proposal.state.name,
            "message" to "Proposal staged. The target file has not been changed. Ask the user to review it in the Agent Activity timeline.",
        )

        is FileReviewActionResult.Conflict -> mapOf(
            "success" to false,
            "proposal_id" to action.proposal.id,
            "state" to action.proposal.state.name,
            "error" to action.reason,
        )

        is FileReviewActionResult.Failure -> failure(action.reason)

        FileReviewActionResult.NotFound -> failure("Proposal not found")
    }

    private fun failure(message: String): Map<String, Any?> = mapOf("success" to false, "error" to message)
}




