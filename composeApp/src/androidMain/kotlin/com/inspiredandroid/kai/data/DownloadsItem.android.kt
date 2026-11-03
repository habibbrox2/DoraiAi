package com.inspiredandroid.kai.data

import android.app.DownloadManager

/**
 * Android-specific extensions for DownloadsItem.
 * Provides DownloadManager-specific functionality.
 */
@OptIn(ExperimentalStdlibApi::class)
fun DownloadsItem.statusDisplayName(): String? = status?.let { currentStatus ->
    when (currentStatus) {
        DownloadManager.STATUS_PENDING -> "Pending"
        DownloadManager.STATUS_RUNNING -> "Running"
        DownloadManager.STATUS_PAUSED -> "Paused"
        DownloadManager.STATUS_SUCCESSFUL -> "Completed"
        DownloadManager.STATUS_FAILED -> "Failed"
        else -> "Unknown ($currentStatus)"
    }
}

/** ডাউনলোড সফল হয়েছে কিনা */
val DownloadsItem.isSuccessful: Boolean
    get() = status == DownloadManager.STATUS_SUCCESSFUL

/** ডাউনলোড ব্যর্থ হয়েছে কিনা */
val DownloadsItem.isFailed: Boolean
    get() = status == DownloadManager.STATUS_FAILED

/** ডাউনলোড চলছে কিনা */
val DownloadsItem.isActive: Boolean
    get() = status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING
