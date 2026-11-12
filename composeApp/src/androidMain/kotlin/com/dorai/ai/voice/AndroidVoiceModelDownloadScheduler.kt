package com.dorai.ai.voice

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dorai.ai.data.AppSettings
import org.koin.java.KoinJavaComponent
import java.io.File

/**
 * Enqueue and control the voice model download. Unique work with KEEP so an
 * already-running download is not broken; a new request for the same model is ignored.
 * Wi-Fi-only by default; mobile data requires the user to opt in first.
 */
internal object AndroidVoiceModelDownloadScheduler {

    fun enqueue(context: Context, modelId: String, mobileDataAllowed: Boolean): Boolean {
        if (findCatalogVoiceModelById(modelId)?.isDownloadable() != true) return false
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (mobileDataAllowed) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .build()
        val request = OneTimeWorkRequestBuilder<AndroidVoiceModelDownloadWorker>()
            .setInputData(workDataOf(AndroidVoiceModelDownloadWorker.KEY_MODEL to modelId))
            .setConstraints(constraints)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            AndroidVoiceModelDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
        return true
    }

    /** Cancels the download but keeps `.part` files; resume continues where it stopped. */
    fun pause(context: Context, modelId: String) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(AndroidVoiceModelDownloadWorker.WORK_NAME)
        markPaused(context, modelId)
    }

    /** Cancels the download and marks the model absent; files stay on disk until [delete]. */
    fun delete(context: Context, modelId: String): Boolean {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(AndroidVoiceModelDownloadWorker.WORK_NAME)
        val dir = File(context.applicationContext.filesDir, "models/voice/$modelId")
        val deleted = dir.deleteRecursively()
        val appSettings: AppSettings by KoinJavaComponent.inject(AppSettings::class.java)
        VoiceModelRepository(
            appSettings::getVoiceModelManifestJson,
            appSettings::setVoiceModelManifestJson,
        ).deleteEntry(modelId)
        return deleted
    }

    /** Re-download from zero: deletes the model directory and manifest row. */
    fun reset(context: Context, modelId: String): Boolean = delete(context, modelId)

    private fun markPaused(context: Context, modelId: String) {
        val appSettings: AppSettings by KoinJavaComponent.inject(AppSettings::class.java)
        val repository = VoiceModelRepository(
            appSettings::getVoiceModelManifestJson,
            appSettings::setVoiceModelManifestJson,
        )
        val entry = repository.entry(modelId)
        if (entry != null && (entry.state == VoiceModelState.DOWNLOADING || entry.state == VoiceModelState.PAUSED)) {
            repository.setModelState(
                modelId,
                VoiceModelState.PAUSED,
                downloadedBytes = entry.downloadedBytes,
            )
        }
    }
}
