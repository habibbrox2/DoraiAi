package com.inspiredandroid.kai.updater

/**
 * Pure validation for the package and signing identity of a downloaded Android update.
 *
 * The installed certificate is the trust anchor. A fingerprint supplied by a remote
 * manifest is only a claim and must match both the installed signer and APK signer.
 */
object AndroidApkIdentityValidation {
    private val sha256Pattern = Regex("^[a-fA-F0-9]{64}$")

    sealed interface Result {
        data class Valid(val signerSha256: String) : Result
        data class Invalid(val reason: String) : Result
    }

    fun validate(
        expectedPackageName: String,
        installedPackageName: String,
        apkPackageName: String?,
        expectedSignerSha256: String,
        installedSignerSha256: String?,
        apkSignerSha256: String?,
    ): Result {
        if (expectedPackageName.isBlank() ||
            expectedPackageName != installedPackageName ||
            apkPackageName != expectedPackageName
        ) {
            return Result.Invalid("Downloaded APK package name mismatch")
        }
        if (!sha256Pattern.matches(expectedSignerSha256)) {
            return Result.Invalid("Invalid expected signer fingerprint")
        }
        val installedSigner = installedSignerSha256
            ?: return Result.Invalid("Unable to establish installed app signer")
        if (!sha256Pattern.matches(installedSigner)) {
            return Result.Invalid("Invalid installed signer fingerprint")
        }
        if (!installedSigner.equals(expectedSignerSha256, ignoreCase = true)) {
            return Result.Invalid("Release manifest signer does not match installed app")
        }
        val apkSigner = apkSignerSha256
            ?: return Result.Invalid("Downloaded APK must have exactly one signer")
        if (!sha256Pattern.matches(apkSigner)) {
            return Result.Invalid("Invalid downloaded APK signer fingerprint")
        }
        if (!apkSigner.equals(installedSigner, ignoreCase = true)) {
            return Result.Invalid("Downloaded APK signer does not match installed app")
        }
        return Result.Valid(apkSigner.lowercase())
    }
}
