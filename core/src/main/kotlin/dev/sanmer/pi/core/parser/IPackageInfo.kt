package dev.sanmer.pi.core.parser

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface IPackageInfo : Parcelable {
    @Parcelize
    data class Apk(
        val currentPackageInfo: PackageInfoLite? = null,
        val packageInfo: PackageInfoLite,
        val sizeBytes: Long
    ) : IPackageInfo

    @Parcelize
    data class Apks(
        val base: Apk,
        val splitConfigs: List<SplitConfig>
    ) : IPackageInfo

    @Parcelize
    @JvmInline
    value class Zip(
        val packageInfos: Map<String, Apk>
    ) : IPackageInfo
}