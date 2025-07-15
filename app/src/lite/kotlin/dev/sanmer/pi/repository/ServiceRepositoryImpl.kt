package dev.sanmer.pi.repository

import android.content.pm.PackageManager
import android.os.IBinder
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageManagerDelegate
import dev.sanmer.pi.delegate.PermissionManagerDelegate
import dev.sanmer.pi.delegate.UserManagerDelegate
import dev.sanmer.pi.model.ServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import kotlin.coroutines.resume

class ServiceRepositoryImpl() : ServiceRepository {
    private var _state = MutableStateFlow<ServiceState>(ServiceState.Pending)
    override val state get() = _state.asStateFlow()
    override val isSucceed get() = state.value.isSucceed

    private val isGranted get() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    init {
        CoroutineScope(Dispatchers.Main).launch {
            _state.value = when {
                !isAvailable() -> ServiceState.Failure(IllegalStateException("Shizuku not available"))
                !isAuthorized() -> ServiceState.Failure(IllegalStateException("Shizuku not authorized"))
                else -> ServiceState.Success(Shizuku.getUid())
            }
        }
    }

    private fun isAvailable(): Boolean {
        return Shizuku.pingBinder()
    }

    private suspend fun isAuthorized() = when {
        isGranted -> true
        else -> suspendCancellableCoroutine { continuation ->
            val listener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(
                    requestCode: Int,
                    grantResult: Int
                ) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    continuation.resume(isGranted)
                }
            }

            Shizuku.addRequestPermissionResultListener(listener)
            continuation.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(listener)
            }
            Shizuku.requestPermission(listener.hashCode())
        }
    }

    override suspend fun recreate(provider: Provider) {}

    private fun IBinder.proxy() = ShizukuBinderWrapper(this)

    override fun getAppOpsManager() = AppOpsManagerDelegate { proxy() }
    override fun getPackageManager() = PackageManagerDelegate { proxy() }
    override fun getPackageInstaller() = PackageInstallerDelegate { proxy() }
    override fun getPermissionManager() = PermissionManagerDelegate { proxy() }
    override fun getUserManager() = UserManagerDelegate { proxy() }
}