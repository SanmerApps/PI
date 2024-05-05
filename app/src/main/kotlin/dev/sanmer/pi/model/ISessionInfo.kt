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
    constructor(
        session: PackageInstaller.SessionInfo,
        installer: PackageInfo?,
        app: PackageInfo?
    ) : this(
        sessionId = session.sessionId,
        userId = session.userId,
        isActive = session.isActive,
        isStaged = session.isStaged,
        isCommitted = session.isCommitted,
        installerPackageName = session.installerPackageName,
        installer = installer,
        appPackageName = session.appPackageName,
        app = app,
        appLabelInner = session.appLabel?.toString(),
        appIconInner = session.appIcon
    )

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

    companion object {
        fun staged(session: PackageInstaller.SessionInfo) = ISessionInfo(
            sessionId = session.sessionId,
            userId = session.userId,
            isActive = false,
            isStaged = true,
            isCommitted = true,
            installerPackageName = session.installerPackageName,
            installer = null,
            appPackageName = session.appPackageName,
            app = null,
            appLabelInner = null,
            appIconInner = null
        )
    }
}
