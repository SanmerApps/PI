package dev.sanmer.hidden.compat.impl

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.ParceledListSlice
import android.content.pm.ResolveInfo
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat

internal class PackageManagerCompatImpl(
    private val original: IPackageManager
) : IPackageManagerCompat.Stub() {
    private val packageInstaller by lazy {
        PackageInstallerCompatImpl(
            original.packageInstaller
        )
    }

    override fun getPackageInstaller(): IPackageInstallerCompat {
        return packageInstaller
    }

    override fun getApplicationInfo(
        packageName: String,
        flags: Int,
        userId: Int
    ): ApplicationInfo {
        return if (BuildCompat.atLeastT) {
            original.getApplicationInfo(packageName, flags.toLong(), userId)
        } else {
            original.getApplicationInfo(packageName, flags, userId)
        }
    }

    override fun getPackageInfo(
        packageName: String,
        flags: Int,
        userId: Int
    ): PackageInfo {
        return if (BuildCompat.atLeastT) {
            original.getPackageInfo(packageName, flags.toLong(), userId)
        } else {
            original.getPackageInfo(packageName, flags, userId)
        }
    }

    override fun getPackageUid(
        packageName: String,
        flags: Int,
        userId: Int
    ): Int {
        return if (BuildCompat.atLeastT) {
            original.getPackageUid(packageName, flags.toLong(), userId)
        } else {
            original.getPackageUid(packageName, flags, userId)
        }
    }

    override fun getInstalledPackages(
        flags: Int,
        userId: Int
    ): ParceledListSlice<PackageInfo> {
        val packages = if (BuildCompat.atLeastT) {
            original.getInstalledPackages(flags.toLong(), userId)
        } else {
            original.getInstalledPackages(flags, userId)
        }.list

        return ParceledListSlice(packages)
    }

    override fun getInstalledApplications(
        flags: Int,
        userId: Int
    ): ParceledListSlice<ApplicationInfo> {
        val applications = if (BuildCompat.atLeastT) {
            original.getInstalledApplications(flags.toLong(), userId)
        } else {
            original.getInstalledApplications(flags, userId)
        }.list

        return ParceledListSlice(applications)
    }

    override fun queryIntentActivities(
        intent: Intent,
        resolvedType: String?,
        flags: Int,
        userId: Int
    ): ParceledListSlice<ResolveInfo> {
        val activities = if (BuildCompat.atLeastT) {
            original.queryIntentActivities(intent, resolvedType, flags.toLong(), userId)
        } else {
            original.queryIntentActivities(intent, resolvedType, flags, userId)
        }.list

        return ParceledListSlice(activities)
    }

    override fun getPackagesForUid(uid: Int): Array<String> {
        return original.getPackagesForUid(uid)
    }

    override fun getLaunchIntentForPackage(packageName: String, userId: Int): Intent? {
        val intentToResolve = Intent(Intent.ACTION_MAIN)
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER)
        intentToResolve.setPackage(packageName)

        val ris: List<ResolveInfo> = queryIntentActivities(
            intentToResolve, null, 0, userId
        ).list

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