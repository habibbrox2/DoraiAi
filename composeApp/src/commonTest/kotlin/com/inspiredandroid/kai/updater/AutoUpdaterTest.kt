package com.inspiredandroid.kai.updater

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoUpdaterTest {

    // ------------------------------------------------------------------
    // isNewer (semantic version comparison)
    // ------------------------------------------------------------------

    @Test
    fun equalVersionIsNotNewer() {
        assertFalse(AutoUpdater.isNewer("v2.0.9", "2.0.9"))
    }

    @Test
    fun olderRemoteVersionIsNotNewer() {
        assertFalse(AutoUpdater.isNewer("v2.0.8", "2.0.9"))
    }

    @Test
    fun patchBumpIsNewer() {
        assertTrue(AutoUpdater.isNewer("v2.0.10", "2.0.9"))
    }

    @Test
    fun minorBumpIsNewer() {
        assertTrue(AutoUpdater.isNewer("v2.1.0", "2.0.9"))
    }

    @Test
    fun majorBumpIsNewer() {
        assertTrue(AutoUpdater.isNewer("v3.0.0", "2.0.9"))
    }

    @Test
    fun comparisonIsNumericNotLexicographic() {
        // Lexicographic string compare would wrongly report 2.0.9 > 2.0.10.
        assertTrue(AutoUpdater.isNewer("v2.0.10", "2.0.9"))
    }

    @Test
    fun shorterRemoteVersionIsZeroPadded() {
        // 2.1 == 2.1.0, so not newer than local 2.1.0.
        assertFalse(AutoUpdater.isNewer("v2.1", "2.1.0"))
    }

    @Test
    fun shorterRemoteVersionIsNewerWhenInitialComponentBumps() {
        assertTrue(AutoUpdater.isNewer("v2.1", "2.0.9"))
    }

    @Test
    fun nonNumericSegmentsCountAsZero() {
        assertFalse(AutoUpdater.isNewer("v2.0.9-beta", "2.0.9"))
    }

    // ------------------------------------------------------------------
    // pickAssetFor (platform->asset selection)
    // ------------------------------------------------------------------

    private fun asset(name: String) = AutoUpdater.ReleaseAsset(
        name = name,
        browser_download_url = "https://example.com/$name",
    )

    // Full asset set as produced by the release workflow.
    private val allAssets = listOf(
        asset("Brox-Kai-2.0-v2.0.10-Android.apk"),
        asset("Brox-Kai-2.0-v2.0.10-Windows-x64.msi"),
        asset("Brox-Kai-2.0-v2.0.10-Windows-x64.zip"),
        asset("Brox-Kai-2.0-v2.0.10-macos-aarch64.dmg"),
        asset("Brox-Kai-2.0-v2.0.10-macos-x64.dmg"),
        asset("Brox-Kai-2.0-v2.0.10-linux-amd64.deb"),
        asset("Brox-Kai-2.0-v2.0.10-linux-amd64.AppImage"),
        asset("Brox-Kai-2.0-v2.0.10-linux-amd64.tar.gz"),
        asset("Brox-Kai-2.0-v2.0.10-linux-aarch64.deb"),
        asset("Brox-Kai-2.0-v2.0.10-linux-aarch64.AppImage"),
        asset("Brox-Kai-2.0-v2.0.10-linux-aarch64.tar.gz"),
    )

    @Test
    fun windowsPicksMsi() {
        assertEquals(
            "Brox-Kai-2.0-v2.0.10-Windows-x64.msi",
            AutoUpdater.pickAssetFor(allAssets, "windows")?.name,
        )
    }

    @Test
    fun macosX64PicksPlainDmgEvenWhenArm64DmgComesFirst() {
        assertEquals(
            "Brox-Kai-2.0-v2.0.10-macos-x64.dmg",
            AutoUpdater.pickAssetFor(allAssets, "macos-x64")?.name,
        )
    }

    @Test
    fun macosArm64PicksAarch64Dmg() {
        assertEquals(
            "Brox-Kai-2.0-v2.0.10-macos-aarch64.dmg",
            AutoUpdater.pickAssetFor(allAssets, "macos-arm64")?.name,
        )
    }

    @Test
    fun linuxX64PicksPlainPackageEvenWhenAarch64ComesFirst() {
        assertEquals(
            "Brox-Kai-2.0-v2.0.10-linux-amd64.deb",
            AutoUpdater.pickAssetFor(allAssets, "linux-x64")?.name,
        )
    }

    @Test
    fun linuxArm64PicksAarch64Asset() {
        assertEquals(
            "Brox-Kai-2.0-v2.0.10-linux-aarch64.deb",
            AutoUpdater.pickAssetFor(allAssets, "linux-arm64")?.name,
        )
    }

    @Test
    fun unsupportedPlatformReturnsNull() {
        assertNull(AutoUpdater.pickAssetFor(allAssets, "none"))
    }

    @Test
    fun windowsWithoutMsiReturnsNull() {
        val noMsi = allAssets.filter { !it.name.endsWith(".msi") }
        assertNull(AutoUpdater.pickAssetFor(noMsi, "windows"))
    }

    @Test
    fun macosArm64WithoutAarch64DmgReturnsNullRatherThanX64Dmg() {
        val noArm64 = allAssets.filter { !it.name.endsWith("-aarch64.dmg") }
        assertNull(AutoUpdater.pickAssetFor(noArm64, "macos-arm64"))
    }

    @Test
    fun emptyAssetListReturnsNullEverywhere() {
        for (key in listOf("windows", "macos-x64", "macos-arm64", "linux-x64", "linux-arm64", "none")) {
            assertNull(AutoUpdater.pickAssetFor(emptyList(), key))
        }
    }

    // ------------------------------------------------------------------
    // Android release manifest validation
    // ------------------------------------------------------------------

    private fun validAndroidManifest(
        versionName: String = "2.0.75",
        versionCode: Long = 190L,
        tagName: String = "v2.0.75",
        downloadUrl: String = "https://github.com/habibbrox2/brox-kai-2.0/releases/download/v2.0.75/Brox-Kai-2.0-v2.0.75-Android.apk",
        sha256: String = "a".repeat(64),
        signerSha256: String = "b".repeat(64),
    ) = AndroidUpdateManifest(
        schemaVersion = 1,
        packageName = "com.inspiredandroid.kai",
        versionName = versionName,
        versionCode = versionCode,
        assetName = "Brox-Kai-2.0-v$versionName-Android.apk",
        downloadUrl = downloadUrl,
        sha256 = sha256,
        signerCertificateSha256 = signerSha256,
    ).validateForRelease(tagName)

    @Test
    fun validAndroidManifestIsAccepted() {
        assertEquals(ManifestValidation.Valid, validAndroidManifest())
    }

    @Test
    fun manifestVersionMustMatchStableReleaseTag() {
        assertTrue(validAndroidManifest(tagName = "v2.0.76") is ManifestValidation.Invalid)
    }

    @Test
    fun manifestRejectsInvalidVersionName() {
        assertTrue(validAndroidManifest(versionName = "2.0.75-beta") is ManifestValidation.Invalid)
    }

    @Test
    fun manifestRejectsNonPositiveVersionCode() {
        assertTrue(validAndroidManifest(versionCode = 0L) is ManifestValidation.Invalid)
    }

    @Test
    fun manifestRejectsUnexpectedDownloadOriginOrPath() {
        assertTrue(
            validAndroidManifest(downloadUrl = "https://example.com/malicious.apk") is ManifestValidation.Invalid,
        )
    }

    @Test
    fun manifestRejectsMalformedApkChecksum() {
        assertTrue(validAndroidManifest(sha256 = "not-a-sha256") is ManifestValidation.Invalid)
    }

    @Test
    fun manifestRejectsMalformedSignerFingerprint() {
        assertTrue(validAndroidManifest(signerSha256 = "not-a-fingerprint") is ManifestValidation.Invalid)
    }

    @Test
    fun manifestParserRejectsUnknownFields() {
        val raw = """
            {
              "schemaVersion": 1,
              "packageName": "com.inspiredandroid.kai",
              "versionName": "2.0.75",
              "versionCode": 190,
              "assetName": "Brox-Kai-2.0-v2.0.75-Android.apk",
              "downloadUrl": "https://github.com/habibbrox2/brox-kai-2.0/releases/download/v2.0.75/Brox-Kai-2.0-v2.0.75-Android.apk",
              "sha256": "${"a".repeat(64)}",
              "signerCertificateSha256": "${"b".repeat(64)}",
              "unexpected": true
            }
        """.trimIndent()
        kotlin.test.assertFails { AndroidUpdateManifest.parse(raw) }
    }

    @Test
    fun androidUpgradeUsesVersionCodeNotVersionName() {
        val manifest = AndroidUpdateManifest(
            schemaVersion = 1,
            packageName = "com.inspiredandroid.kai",
            versionName = "2.0.75",
            versionCode = 190L,
            assetName = "Brox-Kai-2.0-v2.0.75-Android.apk",
            downloadUrl = "https://github.com/habibbrox2/brox-kai-2.0/releases/download/v2.0.75/Brox-Kai-2.0-v2.0.75-Android.apk",
            sha256 = "a".repeat(64),
            signerCertificateSha256 = "b".repeat(64),
        )
        assertTrue(manifest.isUpgradeFrom(189L))
        assertFalse(manifest.isUpgradeFrom(190L))
        assertFalse(manifest.isUpgradeFrom(191L))
    }
}
