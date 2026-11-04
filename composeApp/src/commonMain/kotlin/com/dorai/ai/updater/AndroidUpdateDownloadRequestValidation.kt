package com.dorai.ai.updater

/**
 * Validates the immutable release coordinates accepted by the Android downloader.
 *
 * The APK URL must be the canonical asset URL for the same v2.0.x tag encoded in the
 * asset filename. Redirect destinations are separately allowlisted by the Android worker.
 */
object AndroidUpdateDownloadRequestValidation {
    private val ASSET_PATTERN = Regex("^DORAI-AI-v([0-9]+\\.[0-9]+\\.[0-9]+)-Android\\.apk$")
    private val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$")
    private const val RELEASE_PREFIX = "https://github.com/habibbrox2/DoraiAi/releases/download/"

    fun isValid(url: String, sha256: String, assetName: String): Boolean {
        if (!SHA256_PATTERN.matches(sha256)) return false
        val match = ASSET_PATTERN.matchEntire(assetName) ?: return false
        val tag = "v${match.groupValues[1]}"
        return url == "$RELEASE_PREFIX$tag/$assetName"
    }
}
