plugins {
    alias(libs.plugins.self.library)
    `maven-publish`
}

android {
    namespace = "dev.sanmer.pi.stub"

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("stub") {
            artifactId = "stub"

            afterEvaluate {
                from(components.getByName("release"))
            }
        }
    }
}

dependencies {
    annotationProcessor(libs.rikka.refine.compiler)
    compileOnly(libs.rikka.refine.annotation)
    compileOnly(libs.androidx.annotation)
}