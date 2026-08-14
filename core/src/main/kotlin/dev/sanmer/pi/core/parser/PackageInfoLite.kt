package dev.sanmer.pi.core.parser

import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Parcelable
import dev.sanmer.pi.core.compat.PackageInfoCompat
import dev.sanmer.pi.core.compat.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.compileSdkVersionCodename
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadLabel
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadUnbadgedIcon
import dev.sanmer.pi.core.compat.PackageInfoCompat.minSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.targetSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.versionCodeMajor
import dev.sanmer.pi.core.parser.ResourceParser.toIcon
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class PackageInfoLite(
    val packageName: String,
    val versionCode: Int,
    val versionCodeMajor: Int,
    val versionName: String,
    val compileSdkVersion: Int,
    val compileSdkVersionCodename: String,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val label: String?,
    val icon: Bitmap?
) : Parcelable {
    @IgnoredOnParcel
    val longVersionCode by lazy {
        PackageInfoCompat.composeLongVersionCode(
            versionCodeMajor, versionCode
        )
    }

    @IgnoredOnParcel
    val labelOrDefault by lazy {
        label ?: packageName
    }

    @IgnoredOnParcel
    val iconOrDefault by lazy {
        icon ?: Resources.getSystem()
            .getDrawable(android.R.drawable.sym_def_app_icon, null)
            .toIcon()
    }

    companion object Default {
        fun from(
            context: Context,
            packageInfo: PackageInfo
        ) = PackageInfoLite(
            packageName = packageInfo.packageName,
            versionCode = @Suppress("DEPRECATION") packageInfo.versionCode,
            versionCodeMajor = packageInfo.versionCodeMajor,
            versionName = packageInfo.versionName.orEmpty(),
            compileSdkVersion = packageInfo.compileSdkVersion,
            compileSdkVersionCodename = packageInfo.compileSdkVersionCodename.orEmpty(),
            minSdkVersion = packageInfo.minSdkVersion,
            targetSdkVersion = packageInfo.targetSdkVersion,
            label = packageInfo.loadLabel(context),
            icon = packageInfo.loadUnbadgedIcon(context)?.toIcon()
        )
    }
}