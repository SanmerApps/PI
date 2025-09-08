package dev.sanmer.pi.parser

import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Parcelable
import dev.sanmer.pi.PackageInfoCompat
import dev.sanmer.pi.parser.ResourceParser.toIcon
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
}