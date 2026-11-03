package com.inspiredandroid.kai.updater

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AndroidUpdateDiscoveryTest {
    private val mainManifestUrl =
        "https://raw.githubusercontent.com/habibbrox2/brox-kai-2.0/main/update.json"

    private fun release(
        tag: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
        includeManifest: Boolean = true,
    ): String {
        val assets = if (includeManifest) {
            """
            "assets": [{
              "name": "update.json",
              "browser_download_url": "https://github.com/habibbrox2/brox-kai-2.0/releases/download/$tag/update.json"
            }]
            """.trimIndent()
        } else {
            "\"assets\": []"
        }
        return """
            {
              "tag_name": "$tag",
              "draft": $draft,
              "prerelease": $prerelease,
              $assets
            }
        """.trimIndent()
    }

    private fun manifest(version: String = "2.0.76", code: Long = 191L): String = """
        {
          "schemaVersion": 1,
          "packageName": "com.inspiredandroid.kai",
          "versionName": "$version",
          "versionCode": $code,
          "assetName": "Brox-Kai-2.0-v$version-Android.apk",
          "downloadUrl": "https://github.com/habibbrox2/brox-kai-2.0/releases/download/v$version/Brox-Kai-2.0-v$version-Android.apk",
          "sha256": "${"a".repeat(64)}",
          "signerCertificateSha256": "${"b".repeat(64)}"
        }
    """.trimIndent()

    private fun legacyFetch(api: String, releaseManifest: String = manifest()): suspend (String) -> String = { url ->
        when {
            url == mainManifestUrl -> throw HttpRequestException(statusCode = 404)
            url.contains("/releases?") -> api
            else -> releaseManifest
        }
    }

    @Test
    fun prefersCanonicalMainManifestWithoutCallingReleasesApi() = runTest {
        var releasesApiCalled = false
        val result = AndroidUpdateDiscovery.check(189L) { url ->
            when (url) {
                mainManifestUrl -> manifest()

                else -> {
                    releasesApiCalled = true
                    error("Legacy releases API should not be called")
                }
            }
        }
        val update = assertIs<AndroidUpdateDiscovery.Result.UpdateAvailable>(result)
        assertEquals("v2.0.76", update.releaseTag)
        assertEquals(191L, update.manifest.versionCode)
        assertEquals(false, releasesApiCalled)
    }

    @Test
    fun fallsBackToLegacyReleaseAssetWhenMainManifestIsMissing() = runTest {
        val api = "[${release("v2.0.76")}]"
        val result = AndroidUpdateDiscovery.check(189L, legacyFetch(api))
        val update = assertIs<AndroidUpdateDiscovery.Result.UpdateAvailable>(result)
        assertEquals("v2.0.76", update.releaseTag)
    }

    @Test
    fun legacyFallbackSelectsNewestStableRelease() = runTest {
        val api = """
            [
              ${release("v2.0.76")},
              ${release("v2.0.77", prerelease = true)},
              ${release("v2.0.78", draft = true)},
              ${release("v2.0.75")}
            ]
        """.trimIndent()
        val result = AndroidUpdateDiscovery.check(189L, legacyFetch(api))
        val update = assertIs<AndroidUpdateDiscovery.Result.UpdateAvailable>(result)
        assertEquals("v2.0.76", update.releaseTag)
        assertEquals(191L, update.manifest.versionCode)
    }

    @Test
    fun ignoresDraftAndPrereleaseReleasesInLegacyFallback() = runTest {
        val api = """
            [
              ${release("v2.0.99", prerelease = true)},
              ${release("v2.0.98", draft = true)}
            ]
        """.trimIndent()
        val result = AndroidUpdateDiscovery.check(189L, legacyFetch(api))
        assertEquals(AndroidUpdateDiscovery.Result.UpToDate, result)
    }

    @Test
    fun missingLegacyManifestFailsClosed() = runTest {
        val api = "[${release("v2.0.76", includeManifest = false)}]"
        val result = AndroidUpdateDiscovery.check(189L, legacyFetch(api))
        assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
    }

    @Test
    fun mainManifestNon404HttpFailureDoesNotSilentlyFallback() = runTest {
        val result = AndroidUpdateDiscovery.check(189L) { throw HttpRequestException(statusCode = 403) }
        val invalid = assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
        assertTrue(invalid.reason.contains("HTTP 403"))
    }

    @Test
    fun malformedMainManifestFailsClosed() = runTest {
        val result = AndroidUpdateDiscovery.check(189L) { "not-json" }
        assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
    }

    @Test
    fun reportsGitHubRateLimitDuringLegacyFallback() = runTest {
        val result = AndroidUpdateDiscovery.check(189L) { url ->
            if (url == mainManifestUrl) throw HttpRequestException(statusCode = 404)
            throw HttpRequestException(statusCode = 429)
        }
        val invalid = assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
        assertTrue(invalid.reason.contains("rate limit"))
    }

    @Test
    fun reportsGitHubServerErrorDuringLegacyFallback() = runTest {
        val result = AndroidUpdateDiscovery.check(189L) { url ->
            if (url == mainManifestUrl) throw HttpRequestException(statusCode = 404)
            throw HttpRequestException(statusCode = 503)
        }
        val invalid = assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
        assertTrue(invalid.reason.contains("HTTP 503"))
    }

    @Test
    fun reportsTimeout() = runTest {
        val result = AndroidUpdateDiscovery.check(189L) { throw HttpRequestException(timedOut = true) }
        val invalid = assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
        assertTrue(invalid.reason.contains("timed out"))
    }

    @Test
    fun malformedLegacyReleaseApiFailsClosed() = runTest {
        val result = AndroidUpdateDiscovery.check(189L) { url ->
            if (url == mainManifestUrl) throw HttpRequestException(statusCode = 404)
            "not-json"
        }
        assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
    }

    @Test
    fun malformedLegacyManifestFailsClosed() = runTest {
        val api = "[${release("v2.0.76")}]"
        val result = AndroidUpdateDiscovery.check(189L) { url ->
            when {
                url == mainManifestUrl -> throw HttpRequestException(statusCode = 404)
                url.contains("/releases?") -> api
                else -> "{invalid"
            }
        }
        assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
    }

    @Test
    fun versionCodePreventsDowngradeAndSameVersionReinstall() = runTest {
        val result = AndroidUpdateDiscovery.check(191L) { manifest() }
        assertEquals(AndroidUpdateDiscovery.Result.UpToDate, result)
    }

    @Test
    fun rejectsInvalidCanonicalManifestInsteadOfFallingBack() = runTest {
        val invalidManifest = manifest().replace("\"versionCode\": 191", "\"versionCode\": 0")
        val result = AndroidUpdateDiscovery.check(189L) { url ->
            if (url == mainManifestUrl) invalidManifest else error("Must not fallback on invalid canonical manifest")
        }
        assertIs<AndroidUpdateDiscovery.Result.InvalidRelease>(result)
    }
}
