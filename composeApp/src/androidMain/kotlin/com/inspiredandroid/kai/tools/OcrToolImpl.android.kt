package com.inspiredandroid.kai.tools

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.ocr.OcrEngine
import com.inspiredandroid.kai.ocr.OcrLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/**
 * Android implementation of the OCR tool using ML Kit Text Recognition v2.
 */
class OcrToolImpl : Tool {
    private val context: Context by inject(Context::class.java)

    override val schema = OcrTool.schema

    override suspend fun execute(args: Map<String, Any>): Any {
        val imageUriString = (args["imageUri"] as? String)?.trim()
            ?: return mapOf("success" to false, "error" to "imageUri is required")
        val languageStr = args["language"] as? String ?: "AUTO"
        val returnBlocks = (args["returnBlocks"] as? Boolean)
            ?: (args["returnBlocks"]?.toString()?.lowercase() == "true")

        val language = runCatching { OcrLanguage.valueOf(languageStr.uppercase()) }
            .getOrElse { return mapOf("success" to false, "error" to "Unknown language: $languageStr") }

        return withContext(Dispatchers.IO) {
            var bitmap: android.graphics.Bitmap? = null
            try {
                val openStream = {
                    if (imageUriString.startsWith("/")) {
                        java.io.File(imageUriString).inputStream()
                    } else {
                        context.contentResolver.openInputStream(Uri.parse(imageUriString))
                    }
                }

                // Decode bounds first to avoid OutOfMemoryError on large images
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openStream()?.use { BitmapFactory.decodeStream(it, null, options) }

                val maxDimension = 2048
                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                bitmap = openStream()?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
                    ?: throw IllegalArgumentException("Failed to decode image from: $imageUriString")

                val ocrEngine = OcrEngine(context)
                val result = try {
                    ocrEngine.recognizeText(bitmap, language)
                } finally {
                    ocrEngine.close()
                }

                val blocks = result.blocks.map { block ->
                    mapOf(
                        "text" to block.text,
                        "boundingBox" to mapOf(
                            "left" to block.boundingBox.left,
                            "top" to block.boundingBox.top,
                            "right" to block.boundingBox.right,
                            "bottom" to block.boundingBox.bottom,
                        ),
                        "confidence" to block.confidence,
                        "lines" to block.lines.map { line ->
                            mapOf(
                                "text" to line.text,
                                "boundingBox" to mapOf(
                                    "left" to line.boundingBox.left,
                                    "top" to line.boundingBox.top,
                                    "right" to line.boundingBox.right,
                                    "bottom" to line.boundingBox.bottom,
                                ),
                                "confidence" to line.confidence,
                                "elements" to line.elements.map { element ->
                                    mapOf(
                                        "text" to element.text,
                                        "boundingBox" to mapOf(
                                            "left" to element.boundingBox.left,
                                            "top" to element.boundingBox.top,
                                            "right" to element.boundingBox.right,
                                            "bottom" to element.boundingBox.bottom,
                                        ),
                                        "confidence" to element.confidence,
                                    )
                                },
                            )
                        },
                    )
                }

                mapOf(
                    "success" to true,
                    "text" to result.text,
                    "language" to result.language.name,
                    "confidence" to result.confidence,
                    "blocks" to if (returnBlocks) blocks else emptyList<Map<String, Any>>(),
                )
            } catch (t: Throwable) {
                mapOf(
                    "success" to false,
                    "error" to "OCR failed: ${t.message ?: t::class.simpleName}",
                )
            } finally {
                bitmap?.recycle()
            }
        }
    }
}
