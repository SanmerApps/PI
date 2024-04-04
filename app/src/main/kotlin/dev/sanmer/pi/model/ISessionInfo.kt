package dev.sanmer.pi.model

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class ISessionInfo(
    val sessionId: Int,
    val isActive: Boolean,
    val isStaged: Boolean,
    val isCommitted: Boolean,
    val installerPackageName: String?,
    val installer: PackageInfo?,
    val appPackageName: String?,
    val app: PackageInfo?,
    private val appLabel: String?,
    private val appIcon: Bitmap?
) {
    fun loadInstallerLabel(context: Context): String? {
        val pm = context.packageManager
        return installer?.applicationInfo?.loadLabel(pm)?.toString()
    }

    fun loadAppLabel(context: Context): String? {
        val pm = context.packageManager
        return appLabel ?: app?.applicationInfo?.loadLabel(pm)?.toString()
    }

    fun appIcon(): Any? {
        return appIcon ?: app
    }

    constructor(
        sessionInfo: PackageInstaller.SessionInfo,
        installer: PackageInfo?,
        app: PackageInfo?
    ) : this(
        sessionId = sessionInfo.sessionId,
        isActive = sessionInfo.isActive,
        isStaged = sessionInfo.isStaged,
        isCommitted = sessionInfo.isCommitted,
        installerPackageName = sessionInfo.installerPackageName,
        installer = installer,
        appPackageName = sessionInfo.appPackageName,
        app = app,
        appLabel = sessionInfo.appLabel?.toString(),
        appIcon = sessionInfo.appIcon
    )
}
