package com.dorai.ai

import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.dorai.ai.filereview.FileReviewController
import com.dorai.ai.network.tools.Tool
import com.dorai.ai.network.tools.ToolInfo
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import kotlin.coroutines.CoroutineContext

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

expect fun createSecureSettings(): Settings

expect fun createLegacySettings(): Settings?

expect fun resolveHostAddresses(hostname: String): List<String>

/**
 * True when [resolveHostAddresses] can actually resolve DNS on this platform.
 * iOS and Web return an empty list for every hostname (no DNS in their engines),
 * so SSRF guards must skip resolved-IP validation there instead of failing every fetch.
 */
expect val supportsDnsResolution: Boolean

expect fun getBackgroundDispatcher(): CoroutineContext

expect fun onDragAndDropEventDropped(event: DragAndDropEvent): PlatformFile?

expect val BackIcon: ImageVector

sealed class Platform(val displayName: String) {
    sealed class Mobile(displayName: String) : Platform(displayName) {
        data object Android : Mobile("Android")
        data object Ios : Mobile("iOS")
    }

    sealed class Desktop(displayName: String) : Platform(displayName) {
        data object Mac : Desktop("macOS")
        data object Windows : Desktop("Windows")
        data object Linux : Desktop("Linux")
    }

    data object Web : Platform("Web")
}

expect val currentPlatform: Platform

expect val defaultUiScale: Float

/** True when the user has requested reduced motion (OS accessibility setting). */
expect fun prefersReducedMotion(): Boolean

expect fun getAppFilesDirectory(): String

/**
 * Applies the app display language (best-effort, platform-specific).
 * On JVM this sets the default Locale; on other platforms it may be a no-op.
 */
expect fun applyAppLanguage(languageCode: String)

/**
 * Returns the SIM operator as MCC+MNC (e.g. "47001") if available, or null.
 * Android: TelephonyManager.getSimOperator(); iOS: CoreTelephony.
 * Desktop/Web return null.
 */
expect fun getSimOperatorCode(): String?

/**
 * Returns a map of device/platform information usable by the agent.
 * Keys include at least "platform" and "appVersion"; platforms may add more.
 */
expect fun getDeviceInfo(): Map<String, Any>

expect fun getAvailableTools(): List<Tool>

/** Creates the durable File Review controller for the current platform. */
expect fun createFileReviewController(): FileReviewController

/**
 * Returns all raw tool definitions available on this platform.
 * The returned tools have no isEnabled state set - that's handled by RemoteDataRepository.
 * Unlike getAvailableTools(), this returns all tools regardless of enabled state.
 */
expect fun getPlatformToolDefinitions(): List<ToolInfo>

expect val isEmailSupported: Boolean

/**
 * True only on the FOSS Android build. Gated on `READ_SMS` being declared in the
 * merged manifest — the Play Store flavor doesn't declare it, so this returns
 * false there, and the SMS feature is invisible in that build.
 */
expect val isSmsSupported: Boolean

/**
 * True only on the FOSS Android build. Gated on `DoraiNotificationListenerService`
 * being declared in the merged manifest — the Play Store flavor doesn't declare
 * it, so this returns false there, and the notification-reading feature is
 * invisible in that build.
 */
expect val isNotificationsSupported: Boolean

expect val isSplinterlandsSupported: Boolean

/**
 * True only on Android. Voice input needs the platform speech recognizer, which the
 * desktop, iOS and web targets do not have yet. The feature plan scopes this
 * iteration to Android; the other targets keep the voice entry points hidden.
 */
expect val isVoiceSupported: Boolean

expect suspend fun compressImageBytes(bytes: ByteArray, mimeType: String): ByteArray

expect fun openUrl(url: String): Boolean

@androidx.compose.runtime.Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

expect fun exitApp()

expect fun decodeToImageBitmap(bytes: ByteArray): ImageBitmap?

expect suspend fun saveFileToDevice(bytes: ByteArray, baseName: String, extension: String)

/**
 * Fires a background push notification for a heartbeat that produced a non-trivial
 * response. Android additionally wires a tap-to-open-heartbeat deep link via its
 * PendingIntent; iOS/desktop just surface the message in the OS notification center
 * without deep-linking back to the conversation. No-op on web.
 */
expect fun sendHeartbeatNotification(title: String, body: String)
