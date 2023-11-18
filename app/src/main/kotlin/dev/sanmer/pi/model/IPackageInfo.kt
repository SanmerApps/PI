package dev.sanmer.pi.model

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.Stable

@Stable
data class IPackageInfo(
    val inner: PackageInfo,
    val packageName: String,
    val lastUpdateTime: Long,
    val enable: Boolean,
    val authorized: Boolean
) {
    constructor(packageInfo: PackageInfo, authorized: Boolean) : this(
        inner = packageInfo,
        packageName = packageInfo.packageName,
        lastUpdateTime = packageInfo.lastUpdateTime,
        enable = packageInfo.applicationInfo.enabled,
        authorized = authorized
    )

    fun loadLabel(pm: PackageManager) =
        inner.applicationInfo.loadLabel(pm).toString()
}