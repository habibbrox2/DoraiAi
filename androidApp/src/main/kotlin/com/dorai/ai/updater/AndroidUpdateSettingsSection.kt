package com.dorai.ai.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dorai.ai.data.AppSettings
import kotlinx.coroutines.launch
import java.io.File

/**
 * FOSS Android update controls rendered inside Settings > General.
 *
 * Discovery and download are separate from installation. The install button re-verifies the
 * APK immediately before handing it to Android's system package installer.
 *
 * When "Auto download and install" is on, a discovered update is downloaded automatically and
 * the installer is opened once the download completes. Android always shows its own confirmation
 * dialog, so the switch cannot bypass the system installer.
 */
@Composable
internal fun AndroidUpdateSettingsSection(appSettings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workManager = remember(context) { WorkManager.getInstance(context.applicationContext) }
    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(AndroidUpdateDownloadWorker.WORK_NAME)
        .collectAsState(initial = emptyList())

    var checking by remember { mutableStateOf(false) }
    var autoUpdateEnabled by remember { mutableStateOf(appSettings.isAutoUpdateEnabled()) }
    var discoveryMessage by rememberSaveable { mutableStateOf("Check GitHub for the latest stable release.") }
    val manifestSaver = remember {
        listSaver<AndroidUpdateManifest?, Any>(
            save = { value ->
                value?.let {
                    listOf(
                        it.schemaVersion,
                        it.packageName,
                        it.versionName,
                        it.versionCode,
                        it.assetName,
                        it.downloadUrl,
                        it.sha256,
                        it.signerCertificateSha256,
                    )
                } ?: emptyList()
            },
            restore = { saved ->
                if (saved.size != 8) {
                    null
                } else {
                    runCatching {
                        AndroidUpdateManifest(
                            schemaVersion = saved[0] as Int,
                            packageName = saved[1] as String,
                            versionName = saved[2] as String,
                            versionCode = saved[3] as Long,
                            assetName = saved[4] as String,
                            downloadUrl = saved[5] as String,
                            sha256 = saved[6] as String,
                            signerCertificateSha256 = saved[7] as String,
                        )
                    }.getOrNull()
                }
            },
        )
    }
    var manifest by rememberSaveable(stateSaver = manifestSaver) { mutableStateOf<AndroidUpdateManifest?>(null) }
    var downloadedApkPath by rememberSaveable { mutableStateOf<String?>(null) }
    val downloadedApk = remember(downloadedApkPath) { downloadedApkPath?.let(::File) }
    var installerMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val activeWork = workInfos.firstOrNull {
        it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.BLOCKED
    }
    val progress = activeWork?.progress
    val bytes = progress?.getLong(AndroidUpdateDownloadWorker.KEY_BYTES_DOWNLOADED, 0L) ?: 0L
    val total = progress?.getLong(AndroidUpdateDownloadWorker.KEY_TOTAL_BYTES, -1L) ?: -1L
    val isDownloading = activeWork != null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Permission is not proof of installability: verify the APK again after returning.
        val currentManifest = manifest
        val apk = downloadedApk
        if (!context.packageManager.canRequestPackageInstalls()) {
            installerMessage = "Installation permission was not granted. The existing app was not changed."
        } else if (currentManifest != null && apk != null) {
            when (val result = AndroidUpdateInstaller(context).prepareInstall(currentManifest, apk)) {
                is AndroidUpdateInstaller.InstallPreparation.Ready -> {
                    runCatching { context.startActivity(result.installIntent) }
                        .onSuccess { installerMessage = "Android's installer was opened. Confirm the update there to continue." }
                        .onFailure { installerMessage = "Android could not open the package installer." }
                }

                is AndroidUpdateInstaller.InstallPreparation.PermissionRequired ->
                    installerMessage = "Installation permission is still required."

                is AndroidUpdateInstaller.InstallPreparation.Unsupported ->
                    installerMessage = result.reason

                is AndroidUpdateInstaller.InstallPreparation.Invalid ->
                    installerMessage = "APK verification failed: ${result.reason}"
            }
        } else {
            installerMessage = "Install permission granted. Future updates can install automatically."
        }
    }

    fun launchVerifiedInstall() {
        val currentManifest = manifest ?: return
        val apk = downloadedApk ?: return
        when (val result = AndroidUpdateInstaller(context).prepareInstall(currentManifest, apk)) {
            is AndroidUpdateInstaller.InstallPreparation.Ready -> {
                try {
                    context.startActivity(result.installIntent)
                    installerMessage = "Android's installer was opened. Confirm the update there to continue."
                } catch (_: Exception) {
                    installerMessage = "Android could not open the package installer."
                }
            }

            is AndroidUpdateInstaller.InstallPreparation.PermissionRequired -> {
                installerMessage = "Allow installation from this source, then return here to verify the APK again."
                permissionLauncher.launch(result.settingsIntent)
            }

            is AndroidUpdateInstaller.InstallPreparation.Unsupported -> installerMessage = result.reason

            is AndroidUpdateInstaller.InstallPreparation.Invalid -> installerMessage = "APK verification failed: ${result.reason}"
        }
    }

    fun runCheck(auto: Boolean, force: Boolean) {
        if (checking) return
        if (auto && !force &&
            System.currentTimeMillis() - appSettings.getLastAutoUpdateCheckMs() < AndroidUpdateAutoUpdate.CHECK_INTERVAL_MS
        ) {
            return
        }
        checking = true
        discoveryMessage = "Checking the latest stable GitHub release…"
        manifest = null
        downloadedApkPath = null
        installerMessage = null
        scope.launch {
            val result = runCatching {
                if (auto) {
                    AndroidUpdateAutoUpdate.checkAndDownload(context)
                } else {
                    AndroidUpdateDiscovery.check(context.installedVersionCode())
                }
            }
            checking = false
            if (auto) appSettings.setLastAutoUpdateCheckMs(System.currentTimeMillis())
            result.fold(
                onSuccess = { update ->
                    when (update) {
                        AndroidUpdateDiscovery.Result.UpToDate -> {
                            discoveryMessage = "This installation is up to date."
                        }

                        is AndroidUpdateDiscovery.Result.InvalidRelease -> {
                            discoveryMessage = "Update metadata could not be validated: ${update.reason}"
                        }

                        is AndroidUpdateDiscovery.Result.UpdateAvailable -> {
                            manifest = update.manifest
                            discoveryMessage = if (auto) {
                                "Version ${update.manifest.versionName} is available. Downloading automatically…"
                            } else {
                                "Version ${update.manifest.versionName} is available."
                            }
                        }
                    }
                },
                onFailure = {
                    discoveryMessage = "Update check failed. Check your connection and try again."
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        if (manifest == null) {
            // A check may have run at app start and already queued a download.
            manifest = AndroidUpdateAutoUpdate.readPendingManifest(context)
            if (manifest != null) {
                discoveryMessage = "Version ${manifest?.versionName} is available."
            }
        }
        if (autoUpdateEnabled && manifest == null) {
            runCheck(auto = true, force = false)
        }
    }

    LaunchedEffect(workInfos, manifest) {
        val completed = workInfos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED } ?: return@LaunchedEffect
        val path = completed.outputData.getString(AndroidUpdateDownloadWorker.KEY_FILE_PATH) ?: return@LaunchedEffect
        val currentManifest = manifest ?: return@LaunchedEffect
        val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return@LaunchedEffect
        if (file.name == currentManifest.assetName && file.isFile && file.length() > 0L) {
            downloadedApkPath = file.absolutePath
            installerMessage = if (autoUpdateEnabled) {
                "Download complete. Opening Android's installer…"
            } else {
                "Download complete. Verify and install when ready."
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("App updates", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto download and install", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "New releases download automatically and open Android's installer. " +
                            "Android still asks you to confirm the install.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = autoUpdateEnabled,
                    onCheckedChange = { enabled ->
                        autoUpdateEnabled = enabled
                        appSettings.setAutoUpdateEnabled(enabled)
                        if (enabled) {
                            if (!context.packageManager.canRequestPackageInstalls()) {
                                installerMessage =
                                    "Allow installs from this source so updates can install automatically."
                                runCatching {
                                    permissionLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                }
                            }
                            runCheck(auto = true, force = true)
                        } else {
                            installerMessage = "Automatic updates are off."
                        }
                    },
                )
            }

            Text(discoveryMessage, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { runCheck(auto = autoUpdateEnabled, force = true) },
                enabled = !checking && !isDownloading,
            ) {
                Text(if (checking) "Checking…" else "Check for updates")
            }

            manifest?.let { available ->
                Text(
                    "Release ${available.versionName} (versionCode ${available.versionCode})",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (downloadedApk == null) {
                    OutlinedButton(
                        onClick = {
                            installerMessage = null
                            AndroidUpdateDownloadScheduler.enqueue(
                                context = context,
                                downloadUrl = available.downloadUrl,
                                sha256 = available.sha256,
                                assetName = available.assetName,
                            )
                        },
                        enabled = !isDownloading,
                    ) {
                        Text("Download update")
                    }
                } else {
                    Button(onClick = { launchVerifiedInstall() }) {
                        Text("Verify and install")
                    }
                }
            }

            if (isDownloading) {
                if (total > 0L) {
                    LinearProgressIndicator(
                        progress = { (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${formatBytes(bytes)} of ${formatBytes(total)} downloaded",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Downloading update…", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { AndroidUpdateDownloadScheduler.cancel(context) }) {
                    Text("Cancel download")
                }
            }

            workInfos.firstOrNull { it.state == WorkInfo.State.FAILED }?.let { failed ->
                val error = failed.outputData.getString(AndroidUpdateDownloadWorker.KEY_ERROR)
                Text(error ?: "Update download failed.", color = MaterialTheme.colorScheme.error)
            }
            if (!isDownloading && workInfos.any { it.state == WorkInfo.State.CANCELLED }) {
                Text("Update download cancelled.", style = MaterialTheme.typography.bodySmall)
            }
            installerMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value < 1024L) return "$value B"
    val kib = value / 1024.0
    if (kib < 1024.0) return "${"%.1f".format(kib)} KB"
    return "${"%.1f".format(kib / 1024.0)} MB"
}
