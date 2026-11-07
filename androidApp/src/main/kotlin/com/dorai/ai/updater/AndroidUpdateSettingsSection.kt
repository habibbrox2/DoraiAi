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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * FOSS Android update controls rendered inside Settings > General.
 *
 * Uses Android PackageInstaller sessions instead of generic ACTION_VIEW intents, ensuring
 * deterministic system confirmation prompts without resolver ambiguity.
 */
@Composable
internal fun AndroidUpdateSettingsSection(appSettings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workManager = remember(context) { WorkManager.getInstance(context.applicationContext) }
    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(AndroidUpdateDownloadWorker.WORK_NAME)
        .collectAsState(initial = emptyList())
    val sessionState by AndroidUpdateSessionState.state.collectAsState()

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
        val currentManifest = manifest
        val apk = downloadedApk
        if (!context.packageManager.canRequestPackageInstalls()) {
            installerMessage = "Installation permission was not granted. The existing app was not changed."
        } else if (currentManifest != null && apk != null) {
            scope.launch(Dispatchers.IO) {
                when (val result = AndroidUpdateInstaller(context).prepareInstall(currentManifest, apk)) {
                    is AndroidUpdateInstaller.InstallPreparation.Staged -> {
                        withContext(Dispatchers.Main) {
                            installerMessage = "Update staged with Android installer. Please confirm installation."
                        }
                    }

                    is AndroidUpdateInstaller.InstallPreparation.PermissionRequired -> {
                        withContext(Dispatchers.Main) {
                            installerMessage = "Installation permission is still required."
                        }
                    }

                    is AndroidUpdateInstaller.InstallPreparation.Unsupported -> {
                        withContext(Dispatchers.Main) {
                            installerMessage = result.reason
                        }
                    }

                    is AndroidUpdateInstaller.InstallPreparation.Invalid -> {
                        withContext(Dispatchers.Main) {
                            installerMessage = "APK verification failed: ${result.reason}"
                        }
                    }
                }
            }
        } else {
            installerMessage = "Install permission granted. Future updates can install automatically."
        }
    }

    fun launchVerifiedInstall() {
        val currentManifest = manifest ?: return
        val apk = downloadedApk ?: return
        scope.launch(Dispatchers.IO) {
            when (val result = AndroidUpdateInstaller(context).prepareInstall(currentManifest, apk)) {
                is AndroidUpdateInstaller.InstallPreparation.Staged -> {
                    withContext(Dispatchers.Main) {
                        installerMessage = "Staging update with system installer. Confirm the install dialog to continue."
                    }
                }

                is AndroidUpdateInstaller.InstallPreparation.PermissionRequired -> {
                    withContext(Dispatchers.Main) {
                        installerMessage = "Allow installation from this source, then return here to install."
                        permissionLauncher.launch(result.settingsIntent)
                    }
                }

                is AndroidUpdateInstaller.InstallPreparation.Unsupported -> {
                    withContext(Dispatchers.Main) {
                        installerMessage = result.reason
                    }
                }

                is AndroidUpdateInstaller.InstallPreparation.Invalid -> {
                    withContext(Dispatchers.Main) {
                        installerMessage = "APK verification failed: ${result.reason}"
                    }
                }
            }
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
        // Detect if an update just completed across process recreation
        val targetVersionCode = AndroidUpdateSessionState.getTargetVersionCode(context)
        val currentCode = context.installedVersionCode()
        if (targetVersionCode > 0L && currentCode >= targetVersionCode) {
            val targetName = AndroidUpdateSessionState.getTargetVersionName(context) ?: ""
            installerMessage = "Successfully updated to version $targetName!"
            AndroidUpdateSessionState.clearActiveSession(context)
            AndroidUpdateAutoUpdate.clearPendingManifest(context)
        } else {
            AndroidUpdateSessionState.getLastFailure(context)?.let { (_, failureMsg) ->
                installerMessage = failureMsg
            }
        }

        if (manifest == null) {
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
                "Download complete. Staging update with Android installer…"
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

            // User confirmation fallback if system prompt is dismissed or backgrounded
            when (val currentSession = sessionState) {
                is AndroidUpdateSessionState.InstallState.AwaitingConfirmation -> {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(currentSession.confirmationIntent)
                            }
                        },
                    ) {
                        Text("Open install confirmation")
                    }
                }

                is AndroidUpdateSessionState.InstallState.Installing -> {
                    Text("Installing update…", style = MaterialTheme.typography.bodySmall)
                }

                is AndroidUpdateSessionState.InstallState.Success -> {
                    Text(
                        "Update succeeded! (Version ${currentSession.versionName})",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                is AndroidUpdateSessionState.InstallState.Failed -> {
                    Text(
                        currentSession.reason,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                else -> Unit
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
