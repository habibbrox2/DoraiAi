package com.dorai.ai.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Verifies APK package, version and signing identity before the installer is exposed.
 *
 * The installed certificate is the trust anchor. The release manifest's fingerprint is
 * only a claim and must match both the installed app and the downloaded APK.
 */
internal class AndroidApkSignatureVerifier(
    private val context: Context,
) {
    fun installedSignerSha256(): String? {
        val packageInfo = getPackageInfo(context.packageName) ?: return null
        return packageInfo.signerSha256()
    }

    fun verifyDownloadedApk(
        apkFile: File,
        expectedPackageName: String,
        expectedVersionCode: Long,
        expectedSignerSha256: String,
    ): VerificationResult {
        if (!apkFile.isFile || apkFile.length() <= 0L) {
            return VerificationResult.Invalid("Downloaded APK is missing or empty")
        }

        val installedPackage = getPackageInfo(context.packageName)
            ?: return VerificationResult.Invalid("Unable to read installed app metadata")
        val archiveInfo = context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            signingFlags(),
        )
        val archiveSigner = archiveInfo?.signerSha256()

        val identityValidation = AndroidApkIdentityValidation.validate(
            expectedPackageName = expectedPackageName,
            installedPackageName = context.packageName,
            apkPackageName = archiveInfo?.packageName,
            expectedSignerSha256 = expectedSignerSha256,
            installedSignerSha256 = installedPackage.signerSha256(),
            apkSignerSha256 = archiveSigner,
        )
        if (identityValidation is AndroidApkIdentityValidation.Result.Invalid) {
            return VerificationResult.Invalid(identityValidation.reason)
        }

        val versionValidation = AndroidApkVersionValidation.validate(
            installedVersionCode = installedPackage.appVersionCode(),
            manifestVersionCode = expectedVersionCode,
            apkVersionCode = archiveInfo?.appVersionCode() ?: 0L,
        )
        if (versionValidation is AndroidApkVersionValidation.Result.Invalid) {
            return VerificationResult.Invalid(versionValidation.reason)
        }

        val validIdentity = identityValidation as AndroidApkIdentityValidation.Result.Valid
        return VerificationResult.Valid(validIdentity.signerSha256)
    }

    private fun getPackageInfo(packageName: String): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(signingFlags().toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, signingFlags())
        }
    }.getOrNull()

    private fun signingFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

    private fun PackageInfo.appVersionCode(): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

    private fun PackageInfo.signerSha256(): String? {
        val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            signatures?.map { it.toByteArray() }.orEmpty()
        }
        if (certificates.size != 1) return null
        return certificates.single().sha256Hex()
    }

    private fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    sealed interface VerificationResult {
        data class Valid(val signerSha256: String) : VerificationResult
        data class Invalid(val reason: String) : VerificationResult
    }
}
