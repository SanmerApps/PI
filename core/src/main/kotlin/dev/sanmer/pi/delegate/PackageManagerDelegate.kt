package dev.sanmer.pi.delegate

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import dev.sanmer.pi.BuildCompat
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat.getSystemService

class PackageManagerDelegate(
    private val service: IServiceManager
) {
    private val packageManager by lazy {
        IPackageManager.Stub.asInterface(
            service.getSystemService("package")
        )
    }

    fun getApplicationInfo(packageName: String, flags: Int, userId: Int): ApplicationInfo {
        return if (BuildCompat.atLeastT) {
            packageManager.getApplicationInfo(packageName, flags.toLong(), userId)
        } else {
            packageManager.getApplicationInfo(packageName, flags, userId)
        }
    }

    fun getPackageInfo(packageName: String, flags: Int, userId: Int): PackageInfo {
        return if (BuildCompat.atLeastT) {
            packageManager.getPackageInfo(packageName, flags.toLong(), userId)
        } else {
            packageManager.getPackageInfo(packageName, flags, userId)
        }
    }

    fun getPackageUid(packageName: String, flags: Int, userId: Int): Int {
        return if (BuildCompat.atLeastT) {
            packageManager.getPackageUid(packageName, flags.toLong(), userId)
        } else {
            packageManager.getPackageUid(packageName, flags, userId)
        }
    }

    fun getInstalledPackages(flags: Int, userId: Int): List<PackageInfo> {
        return if (BuildCompat.atLeastT) {
            packageManager.getInstalledPackages(flags.toLong(), userId)
        } else {
            packageManager.getInstalledPackages(flags, userId)
        }.list
    }

    fun getInstalledApplications(flags: Int, userId: Int): List<ApplicationInfo> {
        return if (BuildCompat.atLeastT) {
            packageManager.getInstalledApplications(flags.toLong(), userId)
        } else {
            packageManager.getInstalledApplications(flags, userId)
        }.list
    }

    fun queryIntentActivities(intent: Intent, resolvedType: String?, flags: Int, userId: Int): List<ResolveInfo> {
        return if (BuildCompat.atLeastT) {
            packageManager.queryIntentActivities(intent, resolvedType, flags.toLong(), userId)
        } else {
            packageManager.queryIntentActivities(intent, resolvedType, flags, userId)
        }.list
    }

    fun getPackagesForUid(uid: Int): List<String> {
        return packageManager.getPackagesForUid(uid).toList()
    }

    fun getLaunchIntentForPackage(packageName: String, userId: Int): Intent? {
        val intentToResolve = Intent(Intent.ACTION_MAIN)
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER)
        intentToResolve.setPackage(packageName)

        val ris: List<ResolveInfo> = queryIntentActivities(
            intentToResolve, null, 0, userId
        )

        if (ris.isEmpty()) {
            return null
        }

        val intent = Intent(intentToResolve)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setClassName(
            ris[0].activityInfo.packageName,
            ris[0].activityInfo.name
        )

        return intent
    }
}