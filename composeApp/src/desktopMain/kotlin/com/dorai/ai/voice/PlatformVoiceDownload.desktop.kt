package com.dorai.ai.voice

/** Desktop has no voice model downloader; the voice entry points stay hidden. */
internal actual fun enqueueVoiceDownload(modelId: String, mobileDataAllowed: Boolean): Boolean = false

internal actual fun cancelVoiceDownload(modelId: String) = Unit

internal actual fun deleteVoiceModelFiles(modelId: String): Boolean = false
