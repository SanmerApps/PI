package dev.sanmer.hidden.compat.shizuku

import android.content.ComponentName
import dev.sanmer.hidden.compat.BuildConfig
import dev.sanmer.hidden.compat.impl.ServiceManagerImpl
import rikka.shizuku.Shizuku

internal class ShizukuService : Shizuku.UserServiceArgs(
    ComponentName(
        BuildConfig.APPLICATION_ID,
        ServiceManagerImpl::class.java.name
    )
) {
    init {
        daemon(true)
        debuggable(BuildConfig.DEBUG)
        version(BuildConfig.VERSION_CODE)
        processNameSuffix("shizuku")
    }
}