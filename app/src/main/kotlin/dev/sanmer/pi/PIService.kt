package dev.sanmer.pi

import android.os.Process
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageManagerDelegate
import dev.sanmer.pi.delegate.PermissionManagerDelegate
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

sealed class PIService {
    open val platform: String
        get() = throw IllegalStateException("Pending")
    open val appOpsService: AppOpsManagerDelegate
        get() = throw IllegalStateException("Pending")
    open val packageManager: PackageManagerDelegate
        get() = throw IllegalStateException("Pending")
    open val packageInstaller: PackageInstallerDelegate
        get() = throw IllegalStateException("Pending")
    open val permissionManager: PermissionManagerDelegate
        get() = throw IllegalStateException("Pending")

    data object Pending : PIService()

    class Success(private val service: IServiceManager) : PIService() {
        override val platform by lazy {
            when (service.uid) {
                Process.ROOT_UID -> "root"
                Process.SHELL_UID -> "adb"
                else -> "unknown (${service.uid})"
            }
        }
        override val appOpsService by lazy { AppOpsManagerDelegate(service) }
        override val packageManager by lazy { PackageManagerDelegate(service) }
        override val packageInstaller by lazy { PackageInstallerDelegate(service) }
        override val permissionManager by lazy { PermissionManagerDelegate(service) }
    }

    class Failure(private val error: Throwable) : PIService() {
        override val platform get() = throw error
        override val appOpsService get() = throw error
        override val packageManager get() = throw error
        override val packageInstaller get() = throw error
        override val permissionManager get() = throw error
    }

    enum class State {
        Pending,
        Success,
        Failure;

        val isPending get() = this == Pending
        val isSucceed get() = this == Success
        val isFailed get() = this == Failure
    }

    companion object Impl : PIService() {
        private var service: PIService = Pending
        private var _stateFlow = MutableStateFlow(State.Pending)
        val stateFlow get() = _stateFlow.asStateFlow()
        val isSucceed get() = _stateFlow.value.isSucceed

        suspend fun init(provider: Provider): Boolean {
            if (isSucceed) return true
            _stateFlow.value = State.Pending
            service = try {
                when (provider) {
                    Provider.Shizuku -> Success(ServiceManagerCompat.fromShizuku())
                        .apply { _stateFlow.value = State.Success }

                    Provider.Superuser -> Success(ServiceManagerCompat.fromLibSu())
                        .apply { _stateFlow.value = State.Success }

                    Provider.None -> Failure(IllegalArgumentException("Expect provider is Shizuku or Superuser"))
                        .apply { _stateFlow.value = State.Failure }
                }
            } catch (e: Throwable) {
                Timber.e(e)
                Failure(e).apply { _stateFlow.value = State.Failure }
            }
            return isSucceed
        }

        override val platform get() = service.platform
        override val appOpsService get() = service.appOpsService
        override val packageManager get() = service.packageManager
        override val packageInstaller get() = service.packageInstaller
        override val permissionManager get() = service.permissionManager
    }
}