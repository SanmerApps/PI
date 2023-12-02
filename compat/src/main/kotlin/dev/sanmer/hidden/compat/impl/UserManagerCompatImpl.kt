package dev.sanmer.hidden.compat.impl

import android.content.pm.UserInfo
import android.os.IUserManager
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.stub.IUserManagerCompat

internal class UserManagerCompatImpl(
    private val original: IUserManager
) : IUserManagerCompat.Stub() {
    override fun getUsers(
        excludePartial: Boolean,
        excludeDying: Boolean,
        excludePreCreated: Boolean
    ): List<UserInfo> {
        return if (BuildCompat.atLeastR) {
            original.getUsers(excludePartial, excludeDying, excludePreCreated)
        } else {
            original.getUsers(excludeDying)
        }
    }

    override fun getUserInfo(userId: Int): UserInfo {
        return original.getUserInfo(userId)
    }
}