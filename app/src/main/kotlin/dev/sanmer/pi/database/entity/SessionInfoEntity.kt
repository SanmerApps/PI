package dev.sanmer.pi.database.entity

import android.content.pm.PackageInstaller
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.sanmer.hidden.compat.SessionInfoCompat.userId
import dev.sanmer.pi.model.ISessionInfo

@Entity(tableName = "sessions")
data class SessionInfoEntity(
    @PrimaryKey val sessionId: Int,
    val userId: Int,
    val isActive: Boolean,
    val isStaged: Boolean,
    val isCommitted: Boolean,
    val installerPackageName: String?,
    val appPackageName: String?
) {
    constructor(sessionInfo: ISessionInfo) : this(
        sessionId = sessionInfo.sessionId,
        userId = sessionInfo.userId,
        isActive = sessionInfo.isActive,
        isStaged = sessionInfo.isStaged,
        isCommitted = sessionInfo.isCommitted,
        installerPackageName = sessionInfo.installerPackageName,
        appPackageName = sessionInfo.appPackageName
    )

    constructor(sessionInfo: PackageInstaller.SessionInfo) : this(
        sessionId = sessionInfo.sessionId,
        userId = sessionInfo.userId,
        isActive = sessionInfo.isActive,
        isStaged = sessionInfo.isStaged,
        isCommitted = sessionInfo.isCommitted,
        installerPackageName = sessionInfo.installerPackageName,
        appPackageName = sessionInfo.appPackageName
    )

    fun toISessionInfo() = ISessionInfo(
        sessionId = sessionId,
        userId = userId,
        isActive = isActive,
        isStaged = isStaged,
        isCommitted = isCommitted,
        installerPackageName = installerPackageName,
        installer = null,
        appPackageName = appPackageName,
        app = null,
        appLabelInner = null,
        appIconInner = null
    )
}
