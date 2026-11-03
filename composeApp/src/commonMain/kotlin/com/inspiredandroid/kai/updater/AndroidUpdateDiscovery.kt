package com.inspiredandroid.kai.updater

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
        "https://api.github.com/repos/habibbrox2/brox-kai-2.0/releases?per_page=100"
    private const val TAG_PREFIX = "v2.0."
    private const val MANIFEST_ASSET = "update.json"
    private const val MAIN_MANIFEST_URL = "https://raw.githubusercontent.com/habibbrox2/brox-kai-2.0/main/update.json"

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
                    "User-Agent" to "BroxKai-Android-Updater/1.0",
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

        if (canonicalManifest != null) {
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
            .filter { !it.draft && !it.prerelease && it.tag_name.matches(Regex("^v2\\.0\\.[0-9]+$")) }
            .maxByOrNull { it.tag_name.removePrefix("v").substringAfterLast('.').toLongOrNull() ?: -1L }
            ?: return Result.UpToDate

        val manifestAsset = stable.assets.singleOrNull { it.name == MANIFEST_ASSET }
            ?: return Result.InvalidRelease("Stable release is missing a unique update.json asset")
        if (manifestAsset.browser_download_url !=
            "https://github.com/habibbrox2/brox-kai-2.0/releases/download/${stable.tag_name}/update.json"
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
