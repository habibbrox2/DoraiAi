package com.inspiredandroid.kai.updater

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AndroidApkVersionValidationTest {
    @Test
    fun acceptsExactNewerManifestAndApkVersion() {
        assertIs<AndroidApkVersionValidation.Result.Valid>(
            AndroidApkVersionValidation.validate(
                installedVersionCode = 189,
                manifestVersionCode = 190,
                apkVersionCode = 190,
            ),
        )
    }

    @Test
    fun rejectsDowngrade() {
        val result = AndroidApkVersionValidation.validate(190, 189, 189)
        assertEquals(
            "Update is not newer than the installed app",
            (result as AndroidApkVersionValidation.Result.Invalid).reason,
        )
    }

    @Test
    fun rejectsApkManifestVersionMismatch() {
        assertIs<AndroidApkVersionValidation.Result.Invalid>(
            AndroidApkVersionValidation.validate(189, 190, 191),
        )
    }

    @Test
    fun rejectsNonPositiveVersionCodes() {
        assertIs<AndroidApkVersionValidation.Result.Invalid>(
            AndroidApkVersionValidation.validate(0, 190, 190),
        )
    }
}
