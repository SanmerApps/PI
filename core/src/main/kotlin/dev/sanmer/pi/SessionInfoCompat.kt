package dev.sanmer.pi

import android.content.pm.PackageInstaller.SessionInfo
import android.content.pm.PackageInstallerHidden
import dev.rikka.tools.refine.Refine

object SessionInfoCompat {
    val SessionInfo.userId
        get() = Refine.unsafeCast<PackageInstallerHidden.SessionInfoHidden>(this).userId
}