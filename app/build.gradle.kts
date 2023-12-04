import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.pro.application)
    alias(libs.plugins.pro.compose)
    alias(libs.plugins.pro.hilt)
    alias(libs.plugins.pro.room)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.rikka.refine)
}

val baseVersionName = "0.1.1"
val isDevVersion get() = exec("git tag --contains HEAD").isEmpty()
val verNameSuffix: String get() = if (isDevVersion) ".dev" else ""

android {
    namespace = "dev.sanmer.pi"

    defaultConfig {
        applicationId = namespace
        versionName = "${baseVersionName}${verNameSuffix}.${commitId}"
        versionCode = commitCount

        resourceConfigurations += arrayOf("en", "zh-rCN")
    }

    val releaseSigning = if (project.hasReleaseKeyStore) {
        signingConfigs.create("release") {
            storeFile = project.releaseKeyStore
            storePassword = project.releaseKeyStorePassword
            keyAlias = project.releaseKeyAlias
            keyPassword = project.releaseKeyPassword
            enableV2Signing = true
            enableV3Signing = true
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
            buildConfigField("Boolean", "IS_DEV_VERSION", isDevVersion.toString())
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    packaging.resources.excludes += setOf(
        "META-INF/**",
        "okhttp3/**",
        "kotlin/**",
        "org/**",
        "**.properties",
        "**.bin",
        "**/*.proto"
    )

    applicationVariants.configureEach {
        outputs.configureEach {
            (this as? ApkVariantOutputImpl)?.outputFileName =
                "PI-${versionName}-${versionCode}-${name}.apk"
        }
    }
}

dependencies {
    compileOnly(projects.hiddenApi)
    implementation(projects.compat)

    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewModel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.hiddenApiBypass)
    implementation(libs.rikka.refine.runtime)

    implementation(libs.appiconloader)
    implementation(libs.appiconloader.coil)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)

}