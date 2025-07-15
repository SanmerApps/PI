package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageManagerDelegate
import dev.sanmer.pi.delegate.PermissionManagerDelegate
import dev.sanmer.pi.delegate.UserManagerDelegate
import dev.sanmer.pi.model.ServiceState
import kotlinx.coroutines.flow.StateFlow

interface ServiceRepository {
    val state: StateFlow<ServiceState>
    val isSucceed: Boolean
    suspend fun recreate(provider: Provider)
    fun getAppOpsManager(): AppOpsManagerDelegate
    fun getPackageManager(): PackageManagerDelegate
    fun getPackageInstaller(): PackageInstallerDelegate
    fun getPermissionManager(): PermissionManagerDelegate
    fun getUserManager(): UserManagerDelegate
}