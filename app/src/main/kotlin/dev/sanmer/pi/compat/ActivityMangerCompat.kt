package dev.sanmer.pi.compat

import android.app.ActivityManager
import android.app.IActivityManager
import android.os.Process
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

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

    fun getCallingUid(): Int {
        val processes = runningAppProcesses
        @Suppress("UNUSED_VARIABLE")
        val self = processes[0]
        val before1 = processes[1]
        val before2 = processes[2]

        return if (before1.uid != Process.SYSTEM_UID) {
            before1.uid
        } else {
            before2.uid
        }
    }
}