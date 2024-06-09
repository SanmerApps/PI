plugins {
    alias(libs.plugins.self.library)
    alias(libs.plugins.rikka.refine)
}

android {
    namespace = "dev.sanmer.pi.core"

    defaultConfig {
        consumerProguardFile("proguard-rules.pro")
    }
}

dependencies {
    api(libs.sanmer.su)

    compileOnly(projects.stub)
    implementation(libs.hiddenApiBypass)
    implementation(libs.rikka.refine.runtime)

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)
}