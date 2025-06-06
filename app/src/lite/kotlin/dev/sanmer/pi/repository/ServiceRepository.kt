package dev.sanmer.pi.repository

import android.content.pm.PackageManager
import android.os.IBinder
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageManagerDelegate
import dev.sanmer.pi.delegate.PermissionManagerDelegate
import dev.sanmer.pi.delegate.UserManagerDelegate
import dev.sanmer.pi.model.ShizukuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ServiceRepository @Inject constructor() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var _state = MutableStateFlow<ShizukuState>(ShizukuState.Pending)
    val state get() = _state.asStateFlow()

    private val isGranted get() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    init {
        coroutineScope.launch {
            _state.value = when {
                !isAvailable() -> ShizukuState.Failure(IllegalStateException("Shizuku not available"))
                !isAuthorized() -> ShizukuState.Failure(IllegalStateException("Shizuku not authorized"))
                else -> ShizukuState.Success(Shizuku.getUid())
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

    private fun IBinder.proxy() = ShizukuBinderWrapper(this)

    fun getAppOpsManager() = AppOpsManagerDelegate { proxy() }
    fun getPackageManager() = PackageManagerDelegate { proxy() }
    fun getPackageInstaller() = PackageInstallerDelegate { proxy() }
    fun getPermissionManager() = PermissionManagerDelegate { proxy() }
    fun getUserManager() = UserManagerDelegate { proxy() }
}