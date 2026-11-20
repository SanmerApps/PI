import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class LibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "com.android.library")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val compileSdk = libs.findVersion("compileSdk").get().requiredVersion
        val minSdk = libs.findVersion("minSdk").get().requiredVersion
        val buildToolsVersion = libs.findVersion("buildToolsVersion").get().requiredVersion
        val kotlinVersion = KotlinVersion.fromVersion(
            libs.findVersion("kotlin").get().requiredVersion.versionMajor
        )

        extensions.configure<LibraryExtension> {
            this.compileSdk = compileSdk.versionMajor.toInt()
            this.compileSdkMinor = compileSdk.versionMinor.toIntOrNull()
            this.buildToolsVersion = buildToolsVersion

            defaultConfig {
                this.minSdk = minSdk.toInt()
            }

            compileOptions {
                sourceCompatibility = JavaVersion.current()
                targetCompatibility = JavaVersion.current()
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                languageVersion.set(kotlinVersion)
            }
        }
    }
}
