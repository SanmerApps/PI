import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = 36
            buildToolsVersion = "36.1.0"

            defaultConfig {
                minSdk = 30
                targetSdk = compileSdk
            }

            buildFeatures {
                buildConfig = true
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                languageVersion.set(KotlinVersion.KOTLIN_2_3)
                optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
    }
}
