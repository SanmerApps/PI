plugins {
    alias(libs.plugins.self.library)
    alias(libs.plugins.rikka.refine)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "dev.sanmer.pi.core"

    defaultConfig {
        consumerProguardFile("proguard-rules.pro")
    }
}

dependencies {
    compileOnly(project(":stub"))
    implementation(libs.rikka.refine.runtime)

    implementation(libs.androidx.annotation)
    implementation(libs.apache.commons.compress)
    implementation(libs.appiconloader.iconloaderlib)
    implementation(libs.kotlinx.coroutines.android)
}