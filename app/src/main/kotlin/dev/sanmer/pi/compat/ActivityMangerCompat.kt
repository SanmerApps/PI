package dev.sanmer.pi.compat

import android.app.ActivityManager
import android.app.IActivityManager
import android.os.Process
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

object ActivityMangerCompat {
    private val activityManager: IActivityManager by lazy {
        IActivityManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity"))
        )
    }

    fun getTasks(maxNum: Int): List<ActivityManager.RunningTaskInfo> {
        val tasks = activityManager.getTasks(maxNum)
        return tasks ?: emptyList()
    }

    fun getRecentTasks(maxNum: Int, flags: Int, userId: Int): List<ActivityManager.RecentTaskInfo> {
        val tasks = activityManager.getRecentTasks(maxNum, flags, userId)
        return if (tasks != null) {
            tasks.list
        } else {
            emptyList()
        }
    }

    val runningAppProcesses: List<ActivityManager.RunningAppProcessInfo> get() =
        activityManager.runningAppProcesses ?: emptyList()

    fun getCallingPackage(targetPackageName: String): String {
        val task = getTasks(1).first()
        val packageName = task.baseActivity?.packageName
        if (packageName != null) return packageName

        // unused (maybe)
        val home = PackageManagerCompat.getHomeActivities()
        val processes = runningAppProcesses.first {
            Timber.d("${it.uid}, ${it.pkgList.toList()}")
            val isTarget = targetPackageName in it.pkgList
            val isSystem = it.uid == Process.SYSTEM_UID
            val isHome = home.packageName in it.pkgList

            !(isTarget || isSystem || isHome)
        }

        return processes.pkgList.first()
    }
}