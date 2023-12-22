package dev.sanmer.hidden.compat

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import dev.rikka.tools.refine.Refine

object PackageInfoCompat {
    val PackageInfo.isOverlayPackage get() =
        Refine.unsafeCast<PackageInfoHidden>(this)
            .isOverlayPackage

    val PackageInfo.isPreinstalled get() =
        lastUpdateTime <= 1230768000000 // 2009-01-01 08:00:00 GMT+8

    val PackageInfo.isSystemApp get() =
        applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or
                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}