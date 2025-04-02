package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageManagerDelegate
import dev.sanmer.pi.delegate.PermissionManagerDelegate
import dev.sanmer.pi.model.ServiceState
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepository @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var _state = MutableStateFlow<ServiceState>(ServiceState.Pending)
    val state get() = _state.asStateFlow()

    val isSucceed get() = state.value.isSucceed

    init {
        preferenceObserver()
    }

    private fun preferenceObserver() {
        coroutineScope.launch {
            preferenceRepository.data.collect { preference ->
                _state.update { if (!it.isSucceed) create(preference.provider) else it }
            }
        }
    }

    private suspend fun create(provider: Provider) = try {
        when (provider) {
            Provider.None -> ServiceState.Pending
            Provider.Shizuku -> ServiceState.Success(ServiceManagerCompat.fromShizuku())
            Provider.Superuser -> ServiceState.Success(ServiceManagerCompat.fromLibSu())
        }
    } catch (e: Throwable) {
        Timber.e(e)
        ServiceState.Failure(e)
    }

    suspend fun recreate(provider: Provider) {
        _state.update { create(provider) }
    }

    private fun <T> unsafe(block: (IServiceManager) -> T): T {
        return when (val value = state.value) {
            is ServiceState.Success -> block(value.service)
            is ServiceState.Failure -> throw value.error
            ServiceState.Pending -> throw IllegalStateException("Pending")
        }
    }

    fun getAppOpsManager() = unsafe { AppOpsManagerDelegate(it) }
    fun getPackageManager() = unsafe { PackageManagerDelegate(it) }
    fun getPackageInstaller() = unsafe { PackageInstallerDelegate(it) }
    fun getPermissionManager() = unsafe { PermissionManagerDelegate(it) }
}