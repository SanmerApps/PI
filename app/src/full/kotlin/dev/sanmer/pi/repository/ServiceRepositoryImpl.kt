package dev.sanmer.pi.repository

import dev.sanmer.pi.Logger
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageManagerDelegate
import dev.sanmer.pi.delegate.PermissionManagerDelegate
import dev.sanmer.pi.delegate.UserManagerDelegate
import dev.sanmer.pi.model.ServiceState
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat
import dev.sanmer.su.ServiceManagerCompat.proxyBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServiceRepositoryImpl(
    private val preferenceRepository: PreferenceRepository
) : ServiceRepository {
    private val logger = Logger.Android("ServiceRepositoryImpl")

    private var _state = MutableStateFlow<ServiceState>(ServiceState.Pending)
    override val state = _state.asStateFlow()
    override val isSucceed get() = state.value.isSucceed

    init {
        preferenceObserver()
    }

    private fun preferenceObserver() {
        CoroutineScope(Dispatchers.IO).launch {
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
        logger.e(e)
        ServiceState.Failure(e)
    }

    override suspend fun recreate(provider: Provider) {
        _state.update { create(provider) }
    }

    private fun <T> unsafe(block: (IServiceManager) -> T): T {
        return when (val value = state.value) {
            is ServiceState.Success -> block(value.service)
            is ServiceState.Failure -> throw value.error
            ServiceState.Pending -> throw IllegalStateException("Pending")
        }
    }

    override fun getAppOpsManager() = unsafe { ism -> AppOpsManagerDelegate { proxyBy(ism) } }
    override fun getPackageManager() = unsafe { ism -> PackageManagerDelegate { proxyBy(ism) } }
    override fun getPackageInstaller() = unsafe { ism -> PackageInstallerDelegate { proxyBy(ism) } }
    override fun getPermissionManager() =
        unsafe { ism -> PermissionManagerDelegate { proxyBy(ism) } }

    override fun getUserManager() = unsafe { ism -> UserManagerDelegate { proxyBy(ism) } }
}