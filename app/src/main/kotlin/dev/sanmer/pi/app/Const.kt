package dev.sanmer.pi.app

import kotlin.random.Random

object Const {
    // TMP
    val TEMP_PACKAGE get() = "temp_package_${Random.nextInt(1000, 1900)}.apk"

    // URL
    const val GITHUB_URL = "https://github.com/SanmerApps/PI"
    const val TRANSLATE_URL = "https://weblate.sanmer.app/engage/pi"
}