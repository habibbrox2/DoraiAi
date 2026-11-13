import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("DoraIDatabase") {
            packageName.set("com.dorai.ai.db")
            // The schema lives on user devices — structural changes need a
            // numbered .sqm migration file (see conversation.sq header).
            // This makes the build verify that migrations reproduce the schema.
            verifyMigrations.set(true)
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(project.layout.projectDirectory.file("compose_stability.conf"))
}

kotlin {
    android {
        namespace = "com.dorai.ai.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            // Dynamic so LiteRT-LM's `-Xlinker -all_load` (in its Package.swift) doesn't
            // sweep up ComposeApp's static archive too and trip thousands of duplicate
            // symbols at link time. Each framework gets its own link context.
            isStatic = false
            // Must differ from the iosApp bundle identifier — iOS refuses to install a
            // .app whose embedded framework shares its parent's identifier (MIInstaller
            // error 57 / DuplicateIdentifier).
            binaryOption("bundleId", "com.dorai.ai.composeapp")
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        // Serve sources to debug inside browser
                        static(rootDirPath)
                        static(projectDirPath)
                    }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/src/commonMain/kotlin"))
            dependencies {
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.core)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.components.uiToolingPreview)

                implementation(libs.androidx.navigation.compose)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)

                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)

                implementation(libs.tts)
                implementation(libs.tts.compose)

                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.core)

                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.no.arg)

                implementation(libs.filekit.core)
                implementation(libs.filekit.compose)

                implementation(libs.coil.compose)
                implementation(libs.coil.svg)
                implementation(libs.coil.network.ktor3)

                implementation(libs.reorderable)

                implementation(libs.sqldelight.runtime)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.multiplatform.settings.test)
            }
        }
        androidMain {
            kotlin.srcDir("src/jvmShared/kotlin")
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.process)
                implementation(libs.spght.encryptedprefs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.koin.android)
                implementation(libs.material)
                implementation(libs.bouncycastle.provider)
                implementation(libs.litert.lm)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.xz)
                // Voice model download worker (Phase 2): WorkManager + foreground service.
                implementation(libs.androidx.work.runtime)
                // ML Kit Text Recognition v2 - OCR support
                implementation(libs.mlkit.text.recognition)
                implementation(libs.mlkit.text.recognition.chinese)
                implementation(libs.mlkit.text.recognition.devanagari)
                implementation(libs.mlkit.text.recognition.japanese)
                implementation(libs.mlkit.text.recognition.korean)
            }
        }
        getByName("desktopMain") {
            kotlin.srcDir("src/jvmShared/kotlin")
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.cio)
                implementation(libs.bouncycastle.provider)
                implementation(libs.slf4j.nop)
                implementation(libs.litert.lm.jvm)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.jna.core)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.network)
            implementation(libs.ktor.network.tls)
            implementation(libs.sqldelight.native.driver)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.dorai.ai.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(
                project.file("proguard-rules.pro"),
                project.file("proguard-desktop.pro"),
            )
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "DORAI"
            packageVersion = libs.versions.appVersion.get()

            macOS {
                bundleID = "com.dorai.ai"
                iconFile.set(project.file("icon.icns"))
            }
            windows {
                iconFile.set(project.file("icon.ico"))
                menuGroup = "DORAI"
            }
            linux {
                iconFile.set(project.file("icon.png"))
                // Applies to the shared jlink runtime image for every platform, not just Linux —
                // java.sql is required since the SQLDelight/SQLite migration (9d6ceec9).
                modules("jdk.security.auth", "java.sql")
            }
        }
    }
}

// BouncyCastle is a cryptographically signed JCE provider jar. ProGuard rewrites
// it and strips the META-INF signatures, causing "SHA-256 digest error" at
// runtime. After ProGuard finishes, replace the processed jar with the original.
afterEvaluate {
    tasks.matching { it.name == "proguardReleaseJars" }.configureEach {
        doLast {
            val proguardDir =
                layout.buildDirectory
                    .dir("compose/tmp/main-release/proguard")
                    .get()
                    .asFile
            val processedJar = proguardDir.listFiles()?.find { it.name.startsWith("bcprov") } ?: return@doLast
            val originalJar =
                configurations["desktopRuntimeClasspath"]
                    .resolve()
                    .find { it.name.startsWith("bcprov") } ?: return@doLast
            originalJar.copyTo(processedJar, overwrite = true)
            logger.lifecycle("Restored original signed BouncyCastle jar: ${processedJar.name}")
        }
    }
}

class VersionGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            val appVersion = libs.versions.appVersion.get()

            // Generate Kotlin version file
            val versionFile =
                project.layout.buildDirectory
                    .file("generated/src/commonMain/kotlin/com/dorai/ai/Version.kt")
                    .get()
                    .asFile
            versionFile.parentFile?.mkdirs()
            versionFile.writeText(
                """
                package com.dorai.ai

                object Version {
                    const val appVersion = "$appVersion"
                }
                """.trimIndent(),
            )

            // Generate the build-time GitHub client id constant (never committed).
            GitHubClientIdConfig.write(project, GitHubClientIdConfig.resolve(project))

            // Update iOS Config.xcconfig with version
            val xcConfigFile = rootProject.file("iosApp/Configuration/Config.xcconfig")
            if (xcConfigFile.exists()) {
                val content = xcConfigFile.readText()
                val updatedContent =
                    if (content.contains("APP_VERSION=")) {
                        content.replace(Regex("APP_VERSION=.*"), "APP_VERSION=$appVersion")
                    } else {
                        content.trimEnd() + "\nAPP_VERSION=$appVersion\n"
                    }
                xcConfigFile.writeText(updatedContent)
            }
        }
    }
}

/**
 * Build-time injection of the GitHub OAuth App client id (never committed).
 *
 * The value comes from a Gradle property (`-Pdorai.github.clientId=...`) or the
 * `DORAI_GITHUB_CLIENT_ID` environment variable (CI passes it from a repository secret).
 * When neither is present the generated constant is null and the integration stays
 * fail-closed — no network I/O and no "Connected" state.
 */
private object GitHubClientIdConfig {
    fun resolve(project: Project): String? =
        project.providers
            .gradleProperty("dorai.github.clientId")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: project.providers
                .environmentVariable("DORAI_GITHUB_CLIENT_ID")
                .orNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

    /** Writes the generated commonMain constant that carries the build-time client id. */
    fun write(
        project: Project,
        clientId: String?,
    ) {
        val target =
            project.layout.buildDirectory
                .file("generated/src/commonMain/kotlin/com/dorai/ai/github/GitHubBuildConfig.kt")
                .get()
                .asFile
        target.parentFile?.mkdirs()
        val literal = clientId?.let { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" } ?: "null"
        target.writeText(
            """
            package com.dorai.ai.github

            /** Build-time constants injected by the Gradle build — generated, never edited by hand. */
            object GitHubBuildConfig {
                /**
                 * GitHub OAuth App client id for the Device Flow, or null when the build supplies
                 * none. A null value keeps the GitHub integration fail-closed.
                 */
                val clientId: String? = $literal
            }
            """.trimIndent(),
        )
    }
}

// Apply the plugin to generate Version.kt
apply<VersionGeneratorPlugin>()

// Register the version generation task
tasks.register("generateVersionFile") {
    group = "build"
    description = "Generates Version.kt with app version"
    doLast {
        val appVersion = libs.versions.appVersion.get()
        val versionFile =
            project.layout.buildDirectory
                .file("generated/src/commonMain/kotlin/com/dorai/ai/Version.kt")
                .get()
                .asFile
        versionFile.parentFile?.mkdirs()
        versionFile.writeText(
            """
            package com.dorai.ai

            object Version {
                const val appVersion = "$appVersion"
            }
            """.trimIndent(),
        )
    }
}

// Register the build-time GitHub client id generation task
tasks.register("generateGitHubClientIdConfig") {
    group = "build"
    description = "Generates GitHubBuildConfig.kt with the build-time GitHub client id"
    doLast {
        GitHubClientIdConfig.write(project, GitHubClientIdConfig.resolve(project))
    }
}

// Ensure version file is generated before Kotlin compilation
tasks.named("compileKotlinDesktop") {
    dependsOn("generateVersionFile")
}

// For Android, the task names are different - use pattern matching
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    if (name.startsWith("compileKotlin")) {
        dependsOn("generateVersionFile")
    }
    if (name.startsWith("compile") && name.endsWith("Main")) {
        dependsOn("generateGitHubClientIdConfig")
    }
}

// The Kotlin/Wasm Gradle plugin generates package.json with a malformed
// "packageManager": "yarn@pnpm@12.11.2" field. Yarn 1.x (pre-installed on
// GitHub Actions ubuntu-latest runners) doesn't understand the packageManager
// field at all and fails with "However the current global version of Yarn is 1.22.22".
// Strip the field before npm install so it can proceed with system yarn.
tasks.matching { it.name == "kotlinWasmNpmInstall" }.configureEach {
    doFirst {
        val buildDir = layout.buildDirectory.get().asFile
        val pkgFile = File(buildDir, "wasmJsNpm/package.json")
        if (pkgFile.exists()) {
            val lines = pkgFile.readText().lines()
            val filtered = lines.filter { !it.trimStart().startsWith("\"packageManager\"") }
            val modified = filtered.joinToString("\n") + "\n"
            if (modified != pkgFile.readText()) {
                pkgFile.writeText(modified)
                logger.lifecycle("Stripped malformed packageManager from $pkgFile")
            }
        }
    }
}
