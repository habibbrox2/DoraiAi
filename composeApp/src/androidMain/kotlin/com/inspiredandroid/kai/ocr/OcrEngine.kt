package com.inspiredandroid.kai.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Rect as AndroidRect

private fun AndroidRect?.toRect(): Rect = if (this == null) Rect(0, 0, 0, 0) else Rect(left, top, right, bottom)

/**
 * OCR Engine using ML Kit Text Recognition v2.
 * Supports multiple languages: Latin, Chinese, Devanagari, Japanese, Korean.
 */
class OcrEngine(private val context: Context) {

    private val recognizers = mutableMapOf<OcrLanguage, TextRecognizer>()

    /**
     * Gets or creates a TextRecognizer for the specified language.
     */
    private fun getRecognizer(language: OcrLanguage): TextRecognizer = recognizers.getOrPut(language) {
        when (language) {
            OcrLanguage.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            OcrLanguage.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            OcrLanguage.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            OcrLanguage.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            OcrLanguage.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            OcrLanguage.AUTO -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    /**
     * Performs OCR on a Bitmap image.
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        language: OcrLanguage = OcrLanguage.AUTO,
    ): OcrResult = withContext(Dispatchers.IO) {
        val recognizer = getRecognizer(language)
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = Tasks.await(recognizer.process(image))

        val blocks = result.textBlocks.map { block ->
            OcrBlock(
                text = block.text,
                boundingBox = block.boundingBox.toRect(),
                confidence = 1.0f,
                lines = block.lines.map { line ->
                    OcrLine(
                        text = line.text,
                        boundingBox = line.boundingBox.toRect(),
                        confidence = 1.0f,
                        elements = line.elements.map { element ->
                            OcrElement(
                                text = element.text,
                                boundingBox = element.boundingBox.toRect(),
                                confidence = 1.0f,
                            )
                        },
                    )
                },
            )
        }

        OcrResult(
            text = result.text,
            language = language,
            confidence = 1.0f,
            blocks = blocks,
        )
    }

    /**
     * Recognizes text from a file URI using SAF or file path.
     */
    suspend fun recognizeFromUri(
        uri: android.net.Uri,
        language: OcrLanguage = OcrLanguage.AUTO,
    ): OcrResult = withContext(Dispatchers.IO) {
        val recognizer = getRecognizer(language)
        val image = InputImage.fromFilePath(context, uri)
        val result = Tasks.await(recognizer.process(image))

        val blocks = result.textBlocks.map { block ->
            OcrBlock(
                text = block.text,
                boundingBox = block.boundingBox.toRect(),
                confidence = 1.0f,
                lines = block.lines.map { line ->
                    OcrLine(
                        text = line.text,
                        boundingBox = line.boundingBox.toRect(),
                        confidence = 1.0f,
                        elements = line.elements.map { element ->
                            OcrElement(
                                text = element.text,
                                boundingBox = element.boundingBox.toRect(),
                                confidence = 1.0f,
                            )
                        },
                    )
                },
            )
        }

        OcrResult(
            text = result.text,
            language = language,
            confidence = 1.0f,
            blocks = blocks,
        )
    }

    /**
     * Closes all recognizers and releases resources.
     */
    fun close() {
        recognizers.values.forEach { it.close() }
        recognizers.clear()
    }

    /**
     * Checks if a language is supported on this device.
     */
    fun isLanguageSupported(language: OcrLanguage): Boolean = when (language) {
        OcrLanguage.LATIN -> true
        OcrLanguage.CHINESE -> true
        OcrLanguage.DEVANAGARI -> true
        OcrLanguage.JAPANESE -> true
        OcrLanguage.KOREAN -> true
        OcrLanguage.AUTO -> true
    }

    /**
     * Gets the recommended language for a given text sample.
     */
    fun detectLanguage(text: String): OcrLanguage {
        // Simple heuristic based on Unicode ranges
        return when {
            text.matches(".*[\\u4e00-\\u9fff].*".toRegex()) -> OcrLanguage.CHINESE
            text.matches(".*[\\u0900-\\u097f].*".toRegex()) -> OcrLanguage.DEVANAGARI
            text.matches(".*[\\u3040-\\u309f\\u30a0-\\u30ff].*".toRegex()) -> OcrLanguage.JAPANESE
            text.matches(".*[\\uac00-\\ud7af].*".toRegex()) -> OcrLanguage.KOREAN
            else -> OcrLanguage.LATIN
        }
    }
}

/**
 * OCR Engine factory for dependency injection.
 */
class OcrEngineFactory(private val context: Context) {
    private var engine: OcrEngine? = null

    fun getEngine(): OcrEngine = synchronized(this) {
        engine ?: OcrEngine(context).also { engine = it }
    }

    fun close() {
        engine?.close()
        engine = null
    }
}
