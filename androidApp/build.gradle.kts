import org.gradle.api.GradleException
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun signingCredential(
    env: String,
    prop: String,
): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(prop)?.takeIf { it.isNotBlank() }

val releaseKeystorePath = signingCredential("KEYSTORE_FILE", "storeFile")
val releaseKeystorePassword = signingCredential("KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingCredential("KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingCredential("KEY_PASSWORD", "keyPassword") ?: releaseKeystorePassword
val hasReleaseSigningCredentials =
    listOf(releaseKeystorePath, releaseKeystorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

android {
    namespace = "com.dorai.ai"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.dorai.ai"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode =
            libs.versions.android.versionCode
                .get()
                .toInt()
        versionName = libs.versions.appVersion.get()
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("playStore") {
            dimension = "distribution"
        }
        create("foss") {
            dimension = "distribution"
            isDefault = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningCredentials) {
                val keystoreFile = file(releaseKeystorePath!!)
                storeFile = if (keystoreFile.isAbsolute) keystoreFile else rootProject.file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "../composeApp/proguard-rules.pro")
            if (hasReleaseSigningCredentials) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Only enforce for CI/release workflows or when explicitly requested
                val enforceSigning = System.getenv("ENFORCE_RELEASE_SIGNING") == "true"
                if (enforceSigning) {
                    throw GradleException(
                        "Release builds require signing credentials via KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS " +
                            "(and optional KEY_PASSWORD) environment variables or keystore.properties " +
                            "(storeFile/storePassword/keyAlias/keyPassword).",
                    )
                }
                // CI release workflows must explicitly set ENFORCE_RELEASE_SIGNING=true.
                // Non-release CI tasks (formatting, lint, and unit tests) must not be blocked
                // merely because production signing secrets are unavailable.
                signingConfig = signingConfigs.getByName("debug")
                // Make the fallback loud: a debug-signed "release" artifact must never be
                // mistaken for a production build. CI always sets ENFORCE_RELEASE_SIGNING=true,
                // so any publishable release comes from a properly signed workflow run.
                logger.warn(
                    "WARNING: Android release build is falling back to DEBUG signing " +
                        "(no release signing credentials found in env vars or keystore.properties and " +
                        "ENFORCE_RELEASE_SIGNING!=true). The produced artifact is NOT production-ready " +
                        "and must not be distributed.",
                )
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.foundation.android)
    implementation(libs.compose.material3)
    implementation(libs.koin.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.filekit.core)
    implementation(libs.filekit.compose)
    implementation(libs.tts)
    implementation(libs.tts.compose)
    implementation(libs.compose.components.uiToolingPreview)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation("junit:junit:4.13.2")
    "playStoreImplementation"(libs.play.review)
    "playStoreImplementation"(libs.mlkit.genai.prompt)
}
