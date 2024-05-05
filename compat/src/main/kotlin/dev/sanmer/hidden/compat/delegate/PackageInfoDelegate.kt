package dev.sanmer.hidden.compat.delegate

import android.content.pm.PackageInfo
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.PackageInfoCompat.compileSdkVersion
import dev.sanmer.hidden.compat.PackageInfoCompat.compileSdkVersionCodename
import dev.sanmer.hidden.compat.PackageInfoCompat.coreApp
import dev.sanmer.hidden.compat.PackageInfoCompat.isActiveApex
import dev.sanmer.hidden.compat.PackageInfoCompat.isStub
import dev.sanmer.hidden.compat.PackageInfoCompat.overlayCategory
import dev.sanmer.hidden.compat.PackageInfoCompat.overlayPriority
import dev.sanmer.hidden.compat.PackageInfoCompat.overlayTarget
import dev.sanmer.hidden.compat.PackageInfoCompat.requiredAccountType
import dev.sanmer.hidden.compat.PackageInfoCompat.requiredForAllUsers
import dev.sanmer.hidden.compat.PackageInfoCompat.restrictedAccountType
import dev.sanmer.hidden.compat.PackageInfoCompat.versionCodeMajor

@Suppress("DEPRECATION")
abstract class PackageInfoDelegate(
    private val original: PackageInfo
) : PackageInfo() {
    init {
        packageName = original.packageName
        splitNames = original.splitNames
        versionCode = original.versionCode
        versionCodeMajor = original.versionCodeMajor
        versionName = original.versionName
        baseRevisionCode = original.baseRevisionCode
        splitRevisionCodes = original.splitRevisionCodes
        sharedUserId = original.sharedUserId
        sharedUserLabel = original.sharedUserLabel
        applicationInfo = original.applicationInfo
        firstInstallTime = original.firstInstallTime
        lastUpdateTime = original.lastUpdateTime
        gids = original.gids
        activities = original.activities
        receivers = original.receivers
        services = original.services
        providers = original.providers
        instrumentation = original.instrumentation
        permissions = original.permissions
        requestedPermissions = original.requestedPermissions
        requestedPermissionsFlags = original.requestedPermissionsFlags
        signatures = original.signatures
        configPreferences = original.configPreferences
        reqFeatures = original.reqFeatures
        featureGroups = original.featureGroups
        if (BuildCompat.atLeastS) {
            attributions = original.attributions
        }
        installLocation = original.installLocation
        isStub = original.isStub
        coreApp = original.coreApp
        requiredForAllUsers = original.requiredForAllUsers
        restrictedAccountType = original.restrictedAccountType
        requiredAccountType = original.requiredAccountType
        overlayTarget = original.overlayTarget
        overlayCategory = original.overlayCategory
        overlayPriority = original.overlayPriority
        compileSdkVersion = original.compileSdkVersion
        compileSdkVersionCodename = original.compileSdkVersionCodename
        signingInfo = original.signingInfo
        isApex = original.isApex
        if (BuildCompat.atLeastU) {
            isActiveApex = original.isActiveApex
        }
    }

    val appLabel by lazy {
        val context = ContextDelegate.getContext()
        applicationInfo?.loadLabel(
            context.packageManager
        ).toString()
    }
}