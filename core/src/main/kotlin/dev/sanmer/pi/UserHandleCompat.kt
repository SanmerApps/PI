package dev.sanmer.pi

import android.os.UserHandleHidden

object UserHandleCompat {
    fun myUserId(): Int {
        return UserHandleHidden.myUserId()
    }
}