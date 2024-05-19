package dev.sanmer.hidden.compat.shizuku

import android.content.ComponentName
import dev.sanmer.hidden.compat.Const
import dev.sanmer.hidden.compat.impl.ServiceManagerImpl
import rikka.shizuku.Shizuku

internal class ShizukuService : Shizuku.UserServiceArgs(
    ComponentName(
        Const.PACKAGE_NAME,
        ServiceManagerImpl::class.java.name
    )
) {
    init {
        daemon(false)
        debuggable(false)
        version(Const.VERSION_CODE)
        processNameSuffix("shizuku")
    }
}