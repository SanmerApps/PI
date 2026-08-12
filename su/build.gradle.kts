plugins {
    alias(libs.plugins.self.library)
}

android {
    namespace = "dev.sanmer.su"

    buildFeatures {
        aidl = true
    }
}

dependencies {
    compileOnly(project(":stub"))

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
}