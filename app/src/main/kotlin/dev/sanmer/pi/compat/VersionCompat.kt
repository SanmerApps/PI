package dev.sanmer.pi.compat

import android.content.pm.PackageInfo
import dev.sanmer.hidden.compat.PackageInfoCompat.compileSdkVersion
import dev.sanmer.hidden.compat.PackageInfoCompat.isEmpty

object VersionCompat {
    private const val ARROW = "→"
    private const val ARROW_REVERT = "←"

    private fun <T: Comparable<T>> StringBuilder.comparator(
        v0: Pair<T, String>,
        v1: Pair<T, String>
    ) {
        val (k0, s0) = v0
        val (k1, s1) = v1

        if (s0.isNotEmpty()) {
            append(s0)
        } else {
            append(k0.toString())
        }

        when {
            k0 == k1 -> {
                return
            }
            k0 < k1 -> {
                append(" $ARROW ")
            }
            k0 > k1 -> {
                append(" $ARROW_REVERT ")
            }
        }

        if (s1.isNotEmpty()) {
            append(s1)
        } else {
            append(k1.toString())
        }
    }

    private fun <T: Comparable<T>> StringBuilder.comparator(
        v0: T,
        v1: T
    ) = comparator(
        v0 = v0 to "",
        v1 = v1 to ""
    )

    private val PackageInfo.versionStr get() =
        "$versionName (${longVersionCode})"

    fun getVersionDiff(
        old: PackageInfo,
        new: PackageInfo
    ) = buildString {
        when {
            new.isEmpty -> {}
            old.isEmpty -> {
                append(new.versionStr)
            }
            else -> {
                comparator(
                    v0 = with(old) { longVersionCode to versionStr },
                    v1 = with(new) { longVersionCode to versionStr },
                )
            }
        }
    }

    fun getSdkVersionDiff(
        old: PackageInfo,
        new: PackageInfo
    ) = buildString {
        val oldAppInfo = old.applicationInfo
        val newAppInfo = new.applicationInfo

        when {
            new.isEmpty -> {}
            old.isEmpty -> {
                append("Target: ")
                append("${newAppInfo.targetSdkVersion}")
                append(", ")
                append("Min: ")
                append("${newAppInfo.minSdkVersion}")

                if (new.compileSdkVersion != 0) {
                    append(", ")
                    append("Compile: ")
                    append("${new.compileSdkVersion}")
                }
            }
            else -> {
                append("Target: ")
                comparator(
                    v0 = oldAppInfo.targetSdkVersion,
                    v1 = newAppInfo.targetSdkVersion
                )
                append(", ")
                append("Min: ")
                comparator(
                    v0 = oldAppInfo.minSdkVersion,
                    v1 = newAppInfo.minSdkVersion
                )

                if (new.compileSdkVersion != 0) {
                    append(", ")
                    append("Compile: ")
                    comparator(
                        v0 = old.compileSdkVersion,
                        v1 = new.compileSdkVersion
                    )
                }
            }
        }
    }

    fun getVersion(pi: PackageInfo) = getVersionDiff(
        old = PackageInfo(),
        new = pi
    )

    fun getSdkVersion(pi: PackageInfo) = getSdkVersionDiff(
        old = PackageInfo(),
        new = pi
    )
}