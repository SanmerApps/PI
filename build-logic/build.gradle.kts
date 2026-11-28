plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.gradle)
}

gradlePlugin {
    plugins {
        register("self.application") {
            id = "self.application"
            implementationClass = "ApplicationPlugin"
        }
        
        register("self.library") {
            id = "self.library"
            implementationClass = "LibraryPlugin"
        }
    }
}