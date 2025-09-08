import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.time.Instant

plugins {
    alias(libs.plugins.self.application)
    alias(libs.plugins.self.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val baseVersionName: String by extra
val devVersion = exec("git tag --contains HEAD").isEmpty()
val shaSuffix = gitCommitSha.let { ".${it.substring(0, 7)}" }
val devSuffix = if (devVersion) ".dev" else ""

android {
    namespace = "dev.sanmer.pi"

    defaultConfig {
        applicationId = namespace
        versionName = "${baseVersionName}${shaSuffix}${devSuffix}"
        versionCode = gitCommitCount

        ndk.abiFilters += listOf("arm64-v8a", "x86_64")
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf(
            "en",
            "ar",
            "es",
            "fa",
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

    val releaseSigning = if (hasReleaseKeyStore) {
        signingConfigs.create("release") {
            storeFile = releaseKeyStore
            storePassword = releaseKeyStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
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

    flavorDimensions += "feature"
    productFlavors {
        create("full") {
            dimension = "feature"
        }

        create("lite") {
            dimension = "feature"
            applicationIdSuffix = ".lite"
        }
    }

    packaging.resources.excludes += setOf(
        "META-INF/**",
        "kotlin/**",
        "org/**",
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
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hiddenApiBypass)

    "fullImplementation"(libs.androidx.datastore.core)
    "fullImplementation"(libs.appiconloader.coil)
    "fullImplementation"(libs.coil.kt)
    "fullImplementation"(libs.coil.kt.compose)
    "fullImplementation"(libs.kotlinx.serialization.protobuf)
    "fullImplementation"(libs.sanmer.su)

    "liteImplementation"(libs.rikka.shizuku.api)
    "liteImplementation"(libs.rikka.shizuku.provider)
}
