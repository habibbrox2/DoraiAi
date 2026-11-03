package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import kotlinx.coroutines.flow.Flow

/**
 * Workflow Tool - Execute multi-step tool workflows from natural language.
 * Enables chaining multiple tools together for complex operations.
 */
object WorkflowTool : Tool {

    override val schema = ToolSchema(
        name = "workflow",
        description = "Execute a multi-step workflow by describing what you want to accomplish. The system will plan and execute the necessary tool sequence.",
        parameters = mapOf(
            "instruction" to ParameterSchema(
                type = "string",
                description = "Natural language description of what to accomplish (e.g., 'Find my latest downloaded PDF and extract text from it')",
                required = true,
            ),
            "maxSteps" to ParameterSchema(
                type = "integer",
                description = "Maximum number of tool steps to execute (default 5, max 10)",
                required = false,
            ),
            "confirmEachStep" to ParameterSchema(
                type = "boolean",
                description = "Whether to ask for confirmation before each step (default false)",
                required = false,
            ),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any = try {
        mapOf(
            "success" to false,
            "error" to "This tool must be implemented platform-specifically",
        )
    } catch (e: Exception) {
        mapOf(
            "success" to false,
            "error" to "Failed to execute workflow: ${e.message}",
        )
    }

    val toolInfo = ToolInfo(
        id = "workflow",
        name = "Workflow",
        description = "Execute multi-step workflows from natural language",
        nameRes = null,
        descriptionRes = null,
    )
}

/**
 * Result of a workflow execution.
 */
data class WorkflowResult(
    val success: Boolean,
    val steps: List<WorkflowStepResult> = emptyList(),
    val finalResult: String = "",
    val error: String? = null,
)

data class WorkflowStepResult(
    val stepNumber: Int,
    val toolName: String,
    val description: String,
    val success: Boolean,
    val result: String = "",
    val error: String? = null,
)

/**
 * Tool Parameter Extraction - Extract structured parameters from natural language.
 * This is a helper tool for the workflow system.
 */
object ParameterExtractionTool : Tool {

    override val schema = ToolSchema(
        name = "extract_parameters",
        description = "Extract structured parameters from natural language text for a specific tool schema",
        parameters = mapOf(
            "text" to ParameterSchema(
                type = "string",
                description = "Natural language text to extract parameters from",
                required = true,
            ),
            "toolSchema" to ParameterSchema(
                type = "object",
                description = "JSON schema of the target tool's parameters",
                required = true,
            ),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any = try {
        mapOf(
            "success" to false,
            "error" to "This tool must be implemented platform-specifically",
        )
    } catch (e: Exception) {
        mapOf(
            "success" to false,
            "error" to "Failed to extract parameters: ${e.message}",
        )
    }

    val toolInfo = ToolInfo(
        id = "extract_parameters",
        name = "Extract Parameters",
        description = "Extract structured parameters from natural language",
        nameRes = null,
        descriptionRes = null,
    )
}

/**
 * Tool Result Summarization - Generate human-readable summaries of tool results.
 */
object ResultSummarizationTool : Tool {

    override val schema = ToolSchema(
        name = "summarize_result",
        description = "Generate a human-readable summary of a tool's result for the user",
        parameters = mapOf(
            "toolName" to ParameterSchema(
                type = "string",
                description = "Name of the tool that produced the result",
                required = true,
            ),
            "result" to ParameterSchema(
                type = "object",
                description = "The raw result object from the tool",
                required = true,
            ),
            "context" to ParameterSchema(
                type = "string",
                description = "Optional context about what the user was trying to achieve",
                required = false,
            ),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any = try {
        mapOf(
            "success" to false,
            "error" to "This tool must be implemented platform-specifically",
        )
    } catch (e: Exception) {
        mapOf(
            "success" to false,
            "error" to "Failed to summarize result: ${e.message}",
        )
    }

    val toolInfo = ToolInfo(
        id = "summarize_result",
        name = "Summarize Result",
        description = "Generate human-readable summary of tool results",
        nameRes = null,
        descriptionRes = null,
    )
}

/**
 * Workflow step definition for planning.
 */
data class WorkflowStep(
    val toolName: String,
    val description: String,
    val parameters: Map<String, Any>,
    val dependsOn: List<Int> = emptyList(), // Step indices this depends on
)

/**
 * Workflow plan - a sequence of steps to execute.
 */
data class WorkflowPlan(
    val steps: List<WorkflowStep>,
    val estimatedSteps: Int,
    val description: String,
)

/**
 * Expected tool result for planning.
 */
data class ExpectedToolResult(
    val toolName: String,
    val expectedOutputType: String,
    val requiredForNextSteps: Boolean = true,
)
