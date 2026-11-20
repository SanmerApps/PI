import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "com.android.application")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val compileSdk = libs.findVersion("compileSdk").get().requiredVersion
        val targetSdk = libs.findVersion("targetSdk").get().requiredVersion
        val minSdk = libs.findVersion("minSdk").get().requiredVersion
        val buildToolsVersion = libs.findVersion("buildToolsVersion").get().requiredVersion
        val kotlinVersion = KotlinVersion.fromVersion(
            libs.findVersion("kotlin").get().requiredVersion.versionMajor
        )

        extensions.configure<ApplicationExtension> {
            this.compileSdk = compileSdk.versionMajor.toInt()
            this.compileSdkMinor = compileSdk.versionMinor.toIntOrNull()
            this.buildToolsVersion = buildToolsVersion

            defaultConfig {
                this.targetSdk = targetSdk.toInt()
                this.minSdk = minSdk.toInt()
            }

            buildFeatures {
                buildConfig = true
            }

            compileOptions {
                sourceCompatibility = JavaVersion.current()
                targetCompatibility = JavaVersion.current()
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                languageVersion.set(kotlinVersion)
                optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
    }
}
