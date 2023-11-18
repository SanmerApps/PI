package dev.sanmer.pi.compat

import android.content.pm.UserInfo
import android.os.IUserManager
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object UserManagerCompat {
    private val userManager: IUserManager by lazy {
        IUserManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("user"))
        )
    }

    fun getUsers(
        excludePartial: Boolean,
        excludeDying: Boolean,
        excludePreCreated: Boolean
    ): List<UserInfo> {
        return if (BuildCompat.atLeastR) {
            userManager.getUsers(excludePartial, excludeDying, excludePreCreated)
        } else {
            userManager.getUsers(excludeDying)
        }
    }

    fun getUserInfo(userId: Int): UserInfo? {
        return userManager.getUserInfo(userId)
    }
}