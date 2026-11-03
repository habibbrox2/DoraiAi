package com.inspiredandroid.kai.updater

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidUpdateDownloadRequestValidationTest {
    private val asset = "Brox-Kai-2.0-v2.0.75-Android.apk"
    private val url = "https://github.com/habibbrox2/brox-kai-2.0/releases/download/v2.0.75/$asset"
    private val sha256 = "a".repeat(64)

    @Test
    fun acceptsCanonicalReleaseAsset() {
        assertTrue(AndroidUpdateDownloadRequestValidation.isValid(url, sha256, asset))
    }

    @Test
    fun rejectsNonGitHubHost() {
        assertFalse(AndroidUpdateDownloadRequestValidation.isValid(url.replace("github.com", "example.com"), sha256, asset))
    }

    @Test
    fun rejectsTagAndAssetMismatch() {
        assertFalse(AndroidUpdateDownloadRequestValidation.isValid(url.replace("v2.0.75", "v2.0.76"), sha256, asset))
    }

    @Test
    fun rejectsPathTraversalAssetName() {
        assertFalse(AndroidUpdateDownloadRequestValidation.isValid(url, sha256, "../$asset"))
    }

    @Test
    fun rejectsMalformedChecksum() {
        assertFalse(AndroidUpdateDownloadRequestValidation.isValid(url, "not-a-sha256", asset))
    }
}
