package dev.sanmer.pi.viewmodel

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.compat.PackageManagerCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : ViewModel() {
    private val appsFlow = MutableStateFlow(listOf<IPackageInfo>())
    val apps get() = appsFlow.asStateFlow()

    init {
        Timber.d("AppsViewModel init")

        localRepository.getAllAsFlow()
            .onEach { loadData() }
            .launchIn(viewModelScope)
    }

    fun loadData() {
        viewModelScope.launch {
            val source = async { getPackages() }
            val local = async { localRepository.getAll() }

            val authorized = local.await()
            val packageNames = mutableListOf<String>()

            appsFlow.value = source.await().map { pi ->
                packageNames.add(pi.packageName)

                val isAuthorized = authorized.find {
                    it.packageName == pi.packageName
                }?.authorized ?: false

                IPackageInfo(
                    packageInfo = pi,
                    authorized = isAuthorized
                )
            }.sortedByDescending { it.lastUpdateTime }
                .toMutableStateList()

            localRepository.delete(
                authorized.filter { it.packageName !in packageNames }
            )
        }
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        val allPackages = runCatching {
            PackageManagerCompat.getInstalledPackages(
                PackageManager.GET_PERMISSIONS, 0
            )
        }.onFailure {
            Timber.e(it, "getInstalledPackages")
        }.getOrDefault(emptyList())

        val isRequestedInstall: (PackageInfo) -> Boolean = {
            it.requestedPermissions?.contains(
                Manifest.permission.REQUEST_INSTALL_PACKAGES
            ) == true
        }

        val isNotSystemApp: (PackageInfo) -> Boolean = {
            it.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or
                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        }

        allPackages.filter {
            isRequestedInstall(it) && isNotSystemApp(it) &&
                    it.applicationInfo.enabled
        }
    }

    fun toggle(pi: IPackageInfo) {
        viewModelScope.launch {
            val authorized = !pi.authorized
            localRepository.insert(pi.copy(authorized = authorized))
        }
    }
}