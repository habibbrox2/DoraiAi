package com.inspiredandroid.kai.updater

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class AndroidUpdateManifest(
    val schemaVersion: Int,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val assetName: String,
    val downloadUrl: String,
    val sha256: String,
    val signerCertificateSha256: String,
) {
    fun isUpgradeFrom(installedVersionCode: Long): Boolean = versionCode > installedVersionCode

    fun validateForRelease(tagName: String): ManifestValidation {
        if (schemaVersion != 1) return ManifestValidation.Invalid("Unsupported manifest schema")
        if (packageName != EXPECTED_PACKAGE_NAME) return ManifestValidation.Invalid("Unexpected package name")
        if (!VERSION_NAME.matches(versionName)) return ManifestValidation.Invalid("Invalid version name")
        if (tagName != "v$versionName") return ManifestValidation.Invalid("Manifest version does not match release tag")
        if (versionCode <= 0L) return ManifestValidation.Invalid("Invalid version code")
        val expectedAsset = "Brox-Kai-2.0-v$versionName-Android.apk"
        if (assetName != expectedAsset) return ManifestValidation.Invalid("Unexpected APK asset name")
        val expectedUrl = "$RELEASE_DOWNLOAD_PREFIX$tagName/$assetName"
        if (downloadUrl != expectedUrl) return ManifestValidation.Invalid("Unexpected APK download URL")
        if (!SHA256.matches(sha256)) return ManifestValidation.Invalid("Invalid APK SHA-256")
        if (!SHA256.matches(signerCertificateSha256)) return ManifestValidation.Invalid("Invalid signer SHA-256")
        return ManifestValidation.Valid
    }

    companion object {
        const val EXPECTED_PACKAGE_NAME = "com.inspiredandroid.kai"
        const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/habibbrox2/brox-kai-2.0/releases/download/"
        private val VERSION_NAME = Regex("^2\\.0\\.[0-9]+$")
        private val SHA256 = Regex("^[a-f0-9]{64}$")
        private val json = Json {
            ignoreUnknownKeys = false
            isLenient = false
            coerceInputValues = false
        }
        fun parse(rawJson: String): AndroidUpdateManifest = json.decodeFromString(rawJson)
    }
}

sealed interface ManifestValidation {
    data object Valid : ManifestValidation
    data class Invalid(val reason: String) : ManifestValidation
}
