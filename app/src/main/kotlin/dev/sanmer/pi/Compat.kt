package dev.sanmer.pi

import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageManagerDelegate
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

object Compat {
    private var mServiceOrNull: IServiceManager? = null
    private val mService get() = checkNotNull(mServiceOrNull) {
        "IServiceManager haven't been received"
    }

    var isAlive by mutableStateOf(false)
        private set

    private val _isAliveFlow = MutableStateFlow(false)
    val isAliveFlow get() = _isAliveFlow.asStateFlow()

    val platform get() = when (mService.uid) {
        Process.ROOT_UID -> "root"
        Process.SHELL_UID -> "adb"
        else -> "unknown"
    }

    private fun state(): Boolean {
        isAlive = mServiceOrNull != null
        _isAliveFlow.value = isAlive

        return isAlive
    }

    suspend fun init(provider: Provider) = when {
        isAlive -> true
        else -> try {
            mServiceOrNull = when (provider) {
                Provider.Shizuku -> ServiceManagerCompat.fromShizuku()
                Provider.Superuser -> ServiceManagerCompat.fromLibSu()
                else -> null
            }

            state()
        } catch (e: Throwable) {
            Timber.e(e)

            mServiceOrNull = null
            state()
        }
    }

    fun <T> get(fallback: T, block: Compat.() -> T): T {
        return when {
            isAlive -> block(this)
            else -> fallback
        }
    }

    fun getAppOpsService() = AppOpsManagerDelegate(mService)
    fun getPackageManager() = PackageManagerDelegate(mService)
    fun getPackageInstaller() = PackageInstallerDelegate(mService)
}