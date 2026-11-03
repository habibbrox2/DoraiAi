package com.inspiredandroid.kai.ocr

/**
 * Simple Rect class for cross-platform use.
 */
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * OCR Language support.
 */
enum class OcrLanguage {
    LATIN, // Latin script (English, etc.)
    CHINESE, // Chinese (Simplified/Traditional)
    DEVANAGARI, // Devanagari script (Hindi, etc.)
    JAPANESE, // Japanese
    KOREAN, // Korean
    AUTO, // Auto-detect (Latin fallback)
}

/**
 * OCR Result containing recognized text and metadata.
 */
data class OcrResult(
    val text: String,
    val language: OcrLanguage,
    val confidence: Float,
    val blocks: List<OcrBlock>,
)

/**
 * Recognized text block with bounding box.
 */
data class OcrBlock(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float,
    val lines: List<OcrLine>,
)

/**
 * Recognized text line.
 */
data class OcrLine(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float,
    val elements: List<OcrElement>,
)

/**
 * Recognized text element (word/symbol).
 */
data class OcrElement(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float,
)
