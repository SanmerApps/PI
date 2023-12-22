plugins {
    alias(libs.plugins.pro.library)
    alias(libs.plugins.rikka.refine)
}

android {
    namespace = "dev.sanmer.hidden.compat"

    buildFeatures {
        aidl = true
    }
}

dependencies {
    compileOnly(projects.hiddenApi)

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.rikka.refine.runtime)
}