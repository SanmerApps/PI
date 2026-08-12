package dev.sanmer.pi.core.compat

import android.os.UserHandleHidden

object UserHandleCompat {
    fun myUserId() = UserHandleHidden.myUserId()
}