plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

task<Delete>("clean") {
    delete(layout.buildDirectory)
}

subprojects {
    extra["baseVersionName"] = "1.1.1"

    apply(plugin = "maven-publish")
    configure<PublishingExtension> {
        publications {
            val baseVersionName: String by extra
            all {
                group = "dev.sanmer.pi"
                version = baseVersionName
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/SanmerApps/PI")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}