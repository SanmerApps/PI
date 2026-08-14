package dev.sanmer.pi.repository

import dev.sanmer.pi.core.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.core.delegate.PackageInstallerDelegate
import dev.sanmer.pi.core.delegate.PackageManagerDelegate
import dev.sanmer.pi.core.delegate.PermissionManagerDelegate
import dev.sanmer.pi.core.delegate.UserManagerDelegate
import dev.sanmer.pi.model.LoadData
import dev.sanmer.su.BinderWrapper
import kotlinx.coroutines.flow.StateFlow

interface SuRepository {
    val state: StateFlow<LoadData<BinderWrapper>>
    suspend fun launch()
    fun getAppOpsManager(): AppOpsManagerDelegate
    fun getPackageManager(): PackageManagerDelegate
    fun getPackageInstaller(): PackageInstallerDelegate
    fun getPermissionManager(): PermissionManagerDelegate
    fun getUserManager(): UserManagerDelegate
}