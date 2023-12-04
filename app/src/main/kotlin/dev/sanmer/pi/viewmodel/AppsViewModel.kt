package dev.sanmer.pi.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.compat.ContextCompat.userId
import dev.sanmer.pi.compat.PackageInfoCompat.isSystemApp
import dev.sanmer.pi.compat.ProviderCompat
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
    private val localRepository: LocalRepository,
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { context.packageManager }
    private val pmCompat get() = ProviderCompat.packageManagerCompat

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
                    authorized = isAuthorized,
                    pm = pm
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
            pmCompat.getInstalledPackages(
                PackageManager.GET_PERMISSIONS, context.userId
            ).list
        }.onFailure {
            Timber.e(it, "getInstalledPackages")
        }.getOrDefault(emptyList())

        val isRequestedInstall: (PackageInfo) -> Boolean = {
            it.requestedPermissions?.contains(
                Manifest.permission.REQUEST_INSTALL_PACKAGES
            ) == true
        }

        allPackages.filter {
            isRequestedInstall(it) && !it.isSystemApp && it.applicationInfo.enabled
        }
    }

    fun toggle(pi: IPackageInfo) {
        viewModelScope.launch {
            val authorized = !pi.authorized
            localRepository.insert(pi.copy(authorized = authorized))
        }
    }
}