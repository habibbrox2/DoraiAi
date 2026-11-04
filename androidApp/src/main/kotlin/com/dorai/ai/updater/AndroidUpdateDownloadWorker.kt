package com.dorai.ai.updater

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a release APK into app-private storage and publishes it only after SHA-256 passes.
 *
 * The worker never launches an installer. The foreground UI must independently verify the APK
 * signer and request the user's explicit confirmation through Android's package installer.
 */
internal class AndroidUpdateDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_URL) ?: return failure("Missing download URL")
        val expectedSha256 = inputData.getString(KEY_SHA256) ?: return failure("Missing checksum")
        val assetName = inputData.getString(KEY_ASSET_NAME) ?: return failure("Missing asset name")
        if (!isValidDownloadRequest(downloadUrl, expectedSha256, assetName)) {
            return failure("Invalid update download request")
        }

        val updateDirectory = File(applicationContext.filesDir, "updates").apply { mkdirs() }
        val partialFile = File(updateDirectory, "$assetName.part")
        val finalFile = File(updateDirectory, assetName)
        return try {
            partialFile.delete()
            val connection = openReleaseConnection(downloadUrl)
            try {
                val contentLength = connection.contentLengthLong
                if (contentLength > MAX_DOWNLOAD_BYTES) {
                    return failure("Update APK exceeds the maximum allowed size")
                }
                connection.inputStream.use { input ->
                    partialFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            if (isStopped) return Result.failure(workDataOf(KEY_ERROR to "Download cancelled"))
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_DOWNLOAD_BYTES) {
                                return failure("Update APK exceeds the maximum allowed size")
                            }
                            output.write(buffer, 0, count)
                            setProgress(
                                workDataOf(
                                    KEY_BYTES_DOWNLOADED to total,
                                    KEY_TOTAL_BYTES to contentLength,
                                ),
                            )
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            val actualSha256 = partialFile.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                return failure("Downloaded APK checksum mismatch")
            }
            if (finalFile.exists() && !finalFile.delete()) {
                return failure("Unable to replace previous update file")
            }
            if (!partialFile.renameTo(finalFile)) {
                return failure("Unable to finalize downloaded APK")
            }
            Result.success(workDataOf(KEY_FILE_PATH to finalFile.absolutePath))
        } catch (exception: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                failure(exception.message ?: "Update download failed")
            }
        } finally {
            // Never leave a partial or checksum-failed APK available to an installer.
            if (partialFile.exists()) partialFile.delete()
        }
    }

    private fun openReleaseConnection(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val url = URL(currentUrl)
            require(url.protocol == "https" && url.host.lowercase() in ALLOWED_DOWNLOAD_HOSTS) {
                "Untrusted update download host"
            }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.android.package-archive")
                setRequestProperty("User-Agent", "DORAI-Android-Updater/1.0")
            }
            val status = connection.responseCode
            if (status in 200..299) return connection
            if (status in REDIRECT_CODES && redirectCount < MAX_REDIRECTS) {
                val location = connection.getHeaderField("Location")
                    ?: run {
                        connection.disconnect()
                        error("Update server returned a redirect without a location")
                    }
                val nextUrl = URL(url, location).toString()
                connection.disconnect()
                currentUrl = nextUrl
            } else {
                connection.disconnect()
                error("Update server returned HTTP $status")
            }
        }
        error("Too many update download redirects")
    }

    private fun isValidDownloadRequest(url: String, sha256: String, assetName: String): Boolean = AndroidUpdateDownloadRequestValidation.isValid(url, sha256, assetName)

    private fun failure(message: String): Result = Result.failure(workDataOf(KEY_ERROR to message))

    companion object {
        const val WORK_NAME = "dorai-android-update-download"
        const val KEY_URL = "download_url"
        const val KEY_SHA256 = "sha256"
        const val KEY_ASSET_NAME = "asset_name"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR = "error"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val MAX_DOWNLOAD_BYTES = 200L * 1024L * 1024L
        private const val MAX_REDIRECTS = 5
        private const val MAX_RETRIES = 3
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val ALLOWED_DOWNLOAD_HOSTS = setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "github-releases.githubusercontent.com",
        )

        fun inputData(downloadUrl: String, sha256: String, assetName: String): Data = workDataOf(
            KEY_URL to downloadUrl,
            KEY_SHA256 to sha256,
            KEY_ASSET_NAME to assetName,
        )
    }
}

/** Enqueues one network-constrained update download, replacing any stale request. */
internal object AndroidUpdateDownloadScheduler {
    fun enqueue(context: Context, downloadUrl: String, sha256: String, assetName: String) {
        val request = OneTimeWorkRequestBuilder<AndroidUpdateDownloadWorker>()
            .setInputData(AndroidUpdateDownloadWorker.inputData(downloadUrl, sha256, assetName))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            AndroidUpdateDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(AndroidUpdateDownloadWorker.WORK_NAME)
    }
}
