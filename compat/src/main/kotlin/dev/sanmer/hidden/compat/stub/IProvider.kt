package dev.sanmer.hidden.compat.stub

import kotlinx.coroutines.flow.StateFlow

interface IProvider {
    val uid: Int
    val pid: Int
    val version: Int
    val seLinuxContext: String

    val appOpsService: IAppOpsServiceCompat
    val packageManager: IPackageManagerCompat
    val userManager: IUserManagerCompat

    val isAlive: StateFlow<Boolean>
    fun init()
    fun destroy()
}