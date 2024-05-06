package dev.sanmer.pi.compat

import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sanmer.hidden.compat.ShizukuProvider
import dev.sanmer.hidden.compat.SuProvider
import dev.sanmer.hidden.compat.stub.IProvider
import dev.sanmer.pi.datastore.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object ProviderCompat {
    private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mProviderOrNull: IProvider? = null
    private val mProvider get() = checkNotNull(mProviderOrNull) {
        "IProvider haven't been received"
    }

    var current by mutableStateOf(Provider.None)
        private set
    var isAlive by mutableStateOf(false)
        private set

    private val _isAliveFlow = MutableStateFlow(false)
    val isAliveFlow get() = _isAliveFlow.asStateFlow()

    val appOpsService get() = mProvider.appOpsService
    val packageManager get() = mProvider.packageManager
    val packageInstaller get() = packageManager.packageInstallerCompat

    val version get() = mProvider.version
    val platform get() = when (mProvider.uid) {
        Process.ROOT_UID -> "root"
        Process.SHELL_UID -> "adb"
        else -> "unknown"
    }

    private fun stateObserver(alive: MutableStateFlow<Boolean>) {
        alive.onEach {
            isAlive = it
            _isAliveFlow.value = it

        }.launchIn(mScope)
    }

    private fun init() = when (current) {
        Provider.Shizuku -> with(ShizukuProvider) {
            mProviderOrNull = this
            stateObserver(isAlive)
            init()
        }

        Provider.Superuser -> with(SuProvider){
            mProviderOrNull = this
            stateObserver(isAlive)
            init()
        }
        else -> {}
    }

    fun init(provider: Provider) {
        when {
            provider == current -> {
                if (!isAlive) init()
            }
            else -> {
                if (isAlive) destroy()

                current = provider
                init()
            }
        }
    }

    fun destroy() = when (current) {
        Provider.Shizuku -> ShizukuProvider.destroy()
        Provider.Superuser -> SuProvider.destroy()
        else -> {}
    }

    fun <T> get(fallback: T, block: ProviderCompat.() -> T): T {
        return when {
            isAlive -> block(this)
            else -> fallback
        }
    }
}