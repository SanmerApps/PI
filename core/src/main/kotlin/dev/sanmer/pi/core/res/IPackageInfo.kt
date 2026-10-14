package dev.sanmer.pi.core.res

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface IPackageInfo : Parcelable {
    @Parcelize
    data class Apk(
        val packageInfo: PackageInfoLite,
        val sizeBytes: Long,
        val currentPackageInfo: PackageInfoLite = packageInfo,
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