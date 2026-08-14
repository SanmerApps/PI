package dev.sanmer.pi.ktx

import dev.sanmer.pi.core.parser.PackageInfoLite

fun PackageInfoLite.versionDisplay() = "$versionName (${longVersionCode})"

infix fun PackageInfoLite?.versionDiff(other: PackageInfoLite) = when {
    this == null -> other.versionDisplay()
    longVersionCode != other.longVersionCode -> "${versionDisplay()} → ${other.versionDisplay()}"
    else -> other.versionDisplay()
}

private inline fun <V> PackageInfoLite.compare(
    other: PackageInfoLite,
    value: (PackageInfoLite) -> V
) = when {
    value(this) != value(other) -> "${value(this)} → ${value(other)}"
    else -> value(other).toString()
}

infix fun PackageInfoLite?.sdkVersionDiff(other: PackageInfoLite) = when {
    this == null -> buildString {
        append("Target: ${other.targetSdkVersion} ")
        append("Min: ${other.minSdkVersion} ")
        append("Compile: ${other.compileSdkVersion}")
    }

    else -> buildString {
        append("Target: ${compare(other) { targetSdkVersion }} ")
        append("Min: ${compare(other) { minSdkVersion }} ")
        append("Compile: ${compare(other) { compileSdkVersion }}")
    }
}