package dev.sanmer.pi.bundle

import android.content.pm.PackageInfo
import java.io.File

data class BundleInfo(
    val baseFile: File,
    val baseInfo: PackageInfo,
    val splitConfigs: List<SplitConfig>
)