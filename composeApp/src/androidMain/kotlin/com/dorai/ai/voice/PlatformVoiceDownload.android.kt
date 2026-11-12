package com.dorai.ai.voice

import android.content.Context
import org.koin.java.KoinJavaComponent

/**
 * Android actuals for starting, cancelling and deleting a voice model download.
 */
internal actual fun enqueueVoiceDownload(modelId: String, mobileDataAllowed: Boolean): Boolean {
    val context: Context by KoinJavaComponent.inject(Context::class.java)
    return AndroidVoiceModelDownloadScheduler.enqueue(context, modelId, mobileDataAllowed)
}

internal actual fun cancelVoiceDownload(modelId: String) {
    val context: Context by KoinJavaComponent.inject(Context::class.java)
    AndroidVoiceModelDownloadScheduler.pause(context, modelId)
}

internal actual fun deleteVoiceModelFiles(modelId: String): Boolean {
    val context: Context by KoinJavaComponent.inject(Context::class.java)
    return AndroidVoiceModelDownloadScheduler.delete(context, modelId)
}
