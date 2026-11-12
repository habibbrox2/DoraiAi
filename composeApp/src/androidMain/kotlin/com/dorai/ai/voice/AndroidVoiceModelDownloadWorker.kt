package com.dorai.ai.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.StatFs
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dorai.ai.data.AppSettings
import org.koin.java.KoinJavaComponent
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a pinned voice model into app-private storage.
 *
 * Per-file flow:
 *  1. storage gate: free space must cover the model plus 25% headroom;
 *  2. if the final file already exists and its digest matches, skip it;
 *  3. resume a `.part` from its length, re-hashing the kept bytes to rebuild the digest;
 *  4. `206` keeps the resume, `200`/`416` restart from zero;
 *  5. stream-to-file with a running SHA-256;
 *  6. length and digest are checked before the file is renamed to its final name;
 *  7. a sibling `<name>.sha256` marker is written next to the verified file.
 *
 * Nothing is usable until every file passed verification and the manifest says READY.
 */
internal class AndroidVoiceModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL) ?: return fail("missing model id")
        val entry = findCatalogVoiceModelById(modelId) ?: return fail("unknown model id")
        if (!entry.isDownloadable()) {
            return fail("model requires license sign-off before it can be downloaded")
        }

        val appContext = applicationContext
        val storageDir = File(appContext.filesDir, "models/voice/$modelId")
        if (!storageDir.mkdirs() && !storageDir.isDirectory) {
            return fail("unable to create model directory")
        }

        val repository = voiceRepository()

        // Pre-download storage gate: sizeBytes + 25% headroom.
        val free = StatFs(storageDir.path).availableBytes
        if (storageGate(entry, free) is StorageGateResult.Refused) {
            repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = "not enough free space")
            return fail("not enough free space for the model")
        }

        ensureNotificationChannel(appContext)
        setForeground(
            foregroundInfo(downloadNotification(appContext, entry.displayName, 0, entry.totalSizeBytes)),
        )

        var cumulativeDownloaded = 0L
        val verifiedNames = mutableListOf<String>()

        for (file in entry.files) {
            val target = File(storageDir, file.name)
            val verified = downloadFile(appContext, entry, file, target, modelId, repository) ?: run {
                return fail(repository.entry(modelId)?.failedReason ?: "download failed")
            }
            verifiedNames += file.name
            cumulativeDownloaded += file.sizeBytes
        }

        repository.setVerifiedFiles(modelId, verifiedNames)
        return Result.success(
            workDataOf(
                KEY_DOWNLOADED to cumulativeDownloaded,
                KEY_FILES to verifiedNames.joinToString(","),
            ),
        )
    }

    /** Downloads and verifies one file. Returns its final name on success, null on failure. */
    private suspend fun downloadFile(
        context: Context,
        entry: VoiceModel,
        file: VoiceModelFile,
        target: File,
        modelId: String,
        repository: VoiceModelRepository,
    ): String? {
        val storageDir = target.parentFile!!
        val partial = File(storageDir, "${file.name}.part")

        if (target.exists() && digestOfFile(target) == file.sha256) {
            repository.setModelState(modelId, VoiceModelState.DOWNLOADING, downloadedBytes = cumulativeOf(storageDir, entry))
            return file.name
        }
        target.delete()
        File(storageDir, "${file.name}.sha256").delete()

        var offset = resumeOffset(if (partial.exists()) partial.length() else 0L, file.sizeBytes)
        if (offset == 0L && partial.exists()) partial.delete()

        // Rebuild the digest from the bytes we keep. A plain local read.
        var digest = MessageDigest.getInstance("SHA-256")
        if (offset > 0L) {
            foldFileInto(partial, digest)
        }

        for (attempt in 0..1) {
            val connection = openPinnedConnection(entry.fileUrl(file), offset)
            try {
                val status = runCatching { connection.responseCode }.getOrElse { -1 }

                when {
                    offset > 0L && status == 416 -> {
                        // Range not satisfiable: the .part no longer matches the pin.
                        partial.delete()
                        offset = 0L
                        digest = MessageDigest.getInstance("SHA-256")
                        continue
                    }

                    offset > 0L && status != HttpURLConnection.HTTP_PARTIAL -> {
                        // The server ignored the range: restart from zero.
                        partial.delete()
                        offset = 0L
                        digest = MessageDigest.getInstance("SHA-256")
                        continue
                    }

                    offset == 0L && status != HttpURLConnection.HTTP_OK -> {
                        if (status == 429 || status >= 500) {
                            repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = "server error ($status)")
                            return null
                        }
                        repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = "unexpected response ($status)")
                        return null
                    }
                }

                var bytesWritten = offset

                partial.outputStream(append = offset > 0L).buffered().use { output ->
                    connection.inputStream.buffered().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var lastReported = 0L
                        while (true) {
                            if (isStopped) {
                                // Keep the .part for a later resume; mark PAUSED rather than FAILED.
                                repository.setModelState(modelId, VoiceModelState.PAUSED, downloadedBytes = bytesWritten)
                                return null
                            }
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            bytesWritten += count
                            if (bytesWritten - lastReported >= PROGRESS_INTERVAL_BYTES) {
                                lastReported = bytesWritten
                                repository.setModelState(
                                    modelId,
                                    VoiceModelState.DOWNLOADING,
                                    downloadedBytes = bytesWritten,
                                )
                                setProgress(workDataOf(KEY_BYTES_DOWNLOADED to bytesWritten, KEY_TOTAL_BYTES to entry.totalSizeBytes))
                                setForeground(
                                    foregroundInfo(
                                        downloadNotification(context, entry.displayName, bytesWritten, entry.totalSizeBytes),
                                    ),
                                )
                            }
                        }
                    }
                }

                if (partial.length() != file.sizeBytes) {
                    partial.delete()
                    repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = "length mismatch for ${file.name}")
                    return null
                }
                if (digest.digest().toVoiceDigestHex() != file.sha256) {
                    partial.delete()
                    repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = "digest mismatch for ${file.name}")
                    return null
                }
                if (target.exists() && !target.delete()) {
                    partial.delete()
                    repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = "cannot replace ${file.name}")
                    return null
                }
                if (!partial.renameTo(target)) {
                    partial.delete()
                    repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = "cannot finalize ${file.name}")
                    return null
                }
                File(storageDir, "${file.name}.sha256").writeText(file.sha256)
                return file.name
            } catch (error: FileNotFoundException) {
                val reason = if (error.message?.contains("No space left", ignoreCase = true) == true ||
                    error.message?.contains("ENOSPC", ignoreCase = true) == true
                ) {
                    "disk full while writing ${file.name}"
                } else {
                    error.message?.take(120) ?: "file not found"
                }
                repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = reason)
                return null
            } catch (error: Exception) {
                val reason = error.message?.take(120) ?: "download failed"
                repository.setModelState(modelId, VoiceModelState.FAILED, failedReason = reason)
                return null
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun cumulativeOf(storageDir: File, entry: VoiceModel): Long = entry.files.sumOf { f ->
        val file = File(storageDir, f.name)
        if (file.exists()) file.length() else 0L
    }

    private fun digestOfFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        foldFileInto(file, digest)
        return digest.digest().toVoiceDigestHex()
    }

    /** Terminal failure result, carrying the message as output data. */
    private fun fail(message: String): Result = Result.failure(workDataOf(KEY_ERROR to message))

    /** Hashes the whole file into [digest]; used for resume rebuild and verification. */
    private fun foldFileInto(file: File, digest: MessageDigest) {
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
    }

    private fun openPinnedConnection(url: String, offset: Long): HttpURLConnection {
        var current = url
        repeat(MAX_REDIRECTS + 1) {
            val parsed = URL(current)
            require(parsed.protocol == "https" && isAllowedDownloadHost(parsed.host)) {
                "untrusted voice model download host"
            }
            val connection = (parsed.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/octet-stream")
                if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
            }
            val statusCode = runCatching { connection.responseCode }.getOrElse { -1 }
            if (statusCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: error("redirect without a Location header")
                connection.disconnect()
                current = URL(parsed, location).toString()
            } else {
                return connection
            }
        }
        error("too many redirects")
    }

    private fun isAllowedDownloadHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "huggingface.co" ||
            lower.endsWith(".huggingface.co") ||
            lower == "hf.co" ||
            lower.endsWith(".hf.co")
    }

    private fun foregroundInfo(notification: Notification): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        notification,
        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )

    private fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Voice model downloads", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress for offline voice model downloads"
            },
        )
    }

    private fun downloadNotification(context: Context, title: String, downloaded: Long, total: Long): Notification {
        val progress = voiceDownloadProgressPercent(downloaded, total)
        return Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading offline voice model - $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()
    }

    private fun File.outputStream(append: Boolean): java.io.FileOutputStream = if (append) java.io.FileOutputStream(this, true) else java.io.FileOutputStream(this, false)

    companion object {
        const val WORK_NAME = "dorai-voice-model-download"
        const val KEY_MODEL = "model_id"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_DOWNLOADED = "downloaded_bytes"
        const val KEY_FILES = "verified_files"
        const val KEY_ERROR = "error"

        private const val CHANNEL_ID = "dorai_voice_download"
        private const val NOTIFICATION_ID = 9010
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val MAX_REDIRECTS = 5
        private const val BUFFER_SIZE = 65536
        private const val PROGRESS_INTERVAL_BYTES = 1_000_000L
    }
}

private fun voiceRepository(): VoiceModelRepository {
    val appSettings: AppSettings by KoinJavaComponent.inject(AppSettings::class.java)
    return VoiceModelRepository(appSettings::getVoiceModelManifestJson, appSettings::setVoiceModelManifestJson)
}
