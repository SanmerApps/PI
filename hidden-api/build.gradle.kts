plugins {
    alias(libs.plugins.pro.library)
}

android {
    namespace = "dev.sanmer.pi.hidden_api"
}

dependencies {
    annotationProcessor(libs.rikka.refine.compiler)
    compileOnly(libs.rikka.refine.annotation)
    compileOnly(libs.androidx.annotation)
}