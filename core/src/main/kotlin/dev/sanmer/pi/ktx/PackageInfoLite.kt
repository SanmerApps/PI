package dev.sanmer.pi.ktx

import dev.sanmer.pi.parser.PackageInfoLite

fun PackageInfoLite?.orEmpty() = PackageInfoLite(
    packageName = "",
    versionCode = 0,
    versionCodeMajor = 0,
    versionName = "",
    compileSdkVersion = 0,
    compileSdkVersionCodename = "",
    minSdkVersion = 0,
    targetSdkVersion = 0,
    label = null,
    icon = null
)