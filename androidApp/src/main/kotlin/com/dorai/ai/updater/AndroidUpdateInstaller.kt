package com.dorai.ai.updater

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Prepares a verified APK for Android's system package installer.
 *
 * This class never installs silently. The returned ACTION_VIEW intent is handled by the
 * system installer, which presents the user with the required confirmation UI.
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

        val contentUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.updateprovider",
                candidate,
            )
        } catch (_: IllegalArgumentException) {
            return InstallPreparation.Invalid("Unable to share the verified APK with Android installer")
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return InstallPreparation.Ready(installIntent)
    }

    private fun Context.declaresInstallPackagesPermission(): Boolean = runCatching {
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES) == true
    }.getOrDefault(false)

    sealed interface InstallPreparation {
        data class Ready(val installIntent: Intent) : InstallPreparation
        data class PermissionRequired(val settingsIntent: Intent) : InstallPreparation
        data class Unsupported(val reason: String) : InstallPreparation
        data class Invalid(val reason: String) : InstallPreparation
    }

    private companion object {
        const val UPDATES_DIRECTORY = "updates"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
