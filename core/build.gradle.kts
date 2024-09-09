plugins {
    alias(libs.plugins.self.library)
    alias(libs.plugins.rikka.refine)
    alias(libs.plugins.kotlin.parcelize)
    `maven-publish`
}

android {
    namespace = "dev.sanmer.pi.core"

    defaultConfig {
        consumerProguardFile("proguard-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("core") {
            artifactId = "core"

            afterEvaluate {
                from(components.getByName("release"))
            }
        }
    }
}

dependencies {
    api(libs.sanmer.su)

    compileOnly(projects.stub)
    implementation(libs.rikka.refine.runtime)

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)
}