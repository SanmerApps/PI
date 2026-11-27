plugins {
    alias(libs.plugins.self.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.rikka.refine)
}

android {
    namespace = "dev.sanmer.pi.core"

    defaultConfig {
        consumerProguardFile("proguard-rules.keep")
    }
}

dependencies {
    compileOnly(project(":stub"))
    api(project(":zip"))
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.rikka.refine.runtime)
}