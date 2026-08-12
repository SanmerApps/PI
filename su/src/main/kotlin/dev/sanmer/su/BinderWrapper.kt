package dev.sanmer.su

import android.os.IBinder

interface BinderWrapper {
    fun getUid(): Int
    fun getSELinuxContext(): String
    fun wrap(original: IBinder): IBinder
}