package dev.sanmer.pi.model

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable

@Immutable
data class IPackageInfo(
    val inner: PackageInfo,
    val packageName: String,
    val authorized: Boolean,
    val label: String,
    val lastUpdateTime: Long,
    val enable: Boolean
) {
    constructor(
        packageInfo: PackageInfo,
        authorized: Boolean = false,
        pm: PackageManager? = null
    ) : this(
        inner = packageInfo,
        packageName = packageInfo.packageName,
        authorized = authorized,
        label = pm?.let {
            packageInfo.applicationInfo.loadLabel(it).toString()
        } ?: packageInfo.packageName,
        lastUpdateTime = packageInfo.lastUpdateTime,
        enable = packageInfo.applicationInfo.enabled,
    )

    companion object {
        fun PackageInfo.toIPackageInfo(
            authorized: Boolean = false,
            pm: PackageManager? = null
        ) = IPackageInfo(
            packageInfo = this,
            authorized = authorized,
            pm = pm
        )

        fun empty() = IPackageInfo(
            inner = PackageInfo(),
            packageName = "",
            authorized = false,
            label = "",
            lastUpdateTime = 0L,
            enable = false
        )
    }
}