package dev.sanmer.pi.delegate

import android.content.Context
import android.content.pm.UserInfo
import android.os.IUserManager
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat.getSystemService

class UserManagerDelegate(
    private val service: IServiceManager
) {
    private val userManager by lazy {
        IUserManager.Stub.asInterface(
            service.getSystemService(Context.USER_SERVICE)
        )
    }

    fun getUsers(): List<UserInfo> {
        return userManager.getUsers(true, true, true)
    }

    fun getUserInfo(userId: Int): UserInfo {
        return userManager.getUserInfo(userId)
    }
}