package dev.sanmer.pi.model

import android.content.pm.PackageInfo
import androidx.compose.runtime.Immutable
import dev.sanmer.hidden.compat.delegate.PackageInfoDelegate

@Immutable
data class IPackageInfo(
    private val inner: PackageInfo,
    val authorized: Boolean,
) : PackageInfoDelegate(inner) {
    companion object {
        fun PackageInfo.toIPackageInfo(
            authorized: Boolean = false
        ) = IPackageInfo(
            inner = this,
            authorized = authorized
        )

        fun empty() = IPackageInfo(
            inner = PackageInfo(),
            authorized = false
        )
    }
}