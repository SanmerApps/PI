package dev.sanmer.pi.delegate

import android.content.Context
import android.content.pm.UserInfo
import android.os.IBinder
import android.os.IUserManager
import android.os.ServiceManager
import dev.sanmer.pi.BuildCompat

class UserManagerDelegate(
    private val proxy: IBinder.() -> IBinder = { this }
) {
    private val userManager by lazy {
        IUserManager.Stub.asInterface(
            ServiceManager.getService(Context.USER_SERVICE).proxy()
        )
    }

    fun getUsers(): List<UserInfo> {
        return try {
            userManager.getUsers(true, true, true)
        } catch (e: NoSuchMethodError) {
            if (BuildCompat.atLeastB) { //TODO: Remove at SDK 37
                userManager.getUsers(true)
            } else {
                throw IllegalStateException(e)
            }
        }
    }

    fun getUserInfo(userId: Int): UserInfo {
        return userManager.getUserInfo(userId)
    }
}