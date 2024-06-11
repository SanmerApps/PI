package dev.sanmer.pi

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.os.Build
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine

object PackageInfoCompat {
    internal val PackageInfo.original
        get() = Refine.unsafeCast<PackageInfoHidden>(this)
    
    var PackageInfo.versionCodeMajor: Int
        get() = original.versionCodeMajor
        set(v) { original.versionCodeMajor = v }

    var PackageInfo.isStub: Boolean
        get() = original.isStub
        set(v) { original.isStub = v }

    var PackageInfo.coreApp: Boolean
        get() = original.coreApp
        set(v) { original.coreApp = v }

    var PackageInfo.requiredForAllUsers: Boolean
        get() = original.requiredForAllUsers
        set(v) { original.requiredForAllUsers = v }

    var PackageInfo.restrictedAccountType: String?
        get() = original.restrictedAccountType
        set(v) { original.restrictedAccountType = v }

    var PackageInfo.requiredAccountType: String?
        get() = original.requiredAccountType
        set(v) { original.requiredAccountType = v }

    var PackageInfo.overlayTarget: String?
        get() = original.overlayTarget
        set(v) { original.overlayTarget = v }

    var PackageInfo.overlayCategory: String?
        get() = original.overlayCategory
        set(v) { original.overlayCategory = v }

    var PackageInfo.overlayPriority: Int
        get() = original.overlayPriority
        set(v) { original.overlayPriority = v }

    var PackageInfo.compileSdkVersion: Int
        get() = original.compileSdkVersion
        set(v) { original.compileSdkVersion = v }

    var PackageInfo.compileSdkVersionCodename: String?
        get() = original.compileSdkVersionCodename
        set(v) { original.compileSdkVersionCodename = v }

    var PackageInfo.isActiveApex: Boolean
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        get() = original.isActiveApex
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        set(v) { original.isActiveApex = v }

    val PackageInfo.isOverlayPackage
        get() = original.isOverlayPackage

    val PackageInfo?.isEmpty
        get() = this?.packageName == null || applicationInfo == null

    val PackageInfo?.isNotEmpty
        get() = !isEmpty

    val PackageInfo.isSystemApp: Boolean get() {
        if (isEmpty) return false

        return applicationInfo.flags and (
                ApplicationInfo.FLAG_SYSTEM or
                        ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                ) != 0
    }
}