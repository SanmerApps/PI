package dev.sanmer.pi.app

import kotlin.random.Random

object Const {
    val TEMP_PACKAGE get() = "temp_package_${Random.nextInt(1000, 1900)}.apk"

    object MIME {
        const val APK = "application/vnd.android.package-archive"
    }
}