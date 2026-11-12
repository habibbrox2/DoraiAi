package com.dorai.ai.voice

/**
 * Platform bridge for starting, stopping and deleting a voice model download.
 * Only Android has a real downloader; the other targets report that no download is
 * possible, and the settings UI keeps the button hidden there.
 */
internal expect fun enqueueVoiceDownload(modelId: String, mobileDataAllowed: Boolean): Boolean

internal expect fun cancelVoiceDownload(modelId: String)

internal expect fun deleteVoiceModelFiles(modelId: String): Boolean
