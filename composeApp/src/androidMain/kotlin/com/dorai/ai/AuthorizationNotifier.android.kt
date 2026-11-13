package com.dorai.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.dorai.ai.shared.R
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.notification_channel_description
import dorai.composeapp.generated.resources.notification_channel_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.java.KoinJavaComponent.inject

/** Shared with the AI `send_notification` tool — ensures the channel is created once. */
private const val CHANNEL_ID = "kai_ai_notifications"

/**
 * Fixed ID for the "authorization completed" notice. A new authorization replaces the
 * previous unread one instead of piling up in the tray.
 */
private const val AUTHORIZATION_NOTIFICATION_ID = 9003

/**
 * Posts the "authorization finished, come back to the app" notice.
 *
 * The tap opens the app on the settings surface that matches the flow: the MCP OAuth
 * hand-off page and the GitHub device flow both finish in a browser, so the app is in the
 * background and the notice is how the user gets back to it.
 */
actual fun sendAuthorizationNotification(
    title: String,
    body: String,
    destination: AuthorizationDestination,
) {
    val context: Context by inject(Context::class.java)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    ensureAuthorizationChannel(notificationManager)

    val returnUri = when (destination) {
        AuthorizationDestination.Mcp -> DeepLinks.OAUTH_RETURN_URI
        AuthorizationDestination.GitHub -> DeepLinks.GITHUB_RETURN_URI
    }
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action = Intent.ACTION_VIEW
        data = Uri.parse(returnUri)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        AUTHORIZATION_NOTIFICATION_ID,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(AUTHORIZATION_NOTIFICATION_ID, notification)
}

private fun ensureAuthorizationChannel(manager: NotificationManager) {
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    val name = runBlocking { getString(Res.string.notification_channel_name) }
    val description = runBlocking { getString(Res.string.notification_channel_description) }
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
            this.description = description
        },
    )
}
