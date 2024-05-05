package dev.sanmer.hidden.compat

import android.os.UserHandleHidden

object UserHandleCompat {
    fun myUserId(): Int {
        return UserHandleHidden.myUserId()
    }
}