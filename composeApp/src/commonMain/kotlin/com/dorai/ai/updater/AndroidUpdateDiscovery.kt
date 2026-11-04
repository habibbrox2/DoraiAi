package com.dorai.ai.updater

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Discovers the newest stable Android release and validates its published update manifest.
 *
 * The manifest is fetched only from the matching GitHub release asset. Its APK URL, package,
 * version/tag relationship, checksums and signer fingerprint are validated before it is returned.
 */
object AndroidUpdateDiscovery {
    private const val RELEASES_URL =
        "https://api.github.com/repos/habibbrox2/DoraiAi/releases?per_page=100"
    private const val TAG_PREFIX = "v"
    private const val MANIFEST_ASSET = "update.json"
    private const val MAIN_MANIFEST_URL = "https://raw.githubusercontent.com/habibbrox2/DoraiAi/main/update.json"

    private val releaseJson = Json { ignoreUnknownKeys = true }

    @Serializable
    data class ReleaseAsset(
        val name: String,
        val browser_download_url: String,
    )

    @Serializable
    data class GitHubRelease(
        val tag_name: String,
        val draft: Boolean,
        val prerelease: Boolean,
        val assets: List<ReleaseAsset>,
    )

    sealed interface Result {
        data object UpToDate : Result
        data class UpdateAvailable(val manifest: AndroidUpdateManifest, val releaseTag: String) : Result
        data class InvalidRelease(val reason: String) : Result
    }

    private fun compareStableTags(left: String, right: String): Int {
        val leftParts = left.removePrefix(TAG_PREFIX).split('.')
        val rightParts = right.removePrefix(TAG_PREFIX).split('.')
        for (index in 0..2) {
            val leftPart = leftParts[index]
            val rightPart = rightParts[index]
            val lengthComparison = leftPart.length.compareTo(rightPart.length)
            if (lengthComparison != 0) return lengthComparison
            val valueComparison = leftPart.compareTo(rightPart)
            if (valueComparison != 0) return valueComparison
        }
        return 0
    }

    private fun httpFailureReason(exception: HttpRequestException): String = when (exception.statusCode) {
        403 -> "GitHub API access was forbidden (HTTP 403); rate limit or repository access may be restricted"
        404 -> "GitHub update resource was not found (HTTP 404)"
        429 -> "GitHub API rate limit exceeded (HTTP 429); try again later"
        in 500..599 -> "GitHub service is temporarily unavailable (HTTP " + exception.statusCode + ")"
        null -> if (exception.timedOut) "GitHub request timed out" else "Network request failed"
        else -> "GitHub request failed (HTTP " + exception.statusCode + ")"
    }

    suspend fun check(
        installedVersionCode: Long,
        fetchText: suspend (String) -> String = { url ->
            httpGet(
                url,
                mapOf(
                    "Accept" to "application/vnd.github+json",
                    "User-Agent" to "DORAI-Android-Updater/1.0",
                ),
            )
        },
    ): Result {
        if (installedVersionCode <= 0L) return Result.InvalidRelease("Invalid installed version code")
        // Prefer the canonical manifest published to main after a successful stable release.
        // Only a missing file (404) falls back to the legacy per-release asset lookup.
        val canonicalManifest = try {
            AndroidUpdateManifest.parse(fetchText(MAIN_MANIFEST_URL))
        } catch (exception: HttpRequestException) {
            if (exception.statusCode != 404) {
                return Result.InvalidRelease("Unable to download main update manifest: ${httpFailureReason(exception)}")
            }
            null
        } catch (_: SerializationException) {
            return Result.InvalidRelease("Main update manifest was not valid JSON")
        } catch (_: Exception) {
            return Result.InvalidRelease("Network error while reading main update manifest")
        }

        if (canonicalManifest?.packageName == AndroidUpdateManifest.EXPECTED_PACKAGE_NAME) {
            val tag = "v${canonicalManifest.versionName}"
            when (val validation = canonicalManifest.validateForRelease(tag)) {
                ManifestValidation.Valid -> Unit
                is ManifestValidation.Invalid -> return Result.InvalidRelease(validation.reason)
            }
            return if (canonicalManifest.isUpgradeFrom(installedVersionCode)) {
                Result.UpdateAvailable(canonicalManifest, tag)
            } else {
                Result.UpToDate
            }
        }

        val releases = try {
            releaseJson.decodeFromString<List<GitHubRelease>>(fetchText(RELEASES_URL))
        } catch (exception: HttpRequestException) {
            return Result.InvalidRelease(httpFailureReason(exception))
        } catch (_: SerializationException) {
            return Result.InvalidRelease("GitHub releases response was not valid JSON")
        } catch (_: Exception) {
            return Result.InvalidRelease("Network error while reading GitHub releases")
        }
        val stable = releases
            .filter { release ->
                !release.draft &&
                    !release.prerelease &&
                    release.tag_name.matches(Regex("^v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$")) &&
                    release.assets.any { asset ->
                        asset.name == "DORAI-AI-${release.tag_name}-Android.apk"
                    }
            }
            .maxWithOrNull { left, right -> compareStableTags(left.tag_name, right.tag_name) }
            ?: return Result.UpToDate

        val manifestAsset = stable.assets.singleOrNull { it.name == MANIFEST_ASSET }
            ?: return Result.InvalidRelease("Stable release is missing a unique update.json asset")
        if (manifestAsset.browser_download_url !=
            "https://github.com/habibbrox2/DoraiAi/releases/download/${stable.tag_name}/update.json"
        ) {
            return Result.InvalidRelease("Unexpected update manifest URL")
        }

        val manifest = try {
            AndroidUpdateManifest.parse(fetchText(manifestAsset.browser_download_url))
        } catch (exception: HttpRequestException) {
            val detail = exception.statusCode?.let { "HTTP $it" } ?: if (exception.timedOut) "request timed out" else "network request failed"
            return Result.InvalidRelease("Unable to download update manifest: $detail")
        } catch (_: SerializationException) {
            return Result.InvalidRelease("Update manifest was not valid JSON")
        } catch (_: Exception) {
            return Result.InvalidRelease("Network error while reading update manifest")
        }
        when (val validation = manifest.validateForRelease(stable.tag_name)) {
            ManifestValidation.Valid -> Unit
            is ManifestValidation.Invalid -> return Result.InvalidRelease(validation.reason)
        }
        return if (manifest.isUpgradeFrom(installedVersionCode)) {
            Result.UpdateAvailable(manifest, stable.tag_name)
        } else {
            Result.UpToDate
        }
    }
}
