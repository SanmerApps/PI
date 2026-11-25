package dev.sanmer.pi.core.res

import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.ApkAssets
import android.graphics.Bitmap
import android.os.Parcelable
import dev.sanmer.pi.core.compat.AssetManagerCompat
import dev.sanmer.pi.core.compat.AssetManagerCompat.resources
import dev.sanmer.pi.core.compat.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadLabel
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadUnbadgedIcon
import dev.sanmer.pi.core.compat.PackageInfoCompat.minSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.targetSdkVersion
import dev.sanmer.pi.core.res.ResourceParser.ANDROID_MANIFEST_XML
import dev.sanmer.pi.core.res.ResourceParser.getPackageInfoLite
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
) : Parcelable {
    class Builder(
        private val factory: ResourceParser.BitmapFactory
    ) {
        companion object Default {
            fun fromPackageInfo(packageInfo: PackageInfo) = PackageInfoLite(
                packageName = packageInfo.packageName,
                versionCode = packageInfo.longVersionCode,
                versionName = packageInfo.versionName.orEmpty(),
                compileSdkVersion = packageInfo.compileSdkVersion,
                minSdkVersion = packageInfo.minSdkVersion,
                targetSdkVersion = packageInfo.targetSdkVersion,
                label = null,
                icon = null
            )
        }

        fun fromPackageInfo(context: Context, packageInfo: PackageInfo) = PackageInfoLite(
            packageName = packageInfo.packageName,
            versionCode = packageInfo.longVersionCode,
            versionName = packageInfo.versionName.orEmpty(),
            compileSdkVersion = packageInfo.compileSdkVersion,
            minSdkVersion = packageInfo.minSdkVersion,
            targetSdkVersion = packageInfo.targetSdkVersion,
            label = packageInfo.loadLabel(context),
            icon = packageInfo.loadUnbadgedIcon(context)?.let { factory.toBitmap(it) }
        )

        fun fromApkAssets(apkAssets: ApkAssets) =
            AssetManagerCompat.new(arrayOf(apkAssets), false).use { assetManager ->
                assetManager.openXmlResourceParser(ANDROID_MANIFEST_XML).use { parser ->
                    parser.getPackageInfoLite(assetManager.resources, factory)
                }
            }
    }
}