package dev.sanmer.pi.model

import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import androidx.compose.runtime.Immutable

@Immutable
data class ISessionInfo(
    val sessionId: Int,
    val isActive: Boolean,
    val isStaged: Boolean,
    val isCommitted: Boolean,
    val installer: PackageInfo?,
    val app: PackageInfo?
) {
    constructor(
        sessionInfo: PackageInstaller.SessionInfo,
        installer: PackageInfo?,
        app: PackageInfo?
    ) : this(
        sessionId = sessionInfo.sessionId,
        isActive = sessionInfo.isActive,
        isStaged = sessionInfo.isStaged,
        isCommitted = sessionInfo.isCommitted,
        installer = installer,
        app = app
    )
}
