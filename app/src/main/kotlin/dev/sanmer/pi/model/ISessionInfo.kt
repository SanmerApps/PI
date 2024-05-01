package dev.sanmer.pi.model

import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import dev.sanmer.hidden.compat.SessionInfoCompat.userId
import dev.sanmer.hidden.compat.delegate.ContextDelegate

@Immutable
data class ISessionInfo(
    val sessionId: Int,
    val userId: Int,
    val isActive: Boolean,
    val isStaged: Boolean,
    val isCommitted: Boolean,
    val installerPackageName: String?,
    val installer: PackageInfo?,
    val appPackageName: String?,
    val app: PackageInfo?,
    private val appLabelInner: String?,
    private val appIconInner: Bitmap?
) {
    private val context by lazy {
        ContextDelegate.getContext()
    }

    val installerLabel by lazy {
        val pm = context.packageManager
        installer?.applicationInfo?.loadLabel(pm)?.toString()
    }

    val appLabel by lazy {
        val pm = context.packageManager
        appLabelInner ?: app?.applicationInfo?.loadLabel(pm)?.toString()
    }

    val appIcon by lazy {
        appIconInner ?: app
    }

    constructor(
        sessionInfo: PackageInstaller.SessionInfo,
        installer: PackageInfo?,
        app: PackageInfo?
    ) : this(
        sessionId = sessionInfo.sessionId,
        userId = sessionInfo.userId,
        isActive = sessionInfo.isActive,
        isStaged = sessionInfo.isStaged,
        isCommitted = sessionInfo.isCommitted,
        installerPackageName = sessionInfo.installerPackageName,
        installer = installer,
        appPackageName = sessionInfo.appPackageName,
        app = app,
        appLabelInner = sessionInfo.appLabel?.toString(),
        appIconInner = sessionInfo.appIcon
    )
}
