package dev.sanmer.pi.parser

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BundleInfo(
    val packageInfo: PackageInfoLite,
    val fileName: String,
    val sizeBytes: Long,
    val splitConfigs: List<SplitConfig>
) : Parcelable {
    val isZip inline get() = fileName.isNotEmpty()
}