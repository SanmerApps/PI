import com.android.build.api.variant.BuildConfigField
import java.time.Instant

plugins {
    alias(libs.plugins.self.application)
    alias(libs.plugins.self.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val baseVersionName = "1.3.1"
val gitCommitTag = gitCommitTag()
val gitCommitSha = gitCommitSha()
val gitCommitNum = gitCommitNum()
val devSuffix = if (gitCommitTag.isEmpty()) ".dev" else ""

android {
    namespace = "dev.sanmer.pi"

    defaultConfig {
        applicationId = namespace
        versionName = "${baseVersionName}.${gitCommitSha}${devSuffix}"
        versionCode = gitCommitNum
        ndk.abiFilters += listOf("arm64-v8a", "x86_64")
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "zh-rCN")
    }

    val releaseSigning = if (hasReleaseKeyStore()) {
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
            optimization {
                enable = true
            }
        }

        all {
            signingConfig = releaseSigning
        }
    }

    packaging {
        jniLibs.excludes += setOf(
            "**/libdatastore_shared_counter.so"
        )
        resources.excludes += setOf(
            "META-INF/**",
            "kotlin/**",
            "org/**",
            "**.bin",
            "**.properties"
        )
    }

    dependenciesInfo.includeInApk = false
}

androidComponents.onVariants { variant ->
    variant.buildConfigFields?.apply {
        put("GIT_SHA", BuildConfigField("String", "\"$gitCommitSha\"", null))
        put("BUILD_TIME", BuildConfigField("long", Instant.now().toEpochMilli().toString(), null))
    }

    variant.outputs.forEach { output ->
        output.outputFileName =
            output.versionName.zip(output.versionCode) { versionName, versionCode ->
                "PI-$versionName-$versionCode-${variant.buildType}.apk"
            }
    }
}

dependencies {
    compileOnly(project(":stub"))
    implementation(project(":core"))
    implementation(project(":su"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hiddenApiBypass)
    implementation(libs.xz)
}
