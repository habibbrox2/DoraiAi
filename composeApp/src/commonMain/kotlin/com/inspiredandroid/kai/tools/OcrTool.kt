package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import com.inspiredandroid.kai.ocr.OcrLanguage
import com.inspiredandroid.kai.ocr.OcrResult
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.tool_ocr_description
import kai.composeapp.generated.resources.tool_ocr_name
import kotlinx.coroutines.flow.Flow

/**
 * OCR Tool - Extract text from images using on-device ML Kit Text Recognition v2.
 * Supports multiple languages: Latin, Chinese, Devanagari, Japanese, Korean.
 */
object OcrTool : Tool {

    override val schema = ToolSchema(
        name = "ocr",
        description = "Extract text from an image using on-device OCR. Supports multiple languages: Latin, Chinese, Devanagari, Japanese, Korean.",
        parameters = mapOf(
            "imageUri" to ParameterSchema(
                type = "string",
                description = "Content URI or file path of the image to process",
                required = true,
            ),
            "language" to ParameterSchema(
                type = "string",
                description = "OCR language: AUTO, LATIN, CHINESE, DEVANAGARI, JAPANESE, KOREAN",
                required = false,
            ),
            "returnBlocks" to ParameterSchema(
                type = "boolean",
                description = "Whether to return detailed block/line/element structure",
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
            "error" to "Failed to perform OCR: ${e.message}",
        )
    }

    val toolInfo = ToolInfo(
        id = "ocr",
        name = "OCR",
        description = "Extract text from images using on-device OCR",
        nameRes = Res.string.tool_ocr_name,
        descriptionRes = Res.string.tool_ocr_description,
    )
}

/**
 * OCR Tool result.
 */
data class OcrToolResult(
    val success: Boolean,
    val text: String = "",
    val language: String = "",
    val confidence: Float = 0f,
    val blocks: List<OcrBlockResult> = emptyList(),
    val error: String? = null,
)

data class OcrBlockResult(
    val text: String,
    val boundingBox: Map<String, Int> = emptyMap(),
    val confidence: Float = 0f,
    val lines: List<OcrLineResult> = emptyList(),
)

data class OcrLineResult(
    val text: String,
    val boundingBox: Map<String, Int> = emptyMap(),
    val confidence: Float = 0f,
    val elements: List<OcrElementResult> = emptyList(),
)

data class OcrElementResult(
    val text: String,
    val boundingBox: Map<String, Int> = emptyMap(),
    val confidence: Float = 0f,
)
