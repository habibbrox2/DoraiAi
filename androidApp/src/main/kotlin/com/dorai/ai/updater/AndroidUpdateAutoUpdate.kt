package com.dorai.ai.updater

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dorai.ai.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Applies the user's "auto download and install" preference to Android updates.
 *
 * A check runs at most once per [CHECK_INTERVAL_MS] when the preference is on. A discovered
 * update is downloaded through [AndroidUpdateDownloadScheduler] and its manifest is kept in
 * app-private storage so a later launch can finish the install. Installation goes through
 * Android's PackageInstaller session API, which always requests the user's explicit confirmation.
 */
internal object AndroidUpdateAutoUpdate {

    internal const val CHECK_INTERVAL_MS = 4L * 60L * 60L * 1000L
    private const val PENDING_MANIFEST_NAME = "pending-update.json"
    private val installMutex = Mutex()

    fun start(activity: ComponentActivity, appSettings: AppSettings) {
        val context = activity.applicationContext

        // Observe lifecycle so a download that completed in the background is installed
        // when the app next comes to the foreground, instead of being forgotten.
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    activity.lifecycleScope.launch {
                        runCatching { installPendingIfReady(activity, appSettings) }
                    }
                }
            },
        )

        // Also react while the app is already in the foreground, so a download that
        // completes during this session opens the installer immediately.
        activity.lifecycleScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(AndroidUpdateDownloadWorker.WORK_NAME)
                .collect { infos ->
                    // Progress updates are frequent; only a finished download can be installed.
                    if (infos.none { it.state == WorkInfo.State.SUCCEEDED }) return@collect
                    if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        runCatching { installPendingIfReady(activity, appSettings) }
                    }
                }
        }

        if (!appSettings.isAutoUpdateEnabled()) return
        val now = System.currentTimeMillis()
        if (now - appSettings.getLastAutoUpdateCheckMs() < CHECK_INTERVAL_MS) return
        appSettings.setLastAutoUpdateCheckMs(now)
        activity.lifecycleScope.launch(Dispatchers.IO) {
            runCatching { checkAndDownload(context) }
        }
    }

    /** Checks for a newer stable release and, if one exists, starts the verified download. */
    suspend fun checkAndDownload(context: Context): AndroidUpdateDiscovery.Result {
        val result = AndroidUpdateDiscovery.check(context.installedVersionCode())
        if (result is AndroidUpdateDiscovery.Result.UpdateAvailable) {
            writePendingManifest(context, result.manifest)
            AndroidUpdateDownloadScheduler.enqueue(
                context = context,
                downloadUrl = result.manifest.downloadUrl,
                sha256 = result.manifest.sha256,
                assetName = result.manifest.assetName,
            )
        }
        return result
    }

    suspend fun readPendingManifest(context: Context): AndroidUpdateManifest? = withContext(Dispatchers.IO) {
        runCatching {
            val file = pendingManifestFile(context)
            if (!file.isFile) return@withContext null
            AndroidUpdateManifest.parse(file.readText())
        }.getOrNull()
    }

    suspend fun writePendingManifest(context: Context, manifest: AndroidUpdateManifest) = withContext(Dispatchers.IO) {
        runCatching {
            val file = pendingManifestFile(context)
            file.parentFile?.mkdirs()
            file.writeText(manifest.encode())
        }
        Unit
    }

    suspend fun clearPendingManifest(context: Context) = withContext(Dispatchers.IO) {
        runCatching { pendingManifestFile(context).delete() }
        Unit
    }

    private suspend fun installPendingIfReady(activity: ComponentActivity, appSettings: AppSettings) = installMutex.withLock {
        if (!appSettings.isAutoUpdateEnabled()) return@withLock
        val context = activity.applicationContext
        val manifest = readPendingManifest(context) ?: return@withLock

        val workInfos = withContext(Dispatchers.IO) {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(AndroidUpdateDownloadWorker.WORK_NAME)
                .get()
        }
        val completed = workInfos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED } ?: return@withLock
        val path = completed.outputData.getString(AndroidUpdateDownloadWorker.KEY_FILE_PATH) ?: return@withLock
        val file = withContext(Dispatchers.IO) {
            runCatching { File(path).canonicalFile }.getOrNull()
        } ?: return@withLock
        if (file.name != manifest.assetName || !file.isFile || file.length() <= 0L) return@withLock

        val preparation = withContext(Dispatchers.IO) {
            AndroidUpdateInstaller(context).prepareInstall(manifest, file)
        }
        when (preparation) {
            is AndroidUpdateInstaller.InstallPreparation.Staged -> {
                // Clear pending manifest once staged so it does not repeatedly re-stage.
                clearPendingManifest(context)
            }

            is AndroidUpdateInstaller.InstallPreparation.PermissionRequired -> {
                // The App updates card explains and requests the permission. Keep the
                // manifest so the install can continue once the user grants it.
            }

            is AndroidUpdateInstaller.InstallPreparation.Unsupported,
            is AndroidUpdateInstaller.InstallPreparation.Invalid,
            -> clearPendingManifest(context)
        }
    }

    private fun pendingManifestFile(context: Context): File = File(File(context.filesDir, "updates"), PENDING_MANIFEST_NAME)
}

internal fun Context.installedVersionCode(): Long {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
}
