plugins {
    alias(libs.plugins.self.library)
}

android {
    namespace = "dev.sanmer.zip"

    defaultConfig {
        ndk.abiFilters += listOf("arm64-v8a", "x86_64")

        consumerProguardFile("proguard-rules.keep")
    }

    sourceSets.all {
        jniLibs.srcDir("src/main/rust/libs")
    }
}

dependencies {
    api(libs.kotlinx.io.core)
}