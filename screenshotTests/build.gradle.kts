plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.dorai.ai.screenshots"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    // Include composeApp's assets (which contain compose resources)
    sourceSets {
        getByName("main") {
            assets.directories.add(
                project(":composeApp").file("build/generated/assets/copyAndroidMainComposeResourcesToAndroidAssets").path,
            )
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    testImplementation(project(":composeApp"))
    // Required for types used directly in test code (KMP doesn't expose transitively)
    testImplementation(libs.filekit.core)
    implementation(libs.tts)
    implementation(libs.tts.compose)
    testImplementation(libs.kotlinx.collections.immutable)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.compose.material3)
    testImplementation(libs.compose.components.resources)
    testImplementation(libs.multiplatform.settings.test)
    // AppSettings exposes Settings in its constructor; keep the API on the test compile classpath.
    testImplementation(libs.multiplatform.settings)
    testImplementation(libs.koin.compose)
    testImplementation(libs.koin.core)
    // Screenshot destination graph uses the same typed Navigation Compose routes as App.kt.
    testImplementation(libs.androidx.navigation.compose)
}

// Ensure composeApp resources are generated before screenshot tests
tasks.matching { it.name.contains("preparePaparazzi") }.configureEach {
    dependsOn(":composeApp:copyAndroidMainComposeResourcesToAndroidAssets")
}

// Paparazzi 2.0.0-alpha04's HTML reporter calls a Gradle internal API removed in 9.4
tasks.withType<Test>().configureEach {
    reports.html.required.set(false)
}

// Record DORAI UI component screenshots only (faster than the full suite).
val recordingDoraiUiScreenshots =
    gradle.startParameter.taskNames.any {
        it == "recordKaiUiScreenshots" || it.endsWith(":recordKaiUiScreenshots")
    }

// Configure the test filter lazily: testDebugUnitTest is created by the
// Android Gradle Plugin and is not available during this script's early phase.
tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    val task = this as Test
    if (gradle.startParameter.projectProperties["includeStoreScreenshots"] == "true" ||
        gradle.startParameter.taskNames.any { it.contains("generateStoreScreenshots") }
    ) {
        task.filter.includeTestsMatching("*.StoreScreenshotTest")
        task.filter.includeTestsMatching("*.TabletStoreScreenshotTest")
        // Phone and tablet test classes run in isolated workers; they write distinct snapshot names.
        task.maxParallelForks = 2
    } else {
        task.filter.excludeTestsMatching("*.StoreScreenshotTest")
        task.filter.excludeTestsMatching("*.TabletStoreScreenshotTest")
    }

    if (recordingDoraiUiScreenshots) {
        task.filter.includeTestsMatching("*.DoraiUiScreenshotTest")
    }
}

// Record DORAI UI component screenshots only (faster than the full suite).

tasks.register("recordKaiUiScreenshots") {
    group = "verification"
    description = "Records golden images for DoraiUiScreenshotTest only."
    dependsOn("recordPaparazziDebug")
}

// Task to copy screenshots to fastlane and README locations
tasks.register("updateScreenshots") {
    dependsOn("recordPaparazziDebug")

    doLast {
        val snapshotsDir = file("src/test/snapshots/images")
        val readmeDir = rootProject.file("screenshots").also { it.mkdirs() }
        val siteImgDir = rootProject.file("site/img")

        // (source-key contains â†’ destination file). Source-key is a substring of the
        // Paparazzi filename; destination is the resolved path on disk.
        val copies =
            buildList<Pair<String, java.io.File>> {
                // Fastlane-style phone screenshots from ScreenshotTest â†’ screenshots/
                mapOf(
                    "ScreenshotTest_chatEmptyState_premiumDark" to "mobile-1.png",
                    "ScreenshotTest_chatWithMessages_dark" to "mobile-2.png",
                    "ScreenshotTest_chatWithDynamicUi_light" to "mobile-3.png",
                    "ScreenshotTest_settingsFree_dark" to "mobile-4.png",
                    "ScreenshotTest_settingsTools_light" to "mobile-5.png",
                    "ScreenshotTest_settingsGeneral_dark" to "mobile-6.png",
                    "ScreenshotTest_settingsSandbox_dark" to "mobile-7.png",
                ).forEach { (k, v) -> add(k to readmeDir.resolve(v)) }

                // Hero carousel screenshots from DoraiUiScreenshotTest â†’ site/img/
                mapOf(
                    "DoraiUiScreenshotTest_scenario_survivalGame_dark" to "survival-dark.png",
                    "DoraiUiScreenshotTest_scenario_recipeCard_light" to "recipe-light.png",
                    "DoraiUiScreenshotTest_scenario_sustainableTech_light" to "ecopulse-light.png",
                    "DoraiUiScreenshotTest_scenario_memories_dark" to "memories-dark.png",
                ).forEach { (k, v) -> add(k to siteImgDir.resolve(v)) }

                // /run-gemma-locally/ landing-page screenshots â†’ site/img/
                mapOf(
                    "GemmaLocalScreenshotTest_gemmaLocal_settings_dark" to "gemma-local-settings.png",
                    "GemmaLocalScreenshotTest_gemmaLocal_modelCard_dark" to "gemma-local-model-card.png",
                    "GemmaLocalScreenshotTest_gemmaLocal_contextSlider_dark" to "gemma-local-context-slider.png",
                    "GemmaLocalScreenshotTest_gemmaLocal_download_dark" to "gemma-local-download.png",
                    "GemmaLocalScreenshotTest_gemmaLocal_select_dark" to "gemma-local-select.png",
                    "GemmaLocalScreenshotTest_gemmaLocal_chat_dark" to "gemma-local-chat.png",
                ).forEach { (k, v) -> add(k to siteImgDir.resolve(v)) }

                // /math/ landing-page screenshots â†’ site/img/
                mapOf(
                    "MathScreenshotTest_math_algebra_light" to "math-algebra.png",
                    "MathScreenshotTest_math_calculus_dark" to "math-calculus.png",
                    "MathScreenshotTest_math_physics_dark" to "math-physics.png",
                    "MathScreenshotTest_math_structures_light" to "math-structures.png",
                    "MathScreenshotTest_math_notation_light" to "math-notation.png",
                ).forEach { (k, v) -> add(k to siteImgDir.resolve(v)) }
            }

        val files = snapshotsDir.listFiles()?.filter { it.isFile && it.extension == "png" } ?: emptyList()
        copies.forEach { (key, dest) ->
            val matches = files.filter { it.name.contains(key) }
            check(matches.size == 1) {
                "Expected exactly one Paparazzi snapshot containing '$key', found ${matches.size}: ${matches.joinToString { it.name }}"
            }
            val match = matches.single()
            dest.parentFile.mkdirs()
            match.copyTo(dest, overwrite = true)
            println("Copied ${match.name} -> ${dest.relativeTo(rootProject.projectDir)}")
        }
    }
}

// Task to generate localized store screenshots and copy to fastlane structure.
// All snapshots are staged and validated before Fastlane is touched. Existing
// assets are backed up, replacements are copied only after a complete 54-locale
// matrix is present, and any copy failure restores the previous directories.
tasks.register("generateStoreScreenshots") {
    group = "distribution"
    description = "Records and safely refreshes localized phone/tablet store screenshots."
    dependsOn("recordPaparazziDebug")

    doLast {
        val snapshotsDir = file("src/test/snapshots/images")
        val fastlaneDir = rootProject.file("fastlane/metadata/android")
        val stagingRoot =
            layout.buildDirectory
                .dir("store-screenshots-staging")
                .get()
                .asFile
        delete(stagingRoot)
        check(stagingRoot.mkdirs()) { "Could not create staging directory: $stagingRoot" }

        val phoneRegex = Regex("""StoreScreenshotTest_\w+\[([^\]]+)\]_store_[a-zA-Z0-9-]+_(\d+(?:_\w+)?)\.png""")
        val tabletRegex = Regex("""TabletStoreScreenshotTest_\w+\[([^\]]+)\]_tablet_[a-zA-Z0-9-]+_(\d+(?:_\w+)?)\.png""")
        val allSnapshots = snapshotsDir.listFiles()?.filter { it.isFile && it.extension == "png" } ?: emptyList()
        val phoneSnapshots =
            allSnapshots.filter {
                it.name.contains("StoreScreenshotTest_") && !it.name.contains("Tablet") && it.name.contains("_store_")
            }
        val tabletSnapshots =
            allSnapshots.filter {
                it.name.contains("TabletStoreScreenshotTest_") && it.name.contains("_tablet_")
            }

        val stagedPhone = linkedMapOf<String, MutableSet<String>>()
        val stagedTablet = linkedMapOf<String, MutableSet<String>>()

        fun stage(
            sources: List<File>,
            regex: Regex,
            folder: String,
            index: MutableMap<String, MutableSet<String>>,
        ) {
            sources.forEach { source ->
                val match =
                    regex.find(source.name)
                        ?: error("Unrecognized store snapshot filename: ${source.name}")
                val locale = match.groupValues[1]
                val number =
                    match.groupValues[2]
                        .substringBefore("_")
                        .trimStart('0')
                        .ifEmpty { "0" }
                val target = File(stagingRoot, "$locale/images/$folder/${number}_$locale.png")
                check(!target.exists()) { "Duplicate staged store screenshot: $target" }
                target.parentFile.mkdirs()
                source.copyTo(target)
                check(target.length() > 1_000L) { "Staged screenshot is empty or suspiciously small: $source" }
                index.getOrPut(locale) { linkedSetOf() }.add(number)
            }
        }

        stage(phoneSnapshots, phoneRegex, "phoneScreenshots", stagedPhone)
        stage(tabletSnapshots, tabletRegex, "tenInchScreenshots", stagedTablet)

        val expectedLocales =
            fastlaneDir
                .listFiles()
                ?.filter { it.isDirectory }
                ?.filter {
                    File(it, "images/phoneScreenshots").isDirectory &&
                        File(it, "images/tenInchScreenshots").isDirectory
                }?.map { it.name }
                ?.toSet()
                ?: emptySet()
        val expectedIndexes = setOf("1", "2", "3", "4", "5", "6", "7")

        check(expectedLocales.size == 54) {
            "Expected 54 Fastlane locales, found ${expectedLocales.size}; refusing replacement."
        }
        expectedLocales.forEach { locale ->
            check(stagedPhone[locale] == expectedIndexes) {
                "Phone screenshot matrix for $locale is incomplete: ${stagedPhone[locale]}"
            }
            check(stagedTablet[locale] == expectedIndexes) {
                "Tablet screenshot matrix for $locale is incomplete: ${stagedTablet[locale]}"
            }
        }
        check(stagedPhone.keys == expectedLocales && stagedTablet.keys == expectedLocales) {
            "Staged locale set does not exactly match Fastlane locale set."
        }

        val backupRoot =
            layout.buildDirectory
                .dir("store-screenshots-backup")
                .get()
                .asFile
                .resolve("backup-${System.currentTimeMillis()}")
        expectedLocales.forEach { locale ->
            listOf("phoneScreenshots", "tenInchScreenshots").forEach { folder ->
                val current = File(fastlaneDir, "$locale/images/$folder")
                val backup = File(backupRoot, "$locale/images/$folder")
                backup.parentFile.mkdirs()
                check(current.copyRecursively(backup, overwrite = true)) {
                    "Could not back up existing screenshots for $locale/$folder"
                }
            }
        }

        try {
            expectedLocales.forEach { locale ->
                listOf("phoneScreenshots", "tenInchScreenshots").forEach { folder ->
                    val staged = File(stagingRoot, "$locale/images/$folder")
                    val target = File(fastlaneDir, "$locale/images/$folder")
                    staged.listFiles()?.filter { it.isFile && it.extension == "png" }?.forEach { image ->
                        image.copyTo(File(target, image.name), overwrite = true)
                    }
                }
            }
            expectedLocales.forEach { locale ->
                listOf("phoneScreenshots", "tenInchScreenshots").forEach { folder ->
                    val target = File(fastlaneDir, "$locale/images/$folder")
                    val count = target.listFiles()?.count { it.isFile && it.extension == "png" } ?: 0
                    check(count == 7) { "Post-copy verification failed for $locale/$folder: found $count PNGs" }
                }
            }
        } catch (failure: Throwable) {
            expectedLocales.forEach { locale ->
                listOf("phoneScreenshots", "tenInchScreenshots").forEach { folder ->
                    val backup = File(backupRoot, "$locale/images/$folder")
                    val target = File(fastlaneDir, "$locale/images/$folder")
                    if (backup.exists()) backup.copyRecursively(target, overwrite = true)
                }
            }
            throw failure
        }

        println("Successfully refreshed 54 locales Ã— 7 phone + 7 tablet screenshots.")
        println("Validated staging: $$stagingRoot")
        println("Fastlane backup: $$backupRoot")
    }
}
