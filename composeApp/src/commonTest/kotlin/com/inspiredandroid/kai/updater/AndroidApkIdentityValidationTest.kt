package com.inspiredandroid.kai.updater

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AndroidApkIdentityValidationTest {
    private val fingerprint = "a".repeat(64)
    private val otherFingerprint = "b".repeat(64)

    private fun validate(
        expectedPackageName: String = "com.inspiredandroid.kai",
        installedPackageName: String = "com.inspiredandroid.kai",
        apkPackageName: String? = "com.inspiredandroid.kai",
        expectedSignerSha256: String = fingerprint,
        installedSignerSha256: String? = fingerprint,
        apkSignerSha256: String? = fingerprint,
    ) = AndroidApkIdentityValidation.validate(
        expectedPackageName,
        installedPackageName,
        apkPackageName,
        expectedSignerSha256,
        installedSignerSha256,
        apkSignerSha256,
    )

    @Test
    fun acceptsMatchingPackageAndSingleSigner() {
        assertIs<AndroidApkIdentityValidation.Result.Valid>(validate())
    }

    @Test
    fun rejectsPackageMismatch() {
        assertIs<AndroidApkIdentityValidation.Result.Invalid>(
            validate(apkPackageName = "com.attacker.app"),
        )
    }

    @Test
    fun rejectsManifestSignerThatDoesNotMatchInstalledSigner() {
        assertIs<AndroidApkIdentityValidation.Result.Invalid>(
            validate(expectedSignerSha256 = otherFingerprint),
        )
    }

    @Test
    fun rejectsApkSignerThatDoesNotMatchInstalledSigner() {
        assertIs<AndroidApkIdentityValidation.Result.Invalid>(
            validate(apkSignerSha256 = otherFingerprint),
        )
    }

    @Test
    fun rejectsMissingInstalledSigner() {
        assertIs<AndroidApkIdentityValidation.Result.Invalid>(
            validate(installedSignerSha256 = null),
        )
    }

    @Test
    fun rejectsMissingOrAmbiguousApkSigner() {
        assertIs<AndroidApkIdentityValidation.Result.Invalid>(
            validate(apkSignerSha256 = null),
        )
    }

    @Test
    fun rejectsMalformedFingerprints() {
        val result = validate(expectedSignerSha256 = "not-a-fingerprint")
        assertEquals(
            "Invalid expected signer fingerprint",
            (result as AndroidApkIdentityValidation.Result.Invalid).reason,
        )
    }

    @Test
    fun acceptsHexFingerprintCaseInsensitivelyAndNormalizesResult() {
        val result = assertIs<AndroidApkIdentityValidation.Result.Valid>(
            validate(
                expectedSignerSha256 = fingerprint.uppercase(),
                installedSignerSha256 = fingerprint.uppercase(),
            ),
        )
        assertEquals(fingerprint, result.signerSha256)
    }
}
