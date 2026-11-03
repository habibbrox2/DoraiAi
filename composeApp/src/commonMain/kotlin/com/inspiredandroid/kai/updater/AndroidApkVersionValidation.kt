package com.inspiredandroid.kai.updater

/** Validates that a downloaded APK's version is exactly the release manifest version and is an upgrade. */
object AndroidApkVersionValidation {
    sealed interface Result {
        data object Valid : Result
        data class Invalid(val reason: String) : Result
    }

    fun validate(
        installedVersionCode: Long,
        manifestVersionCode: Long,
        apkVersionCode: Long,
    ): Result {
        if (installedVersionCode <= 0L || manifestVersionCode <= 0L || apkVersionCode <= 0L) {
            return Result.Invalid("Invalid APK version code")
        }
        if (manifestVersionCode <= installedVersionCode) {
            return Result.Invalid("Update is not newer than the installed app")
        }
        if (apkVersionCode != manifestVersionCode) {
            return Result.Invalid("Downloaded APK version does not match release manifest")
        }
        return Result.Valid
    }
}
