package dev.sanmer.pi.model

import android.content.pm.PackageInfo
import androidx.compose.runtime.Immutable
import dev.sanmer.hidden.compat.delegate.PackageInfoDelegate

@Immutable
data class IPackageInfo(
    private val inner: PackageInfo,
    val isAuthorized: Boolean,
    val isRequester: Boolean,
    val isExecutor: Boolean
) : PackageInfoDelegate(inner) {
    companion object {
        fun PackageInfo.toIPackageInfo(
            isAuthorized: Boolean = false,
            isRequester: Boolean = false,
            isExecutor: Boolean = false
        ) = IPackageInfo(
            inner = this,
            isAuthorized = isAuthorized,
            isRequester = isRequester,
            isExecutor = isExecutor
        )

        fun empty() = IPackageInfo(
            inner = PackageInfo(),
            isAuthorized = false,
            isRequester = false,
            isExecutor = false
        )
    }
}