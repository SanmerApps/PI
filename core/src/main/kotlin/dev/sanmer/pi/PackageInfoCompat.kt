package dev.sanmer.pi

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine

object PackageInfoCompat {
    private val PackageInfo.original
        inline get() = Refine.unsafeCast<PackageInfoHidden>(this)

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
        inline get() = this?.packageName == null || applicationInfo == null

    val PackageInfo?.isNotEmpty
        inline get() = !isEmpty

    val PackageInfo.isSystemApp: Boolean
        inline get() = applicationInfo?.let {
            it.flags and (ApplicationInfo.FLAG_SYSTEM or
                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } ?: false

    fun PackageInfo?.orEmpty() = this ?: PackageInfo()

    val PackageInfo.targetSdkVersion: Int
        inline get() = applicationInfo?.targetSdkVersion ?: 0

    val PackageInfo.minSdkVersion: Int
        inline get() = applicationInfo?.minSdkVersion ?: 0

    fun PackageInfo.loadLabel(context: Context): String? {
        return applicationInfo?.loadLabel(context.packageManager)?.toString()
    }

    fun PackageInfo.loadUnbadgedIcon(context: Context): Drawable? {
        return applicationInfo?.loadUnbadgedIcon(context.packageManager)
    }

    fun composeLongVersionCode(major: Int, minor: Int): Long {
        return ((major.toLong()) shl 32) or ((minor.toLong()) and 0xffffffffL)
    }
}