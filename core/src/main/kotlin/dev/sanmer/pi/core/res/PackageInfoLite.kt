package dev.sanmer.pi.core.res

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PackageInfoLite(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val compileSdkVersion: Int,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val label: String?,
    val icon: Bitmap?
) : Parcelable