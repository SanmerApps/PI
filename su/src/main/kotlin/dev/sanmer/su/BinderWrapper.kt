package dev.sanmer.su

import android.os.IBinder

interface BinderWrapper {
    val ownerPackageName: String get() = ""
    fun wrap(original: IBinder): IBinder
}