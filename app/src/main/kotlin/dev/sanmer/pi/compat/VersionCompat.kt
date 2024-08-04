package dev.sanmer.pi.compat

import android.content.pm.PackageInfo
import dev.sanmer.pi.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.PackageInfoCompat.isEmpty

object VersionCompat {
    private const val ARROW = "→"
    private const val ARROW_REVERT = "←"

    private fun <T : Comparable<T>> StringBuilder.comparator(
        v0: Pair<T, String>,
        v1: Pair<T, String>
    ) {
        val (k0, s0) = v0
        val (k1, s1) = v1

        if (s0.isNotEmpty()) append(s0) else append(k0)
        when {
            k0 == k1 -> return
            k0 < k1 -> append(" $ARROW ")
            k0 > k1 -> append(" $ARROW_REVERT ")
        }
        if (s1.isNotEmpty()) append(s1) else append(k1)
    }

    private fun <T : Comparable<T>> StringBuilder.comparator(
        v0: T,
        v1: T
    ) = comparator(
        v0 = v0 to "",
        v1 = v1 to ""
    )

    val PackageInfo.versionStr
        inline get() = "$versionName (${longVersionCode})"

    val PackageInfo.hasCompileSdkVersion
        inline get() = compileSdkVersion != 0

    val PackageInfo.sdkVersion
        inline get() = buildString {
            if (isEmpty) return@buildString
            val appInfo = requireNotNull(applicationInfo)

            append("Target: ")
            append(appInfo.targetSdkVersion)
            append(", ")
            append("Min: ")
            append(appInfo.minSdkVersion)
            if (hasCompileSdkVersion) {
                append(", ")
                append("Compile: ")
                append(compileSdkVersion)
            }
        }

    fun PackageInfo.versionDiff(other: PackageInfo): String {
        if (isEmpty) return other.versionStr
        if (other.isEmpty) return versionStr
        return buildString {
            comparator(
                v0 = longVersionCode to versionStr,
                v1 = other.longVersionCode to other.versionStr,
            )
        }
    }

    fun PackageInfo.sdkVersionDiff(other: PackageInfo): String {
        if (isEmpty) return other.sdkVersion
        if (other.isEmpty) return sdkVersion
        return buildString {
            val appInfo = requireNotNull(applicationInfo)
            val otherAppInfo = requireNotNull(other.applicationInfo)

            append("Target: ")
            comparator(appInfo.targetSdkVersion, otherAppInfo.targetSdkVersion)
            append(", ")
            append("Min: ")
            comparator(appInfo.minSdkVersion, otherAppInfo.minSdkVersion)
            if (other.hasCompileSdkVersion) {
                append(", ")
                append("Compile: ")
                comparator(compileSdkVersion, other.compileSdkVersion)
            }
        }
    }
}