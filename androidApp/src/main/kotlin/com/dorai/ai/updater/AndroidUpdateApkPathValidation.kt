package com.dorai.ai.updater

import java.io.File

/** Ensures an installer candidate is the exact downloaded asset in app-private update storage. */
internal object AndroidUpdateApkPathValidation {
    fun isValid(updatesDirectory: File, apkFile: File, expectedAssetName: String): Boolean {
        val directory = runCatching { updatesDirectory.canonicalFile }.getOrNull() ?: return false
        val candidate = runCatching { apkFile.canonicalFile }.getOrNull() ?: return false
        return candidate.parentFile == directory &&
            candidate.name == expectedAssetName &&
            candidate.isFile &&
            candidate.length() > 0L
    }
}
