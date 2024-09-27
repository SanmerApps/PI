package dev.sanmer.pi.compat

import android.content.Context
import android.content.pm.PackageInfo
import dev.sanmer.pi.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.PackageInfoCompat.isEmpty
import dev.sanmer.pi.R

object VersionCompat {
    private fun <T : Comparable<T>> Pair<T, String>.comparator(
        ctx: Context,
        other: Pair<T, String>
    ): String {
        val (value0, text0) = this
        val (value1, text1) = other
        return when {
            value0 == value1 -> text0.ifEmpty { value0.toString() }
            else -> ctx.getString(
                R.string.comparator,
                text0.ifEmpty { value0.toString() },
                text1.ifEmpty { value1.toString() }
            )
        }
    }

    private fun <T : Comparable<T>> T.comparator(
        ctx: Context,
        other: T,
    ) = (this to "").comparator(
        ctx = ctx,
        other = other to ""
    )

    val PackageInfo.versionStr
        inline get() = "$versionName (${longVersionCode})"

    private val PackageInfo.compileSdkDisplay
        inline get() = if (compileSdkVersion != 0) compileSdkVersion.toString() else "?"

    fun PackageInfo.getSdkVersion(context: Context): String {
        if (isEmpty) return ""
        val appInfo = requireNotNull(applicationInfo)
        return context.getString(
            R.string.sdk_versions,
            appInfo.targetSdkVersion.toString(),
            appInfo.minSdkVersion.toString(),
            compileSdkDisplay
        )
    }

    fun PackageInfo.getVersionDiff(context: Context, other: PackageInfo): String {
        if (isEmpty) return other.versionStr
        if (other.isEmpty) return versionStr
        return (longVersionCode to versionStr).comparator(
            ctx = context,
            other = other.longVersionCode to other.versionStr,
        )
    }

    fun PackageInfo.getSdkVersionDiff(context: Context, other: PackageInfo): String {
        if (isEmpty) return other.getSdkVersion(context)
        if (other.isEmpty) return getSdkVersion(context)
        val appInfo0 = requireNotNull(applicationInfo)
        val appInfo1 = requireNotNull(other.applicationInfo)
        return context.getString(
            R.string.sdk_versions,
            appInfo0.targetSdkVersion.comparator(
                ctx = context,
                other = appInfo1.targetSdkVersion
            ),
            appInfo0.minSdkVersion.comparator(
                ctx = context,
                other = appInfo1.minSdkVersion
            ),
            (compileSdkVersion to compileSdkDisplay).comparator(
                ctx = context,
                other = other.compileSdkVersion to other.compileSdkDisplay
            )
        )
    }
}