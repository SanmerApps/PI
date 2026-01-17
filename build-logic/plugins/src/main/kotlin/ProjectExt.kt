import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.util.Properties

fun Project.gitCommitTag() = exec("git", "tag", "--points-at", "HEAD")
fun Project.gitCommitSha() = exec("git", "rev-parse", "--short", "HEAD")
fun Project.gitCommitNum() = exec("git", "rev-list", "--count", "HEAD").toInt()
private fun Project.exec(vararg command: String) = providers.exec {
    commandLine(command.toList())
}.standardOutput.asText.get().trim()

val Project.releaseKeyStore: File get() = File(extra["key.store"].toString())
val Project.releaseKeyStorePassword: String get() = extra["key.store.password"].toString()
val Project.releaseKeyAlias: String get() = extra["key.alias"].toString()
val Project.releaseKeyPassword: String get() = extra["key.password"].toString()
fun Project.hasReleaseKeyStore(): Boolean {
    signingProperties(rootDir).forEach { (key, value) ->
        extra[key.toString()] = value
    }
    return extra.has("key.store")
}

private fun signingProperties(rootDir: File): Properties {
    val properties = Properties()
    val signingProperties = File(rootDir, "signing.properties")
    if (signingProperties.isFile) {
        signingProperties.inputStream().use(properties::load)
    }
    return properties
}