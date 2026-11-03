package com.inspiredandroid.kai.updater

import com.inspiredandroid.kai.Version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Performs a plain HTTP GET and returns the raw response body.
 * Uses HTTP-error semantics: throws on non-2xx responses.
 * Platform-specific implementation (JVM/HttpURLConnection, JS/ktor, iOS/ktor).
 */
internal expect suspend fun httpGet(url: String, headers: Map<String, String>): String

internal class HttpRequestException(
    val statusCode: Int? = null,
    val timedOut: Boolean = false,
    cause: Throwable? = null,
) : Exception(
    when {
        statusCode != null -> "HTTP $statusCode"
        timedOut -> "Request timed out"
        else -> "Network request failed"
    },
    cause,
)

/**
 * Identifies the current platform for release-asset selection:
 * "windows", "macos-arm64", "macos-x64", "linux-arm64", "linux-x64" or "none".
 * "none" means auto-update is not applicable on this platform.
 * Platform-specific implementation (JVM/system properties, JS/iOS fixed).
 */
internal expect fun currentPlatformKey(): String

/**
 * Auto-update checker for Brox Kai 2.0.
 * All logic is common; platform differences are isolated behind
 * [httpGet] and [currentPlatformKey] expect/actual functions.
 */
object AutoUpdater {

    sealed interface UpdateState {
        object Idle : UpdateState
        object Checking : UpdateState
        object UpToDate : UpdateState
        data class UpdateAvailable(
            val version: String,
            val releaseNotes: String,
            val downloadUrl: String,
            val assetName: String,
        ) : UpdateState
        data class Error(val message: String) : UpdateState
    }

    data class UpdateChecker(val onStart: () -> Unit)

    private const val REPO_OWNER = "habibbrox2"
    private const val REPO_NAME = "brox-kai-2.0"
    private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"
    private const val TAG_PREFIX = "v2.0."

    private val json = Json { ignoreUnknownKeys = true }

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
        val body: String?,
        val assets: List<ReleaseAsset>,
    )

    fun createUpdateChecker(
        scope: CoroutineScope,
        onStateChange: (UpdateState) -> Unit,
    ): UpdateChecker {
        val checkMutex = Mutex()
        var lastCheckMs: Long? = null

        fun checkForUpdates() {
            scope.launch {
                // Throttle to at most one check per 4 hours.
                val due = checkMutex.withLock {
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val allowed = lastCheckMs == null || nowMs - lastCheckMs!! >= 4.hours.inWholeMilliseconds
                    if (allowed) lastCheckMs = nowMs
                    allowed
                }
                if (!due) return@launch

                onStateChange(UpdateState.Checking)
                try {
                    val latest = fetchLatestRelease()
                    if (latest != null && isNewer(latest.tag_name)) {
                        val asset = pickAssetFor(latest.assets)
                        if (asset != null) {
                            onStateChange(
                                UpdateState.UpdateAvailable(
                                    version = latest.tag_name.removePrefix("v"),
                                    releaseNotes = latest.body ?: "",
                                    downloadUrl = asset.browser_download_url,
                                    assetName = asset.name,
                                ),
                            )
                        } else {
                            onStateChange(UpdateState.UpToDate)
                        }
                    } else {
                        onStateChange(UpdateState.UpToDate)
                    }
                } catch (e: Exception) {
                    onStateChange(UpdateState.Error(e.message ?: "Unknown error"))
                }
            }
        }

        checkForUpdates()

        return UpdateChecker(onStart = { checkForUpdates() })
    }

    private suspend fun fetchLatestRelease(): GitHubRelease? {
        val body = httpGet(
            url = API_URL,
            headers = mapOf(
                "Accept" to "application/vnd.github+json",
                "User-Agent" to "BroxKai-Updater/1.0",
            ),
        )
        val releases = json.decodeFromString<List<GitHubRelease>>(body)
        return releases.firstOrNull { !it.draft && !it.prerelease && it.tag_name.startsWith(TAG_PREFIX) }
    }

    internal fun isNewer(remoteTag: String, localVersion: String = Version.appVersion): Boolean {
        val remote = remoteTag.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val local = localVersion.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0..max(remote.lastIndex, local.lastIndex)) {
            val r = remote.getOrElse(i) { 0 }
            val l = local.getOrElse(i) { 0 }
            if (r != l) return r > l
        }
        return false
    }

    /**
     * Picks the release asset matching the given platform key.
     * ARM64 variants (`*-aarch64.*`) are preferred/tried-against separately and
     * never win for a non-ARM64 platform, with an ARM64 asset kept as a last
     * fallback for ARM64 hosts only.
     */
    internal fun pickAssetFor(assets: List<ReleaseAsset>, platformKey: String = currentPlatformKey()): ReleaseAsset? {
        val isArm64 = platformKey.endsWith("arm64")
        val arm64Assets = assets.filter { it.name.endsWith("-aarch64.deb") || it.name.endsWith("-aarch64.AppImage") || it.name.endsWith("-aarch64.tar.gz") || it.name.endsWith("-aarch64.dmg") }
        val plainAssets = assets - arm64Assets.toSet()
        return when {
            platformKey.startsWith("windows") -> plainAssets.firstOrNull { it.name.endsWith(".msi") }

            platformKey.startsWith("macos") ->
                (if (isArm64) arm64Assets.firstOrNull { it.name.endsWith("-aarch64.dmg") } else plainAssets.firstOrNull { it.name.endsWith(".dmg") })

            platformKey.startsWith("linux") -> if (isArm64) {
                arm64Assets.firstOrNull { it.name.endsWith("-aarch64.deb") }
                    ?: arm64Assets.firstOrNull { it.name.endsWith("-aarch64.AppImage") }
                    ?: arm64Assets.firstOrNull { it.name.endsWith("-aarch64.tar.gz") }
            } else {
                plainAssets.firstOrNull { it.name.endsWith(".deb") || it.name.endsWith(".AppImage") || it.name.endsWith(".tar.gz") }
            }

            else -> null
        }
    }
}
