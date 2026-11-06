package com.dorai.ai.inference

import android.content.Context
import android.os.Build
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Content
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.SystemInstruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adapter implementing [LocalRuntime] for Google Gemini Nano via Android AICore / ML Kit GenAI.
 * Available strictly in the `playStore` build flavor on supported hardware.
 */
class AICoreRuntime(
    context: Context,
    private val consentManager: AICoreConsentManager = AICoreConsentManager(context),
) : LocalRuntime {

    override val id: LocalRuntimeId = LocalRuntimeId.AICORE

    private var modelInstance: GenerativeModel? = null

    private fun getOrCreateModel(): GenerativeModel {
        var model = modelInstance
        if (model == null) {
            model = Generation.getClient()
            modelInstance = model
        }
        return model
    }

    override suspend fun availability(): RuntimeAvailability = withContext(Dispatchers.IO) {
        if (!consentManager.isConsentGranted) {
            return@withContext RuntimeAvailability.DEVICE_UNSUPPORTED
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext RuntimeAvailability.OS_TOO_OLD
        }
        try {
            val model = getOrCreateModel()
            when (model.checkStatus()) {
                FeatureStatus.AVAILABLE -> RuntimeAvailability.AVAILABLE
                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> RuntimeAvailability.NEEDS_DOWNLOAD
                FeatureStatus.UNAVAILABLE -> RuntimeAvailability.DEVICE_UNSUPPORTED
                else -> RuntimeAvailability.DEVICE_UNSUPPORTED
            }
        } catch (_: GenAiException) {
            RuntimeAvailability.DEVICE_UNSUPPORTED
        } catch (_: Throwable) {
            RuntimeAvailability.DEVICE_UNSUPPORTED
        }
    }

    override suspend fun capabilities(modelId: String): LocalModelCapabilities = LocalModelCapabilities(
        supportsFunctionCalling = false,
        supportsThinking = false,
        supportsVision = false,
        supportsAudio = false,
        sampler = null,
    )

    override suspend fun chat(request: LocalChatRequest): LocalChatResult = withContext(Dispatchers.IO) {
        if (!consentManager.isConsentGranted) {
            throw IllegalStateException("AICore execution rejected: user consent has not been granted.")
        }
        val model = getOrCreateModel()
        val contents = ArrayList<Content>()

        for (msg in request.messages) {
            val isModel = msg.role.equals("model", ignoreCase = true) ||
                msg.role.equals("assistant", ignoreCase = true)
            val role = if (isModel) Content.Role.MODEL else Content.Role.USER
            val content = Content.Builder()
                .role(role)
                .text(msg.content)
                .build()
            contents.add(content)
        }

        val requestBuilder = GenerateContentRequest.Builder(contents)
        request.systemPrompt?.takeIf { it.isNotBlank() }?.let { prompt ->
            requestBuilder.systemInstruction = SystemInstruction(prompt)
        }

        try {
            val response = model.generateContent(requestBuilder.build())
            val output = response.candidates.firstOrNull()?.text.orEmpty()
            LocalChatResult(content = output)
        } catch (e: GenAiException) {
            throw IllegalStateException("AICore inference failed with code ${e.errorCode}: ${e.message}", e)
        }
    }

    override fun releaseInBackground() {
        try {
            modelInstance?.close()
        } catch (_: Throwable) {
            // Safe cleanup
        } finally {
            modelInstance = null
        }
    }
}
