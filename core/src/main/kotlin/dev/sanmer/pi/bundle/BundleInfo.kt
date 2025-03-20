package dev.sanmer.pi.bundle

import android.content.pm.PackageInfo
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class BundleInfo(
    val baseFile: File,
    val baseInfo: PackageInfo,
    val splitConfigs: List<SplitConfig>
) : Parcelable