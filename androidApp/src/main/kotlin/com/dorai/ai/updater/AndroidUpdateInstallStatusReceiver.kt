package com.dorai.ai.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File

/**
 * Explicit BroadcastReceiver handling installation status callbacks from PackageInstaller.
 *
 * Intercepts STATUS_PENDING_USER_ACTION to directly launch the official system package
 * confirmation dialog, handles installation success, and maps all system failure codes into
 * structured diagnostics.
 */
class AndroidUpdateInstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, 0L)
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: ""

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val userAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }

                if (userAction != null) {
                    userAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(userAction) }
                    AndroidUpdateSessionState.updateState(
                        AndroidUpdateSessionState.InstallState.AwaitingConfirmation(sessionId, userAction),
                    )
                } else {
                    val error = "Missing user confirmation intent from package installer"
                    AndroidUpdateSessionState.recordFailure(context, status, error)
                    AndroidUpdateSessionState.updateState(
                        AndroidUpdateSessionState.InstallState.Failed(status, error),
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                AndroidUpdateSessionState.clearActiveSession(context)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Success(versionCode, versionName),
                )
                cleanUpUpdatesDirectory(context)
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                val msg = "Installation was cancelled by the user."
                abandonSession(context, sessionId)
                AndroidUpdateSessionState.recordFailure(context, status, msg)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Failed(status, msg),
                )
            }

            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val msg = "Installation failed: Insufficient storage space on device."
                abandonSession(context, sessionId)
                AndroidUpdateSessionState.recordFailure(context, status, msg)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Failed(status, msg),
                )
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                val msg = "Installation failed: Package or signing conflict with installed app."
                abandonSession(context, sessionId)
                AndroidUpdateSessionState.recordFailure(context, status, msg)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Failed(status, msg),
                )
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                val msg = "Installation failed: Package is incompatible with this device."
                abandonSession(context, sessionId)
                AndroidUpdateSessionState.recordFailure(context, status, msg)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Failed(status, msg),
                )
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                val msg = "Installation failed: Blocked by device administrator or policy."
                abandonSession(context, sessionId)
                AndroidUpdateSessionState.recordFailure(context, status, msg)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Failed(status, msg),
                )
            }

            PackageInstaller.STATUS_FAILURE_INVALID -> {
                val msg = "Installation failed: Package archive is invalid or unreadable."
                abandonSession(context, sessionId)
                AndroidUpdateSessionState.recordFailure(context, status, msg)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Failed(status, msg),
                )
            }

            else -> {
                val extraMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                val msg = if (!extraMessage.isNullOrBlank()) {
                    "Installation failed: $extraMessage (code $status)"
                } else {
                    "Installation failed with status code $status"
                }
                abandonSession(context, sessionId)
                AndroidUpdateSessionState.recordFailure(context, status, msg)
                AndroidUpdateSessionState.updateState(
                    AndroidUpdateSessionState.InstallState.Failed(status, msg),
                )
            }
        }
    }

    private fun abandonSession(context: Context, sessionId: Int) {
        if (sessionId > 0) {
            runCatching {
                context.packageManager.packageInstaller.openSession(sessionId).abandon()
            }
        }
    }

    private fun cleanUpUpdatesDirectory(context: Context) {
        runCatching {
            val dir = File(context.filesDir, "updates")
            if (dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk") || file.name.endsWith(".part") || file.name.endsWith(".tmp")) {
                        file.delete()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.dorai.ai.updater.INSTALL_STATUS"
        const val EXTRA_VERSION_CODE = "extra_version_code"
        const val EXTRA_VERSION_NAME = "extra_version_name"
    }
}
