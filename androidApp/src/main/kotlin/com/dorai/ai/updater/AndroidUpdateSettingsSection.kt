package com.dorai.ai.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dorai.ai.Version
import com.dorai.ai.data.AppSettings
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
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

    val colors = LocalDesignColorTokens.current
    val cardShape = RoundedCornerShape(16.dp)
    val subCardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderSubtle, cardShape)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Row: Title + Auto-update Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App updates",
                        style = DoraiTypography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Automatic download & install for official releases",
                        style = DoraiTypography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Auto",
                        style = DoraiTypography.labelMedium,
                        color = if (autoUpdateEnabled) DesignTokens.PrimaryCyan else colors.textSecondary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
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
            }

            // Version Information & Status Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(subCardShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, subCardShape)
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Installed: v${Version.appVersion}",
                                style = DoraiTypography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                            // Status badge
                            val (badgeText, badgeBg, badgeColor) = when {
                                checking -> Triple("Checking…", DesignTokens.Warning.copy(alpha = 0.15f), DesignTokens.Warning)
                                isDownloading -> Triple("Downloading", DesignTokens.PrimaryCyan.copy(alpha = 0.15f), DesignTokens.PrimaryCyan)
                                manifest != null -> Triple("Update Available", DesignTokens.PrimaryCyan.copy(alpha = 0.2f), DesignTokens.PrimaryCyan)
                                else -> Triple("Up to date", DesignTokens.Success.copy(alpha = 0.15f), DesignTokens.Success)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeColor,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = discoveryMessage,
                            style = DoraiTypography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }

                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { runCheck(auto = autoUpdateEnabled, force = true) },
                        enabled = !checking && !isDownloading,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(if (checking) "Checking…" else "Check now")
                    }
                }
            }

            // Available Release Card (Distinct Grouping)
            manifest?.let { available ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(subCardShape)
                        .background(DesignTokens.PrimaryCyan.copy(alpha = 0.08f))
                        .border(1.dp, DesignTokens.PrimaryCyan.copy(alpha = 0.35f), subCardShape)
                        .padding(14.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "New Release Available: v${available.versionName}",
                                    style = DoraiTypography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = "Build ${available.versionCode} • ${available.assetName}",
                                    style = DoraiTypography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (downloadedApk == null) {
                                Button(
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
                                    modifier = Modifier.handCursor(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesignTokens.PrimaryCyan,
                                        contentColor = DesignTokens.BrandNavy,
                                    ),
                                ) {
                                    Text("Download update", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = { launchVerifiedInstall() },
                                    modifier = Modifier.handCursor(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesignTokens.Success,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Text("Verify & Install", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Download Progress Container (Strong Visual Hierarchy)
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(subCardShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, subCardShape)
                        .padding(14.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val pct = if (total > 0L) (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Downloading update…",
                                style = DoraiTypography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                            if (total > 0L) {
                                Text(
                                    text = "${(pct * 100).toInt()}%",
                                    style = DoraiTypography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DesignTokens.PrimaryCyan,
                                )
                            }
                        }

                        if (total > 0L) {
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = DesignTokens.PrimaryCyan,
                                trackColor = colors.borderSubtle,
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = DesignTokens.PrimaryCyan,
                                trackColor = colors.borderSubtle,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (total > 0L) "${formatBytes(bytes)} of ${formatBytes(total)}" else "Connecting…",
                                style = DoraiTypography.bodySmall,
                                color = colors.textSecondary,
                            )
                            OutlinedButton(
                                onClick = { AndroidUpdateDownloadScheduler.cancel(context) },
                                modifier = Modifier.handCursor(),
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Session State Actions (e.g. Awaiting Confirmation fallback)
            when (val currentSession = sessionState) {
                is AndroidUpdateSessionState.InstallState.AwaitingConfirmation -> {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(currentSession.confirmationIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().handCursor(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DesignTokens.PrimaryCyan,
                            contentColor = DesignTokens.BrandNavy,
                        ),
                    ) {
                        Text("Open install confirmation dialog")
                    }
                }

                is AndroidUpdateSessionState.InstallState.Installing -> {
                    Text(
                        text = "Installing update with system package installer…",
                        style = DoraiTypography.bodySmall,
                        color = DesignTokens.PrimaryCyan,
                    )
                }

                is AndroidUpdateSessionState.InstallState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignTokens.Success.copy(alpha = 0.12f))
                            .border(1.dp, DesignTokens.Success.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            text = "Update succeeded! Running version ${currentSession.versionName}",
                            color = DesignTokens.Success,
                            fontWeight = FontWeight.SemiBold,
                            style = DoraiTypography.bodySmall,
                        )
                    }
                }

                is AndroidUpdateSessionState.InstallState.Failed -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignTokens.Error.copy(alpha = 0.12f))
                            .border(1.dp, DesignTokens.Error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            text = currentSession.reason,
                            color = DesignTokens.Error,
                            style = DoraiTypography.bodySmall,
                        )
                    }
                }

                else -> Unit
            }

            // Error or status messages
            workInfos.firstOrNull { it.state == WorkInfo.State.FAILED }?.let { failed ->
                val error = failed.outputData.getString(AndroidUpdateDownloadWorker.KEY_ERROR)
                Text(
                    text = error ?: "Update download failed.",
                    color = DesignTokens.Error,
                    style = DoraiTypography.bodySmall,
                )
            }
            if (!isDownloading && workInfos.any { it.state == WorkInfo.State.CANCELLED }) {
                Text(
                    text = "Update download was cancelled.",
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            installerMessage?.let {
                Text(
                    text = it,
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value < 1024L) return "$value B"
    val kib = value / 1024.0
    if (kib < 1024.0) return "${"%.1f".format(kib)} KB"
    return "${"%.1f".format(kib / 1024.0)} MB"
}
