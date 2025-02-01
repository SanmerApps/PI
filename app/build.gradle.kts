import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.time.Instant

plugins {
    alias(libs.plugins.self.application)
    alias(libs.plugins.self.compose)
    alias(libs.plugins.self.hilt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val baseVersionName: String by extra
val devVersion = exec("git tag --contains HEAD").isEmpty()
val commitShaSuffix = commitSha.let { ".${it.substring(0, 7)}" }
val devSuffix = if (devVersion) ".dev" else ""

android {
    namespace = "dev.sanmer.pi"

    defaultConfig {
        applicationId = namespace
        versionName = "${baseVersionName}${commitShaSuffix}${devSuffix}"
        versionCode = commitCount

        ndk.abiFilters += listOf("arm64-v8a", "x86_64")
    }

    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters += listOf(
            "en",
            "ar",
            "es",
            "fr",
            "in",
            "iw",
            "pt",
            "pt-rBR",
            "ro",
            "ru",
            "su",
            "tr",
            "vi",
            "zh-rCN"
        )
    }

    val releaseSigning = if (project.hasReleaseKeyStore) {
        signingConfigs.create("release") {
            storeFile = project.releaseKeyStore
            storePassword = project.releaseKeyStorePassword
            keyAlias = project.releaseKeyAlias
            keyPassword = project.releaseKeyPassword
            enableV3Signing = true
            enableV4Signing = true
        }
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        all {
            signingConfig = releaseSigning
            buildConfigField("boolean", "DEV_VERSION", devVersion.toString())
            buildConfigField("long", "BUILD_TIME", Instant.now().toEpochMilli().toString())
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging.resources.excludes += setOf(
        "META-INF/**",
        "okhttp3/**",
        "kotlin/**",
        "**.bin",
        "**.properties"
    )

    dependenciesInfo.includeInApk = false

    applicationVariants.configureEach {
        outputs.configureEach {
            if (this is ApkVariantOutputImpl) {
                outputFileName = "PI-${versionName}-${versionCode}-${name}.apk"
            }
        }
    }
}

dependencies {
    compileOnly(projects.stub)
    implementation(projects.core)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.appiconloader)
    implementation(libs.appiconloader.coil)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.timber)
}
