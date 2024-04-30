package dev.sanmer.hidden.compat

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.os.Build
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine

object PackageInfoCompat {

    var PackageInfo.versionCodeMajor: Int
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            versionCodeMajor
        }
        set(v: Int) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            versionCodeMajor = v
        }

    var PackageInfo.isStub: Boolean
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            isStub
        }
        set(v: Boolean) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            isStub = v
        }

    var PackageInfo.coreApp: Boolean
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            coreApp
        }
        set(v: Boolean) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            coreApp = v
        }

    var PackageInfo.requiredForAllUsers: Boolean
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            requiredForAllUsers
        }
        set(v: Boolean) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            requiredForAllUsers = v
        }

    var PackageInfo.restrictedAccountType: String?
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            restrictedAccountType
        }
        set(v: String?) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            restrictedAccountType = v
        }

    var PackageInfo.requiredAccountType: String?
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            requiredAccountType
        }
        set(v: String?) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            requiredAccountType = v
        }

    var PackageInfo.overlayTarget: String?
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            overlayTarget
        }
        set(v: String?) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            overlayTarget = v
        }

    var PackageInfo.overlayCategory: String?
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            overlayCategory
        }
        set(v: String?) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            overlayCategory = v
        }

    var PackageInfo.overlayPriority: Int
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            overlayPriority
        }
        set(v: Int) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            overlayPriority = v
        }

    var PackageInfo.mOverlayIsStatic: Boolean
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            mOverlayIsStatic
        }
        set(v: Boolean) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            mOverlayIsStatic = v
        }

    var PackageInfo.compileSdkVersion: Int
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            compileSdkVersion
        }
        set(v: Int) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            compileSdkVersion = v
        }

    var PackageInfo.compileSdkVersionCodename: String?
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            compileSdkVersionCodename
        }
        set(v: String?) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            compileSdkVersionCodename = v
        }

    var PackageInfo.isActiveApex: Boolean
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        get() = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            isActiveApex
        }
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        set(v: Boolean) = with(Refine.unsafeCast<PackageInfoHidden>(this)) {
            isActiveApex = v
        }

    val PackageInfo.isOverlayPackage get() =
        Refine.unsafeCast<PackageInfoHidden>(this)
            .isOverlayPackage

    val PackageInfo.isPreinstalled get() =
        lastUpdateTime <= 1230768000000 // 2009-01-01 08:00:00 GMT+8

    val PackageInfo.isSystemApp: Boolean get() {
        if (isEmpty) return false

        return applicationInfo.flags and (
                ApplicationInfo.FLAG_SYSTEM or
                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        ) != 0
    }

    val PackageInfo?.isEmpty get() = this?.packageName == null || applicationInfo == null

    val PackageInfo.isNotEmpty get() = !isEmpty
}