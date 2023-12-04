plugins {
    alias(libs.plugins.pro.library)
    alias(libs.plugins.rikka.refine)
}

val applicationId = "dev.sanmer.pi"
val versionCode = commitCount

android {
    namespace = "dev.sanmer.hidden.compat"

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    buildTypes {
        all {
            buildConfigField("String", "APPLICATION_ID", "\"${applicationId}\"")
            buildConfigField("int", "VERSION_CODE", commitCount.toString())
        }
    }
}

dependencies {
    compileOnly(projects.hiddenApi)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.collection.jvm)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.libsu.core)
    implementation(libs.libsu.io)
    implementation(libs.libsu.service)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.rikka.refine.runtime)
}