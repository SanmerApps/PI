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
        val javaVersion = JavaVersion.toVersion(
            libs.findVersion("javaVersion").get().requiredVersion.toInt()
        )
        val kotlinVersion = KotlinVersion.fromVersion(
            libs.findVersion("kotlin").get().requiredVersion.substringBeforeLast('.')
        )

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
            buildToolsVersion = libs.findVersion("buildToolsVersion").get().requiredVersion

            defaultConfig {
                targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
            }

            buildFeatures {
                buildConfig = true
            }

            compileOptions {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
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
