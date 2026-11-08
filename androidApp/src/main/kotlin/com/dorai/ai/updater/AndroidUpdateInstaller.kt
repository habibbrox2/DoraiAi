package com.dorai.ai.updater

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import java.util.zip.ZipFile

/**
 * Prepares and stages a cryptographically verified APK using Android's PackageInstaller session API.
 *
 * Replaces the fragile ACTION_VIEW / FileProvider flow with a deterministic system session.
 * Streams verified APK bytes directly into PackageInstaller.Session, commits with an explicit
 * PendingIntent to AndroidUpdateInstallStatusReceiver, and never prompts for a generic installer.
 */
internal class AndroidUpdateInstaller(
    private val context: Context,
    private val signatureVerifier: AndroidApkSignatureVerifier = AndroidApkSignatureVerifier(context),
) {
    fun prepareInstall(
        manifest: AndroidUpdateManifest,
        apkFile: File,
    ): InstallPreparation {
        val updatesDirectory = File(context.filesDir, UPDATES_DIRECTORY).canonicalFile
        val candidate = runCatching { apkFile.canonicalFile }.getOrNull()
            ?: return InstallPreparation.Invalid("Downloaded APK path is invalid")
        if (!AndroidUpdateApkPathValidation.isValid(updatesDirectory, candidate, manifest.assetName)) {
            return InstallPreparation.Invalid("Downloaded APK is outside the verified update directory")
        }

        when (val checksum = AndroidUpdateChecksumVerifier.verify(candidate, manifest.sha256)) {
            is AndroidUpdateChecksumVerifier.Result.Invalid ->
                return InstallPreparation.Invalid(checksum.reason)

            AndroidUpdateChecksumVerifier.Result.Valid -> Unit
        }

        if (!isValidApkArchive(candidate)) {
            return InstallPreparation.Invalid("Downloaded APK archive is corrupt or invalid")
        }

        when (
            val verification = signatureVerifier.verifyDownloadedApk(
                apkFile = candidate,
                expectedPackageName = manifest.packageName,
                expectedVersionCode = manifest.versionCode,
                expectedSignerSha256 = manifest.signerCertificateSha256,
            )
        ) {
            is AndroidApkSignatureVerifier.VerificationResult.Invalid ->
                return InstallPreparation.Invalid(verification.reason)

            is AndroidApkSignatureVerifier.VerificationResult.Valid -> Unit
        }

        if (!context.declaresInstallPackagesPermission()) {
            return InstallPreparation.Unsupported(
                "This distribution does not permit installing updates from the app",
            )
        }
        if (!context.packageManager.canRequestPackageInstalls()) {
            return InstallPreparation.PermissionRequired(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }

        return stageAndCommitInstall(manifest, candidate)
    }

    private fun stageAndCommitInstall(
        manifest: AndroidUpdateManifest,
        candidate: File,
    ): InstallPreparation {
        val packageInstaller = context.packageManager.packageInstaller
        abandonStaleSessions(packageInstaller)

        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(manifest.packageName)
            setSize(candidate.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_OTHER)
            }
        }

        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: Exception) {
            return InstallPreparation.Invalid("Unable to create install session: ${e.message}")
        }

        return try {
            val session = packageInstaller.openSession(sessionId)
            session.use { activeSession ->
                candidate.inputStream().buffered().use { input ->
                    activeSession.openWrite(SESSION_STREAM_NAME, 0, candidate.length()).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        activeSession.fsync(output)
                    }
                }

                val statusIntent = Intent(context, AndroidUpdateInstallStatusReceiver::class.java).apply {
                    action = AndroidUpdateInstallStatusReceiver.ACTION_INSTALL_STATUS
                    putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
                    putExtra(AndroidUpdateInstallStatusReceiver.EXTRA_VERSION_CODE, manifest.versionCode)
                    putExtra(AndroidUpdateInstallStatusReceiver.EXTRA_VERSION_NAME, manifest.versionName)
                    setPackage(context.packageName)
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    statusIntent,
                    flags,
                )

                activeSession.commit(pendingIntent.intentSender)
            }

            AndroidUpdateSessionState.recordActiveSession(
                context,
                sessionId,
                manifest.versionCode,
                manifest.versionName,
            )
            AndroidUpdateSessionState.updateState(
                AndroidUpdateSessionState.InstallState.Staging(sessionId),
            )
            InstallPreparation.Staged(sessionId)
        } catch (e: Exception) {
            runCatching { packageInstaller.openSession(sessionId).abandon() }
            InstallPreparation.Invalid("Failed to stage package for installation: ${e.message}")
        }
    }

    private fun abandonStaleSessions(packageInstaller: PackageInstaller) {
        runCatching {
            for (info in packageInstaller.mySessions) {
                runCatching { packageInstaller.openSession(info.sessionId).abandon() }
            }
        }
    }

    private fun isValidApkArchive(file: File): Boolean = runCatching {
        ZipFile(file).use { zip ->
            zip.getEntry("AndroidManifest.xml") != null &&
                zip.getEntry("classes.dex") != null
        }
    }.getOrDefault(false)

    private fun Context.declaresInstallPackagesPermission(): Boolean = runCatching {
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES) == true
    }.getOrDefault(false)

    sealed interface InstallPreparation {
        data class Staged(val sessionId: Int) : InstallPreparation
        data class PermissionRequired(val settingsIntent: Intent) : InstallPreparation
        data class Unsupported(val reason: String) : InstallPreparation
        data class Invalid(val reason: String) : InstallPreparation
    }

    private companion object {
        const val UPDATES_DIRECTORY = "updates"
        const val SESSION_STREAM_NAME = "dorai_apk_session"
        const val BUFFER_SIZE = 65536
    }
}
