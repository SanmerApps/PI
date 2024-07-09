package dev.sanmer.pi.viewmodel

import android.content.pm.PackageInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.Compat
import dev.sanmer.pi.UserHandleCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val pm by lazy { Compat.getPackageManager() }
    private val perm by lazy { Compat.getPermissionManager() }

    var packageInfo by mutableStateOf(IPackageInfo.empty())
        private set

    var permissions by mutableStateOf(emptyList<String>())
        private set
    private val requiredPermissions = mutableStateListOf<String>()

    var isLoading by mutableStateOf(true)
        private set

    suspend fun load(packageName: String, permissions: List<String>) = withContext(Dispatchers.IO) {
        val userPreferences = userPreferencesRepository.data.first()

        if (!Compat.init(userPreferences.provider)) {
            return@withContext false
        }

        this@PermissionViewModel.packageInfo = getPackageInfo(packageName).toIPackageInfo()
        this@PermissionViewModel.permissions = permissions

        requiredPermissions.addAll(permissions)
        isLoading = false

        true
    }

    fun isRequiredPermission(permission: String): Boolean {
        return permission in requiredPermissions
    }

    fun togglePermission(permission: String) {
        if (isRequiredPermission(permission)) {
            requiredPermissions.remove(permission)
        } else {
            requiredPermissions.add(permission)
        }
    }

    fun grantPermissions() {
        requiredPermissions.forEach {
            perm.grantRuntimePermission(
                packageName = packageInfo.packageName,
                permissionName = it,
                userId = UserHandleCompat.myUserId()
            )
        }
    }

    fun permissionResults() = permissions.map {
        perm.checkPermission(
            packageName = packageInfo.packageName,
            permissionName = it,
            userId = UserHandleCompat.myUserId()
        )
    }.toIntArray()

    private fun getPackageInfo(packageName: String?): PackageInfo {
        if (packageName == null) return PackageInfo()
        return runCatching {
            pm.getPackageInfo(
                packageName, 0, UserHandleCompat.myUserId()
            )
        }.getOrNull() ?: PackageInfo()
    }
}