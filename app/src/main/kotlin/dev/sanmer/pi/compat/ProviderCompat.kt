package dev.sanmer.pi.compat

import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sanmer.hidden.compat.ShizukuProvider
import dev.sanmer.hidden.compat.SuProvider
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat
import dev.sanmer.hidden.compat.stub.IServiceManager
import dev.sanmer.pi.datastore.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

object ProviderCompat {
    private var mServiceOrNull: IServiceManager? = null
    private val mService get() = checkNotNull(mServiceOrNull) {
        "IServiceManager haven't been received"
    }

    var isAlive by mutableStateOf(false)
        private set

    private val _isAliveFlow = MutableStateFlow(false)
    val isAliveFlow get() = _isAliveFlow.asStateFlow()

    val appOpsService: IAppOpsServiceCompat get() = mService.appOpsService
    val packageManager: IPackageManagerCompat get() = mService.packageManager
    val packageInstaller: IPackageInstallerCompat get() = packageManager.packageInstaller

    val version get() = mService.version
    val platform get() = when (mService.uid) {
        Process.ROOT_UID -> "root"
        Process.SHELL_UID -> "adb"
        else -> "unknown"
    }

    private fun state(alive: Boolean): Boolean {
        isAlive = alive
        _isAliveFlow.value = alive

        return alive
    }

    suspend fun init(provider: Provider) = withContext(Dispatchers.Main) {
        if (isAlive) {
            return@withContext true
        }

        try {
            mServiceOrNull = when (provider) {
                Provider.Shizuku -> ShizukuProvider.launch()
                Provider.Superuser -> SuProvider.launch()
                else -> null
            }

            state(true)
        } catch (e: Exception) {
            Timber.e(e)

            state(false)
        }
    }

    fun <T> get(fallback: T, block: ProviderCompat.() -> T): T {
        return when {
            isAlive -> block(this)
            else -> fallback
        }
    }
}