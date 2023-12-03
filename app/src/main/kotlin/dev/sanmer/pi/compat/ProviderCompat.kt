package dev.sanmer.pi.compat


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sanmer.hidden.compat.ShizukuProvider
import dev.sanmer.hidden.compat.SuProvider
import dev.sanmer.hidden.compat.stub.IProvider
import dev.sanmer.pi.app.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object ProviderCompat {
    private lateinit var provider: IProvider
    var isAlive by mutableStateOf(false)
        private set

    val packageManagerCompat get() = provider.packageManagerCompat
    val userManagerCompat get() = provider.userManagerCompat

    val version get() = provider.version
    val platform get() = when (provider.uid) {
        IProvider.ROOT_UID -> "root"
        IProvider.ADB_UID -> "adb"
        else -> "unknown"
    }

    fun init(mode: Settings.Provider, scope: CoroutineScope) {
        when (mode) {
            Settings.Provider.None -> {}
            Settings.Provider.Shizuku -> {
                ShizukuProvider.apply {
                    provider = this
                    init()
                }

                ShizukuProvider.isAlive
                    .onEach { isAlive = it }
                    .launchIn(scope)
            }
            Settings.Provider.SuperUser -> {
                SuProvider.apply {
                    provider = this
                    init()
                }

                SuProvider.isAlive
                    .onEach { isAlive = it }
                    .launchIn(scope)
            }
        }
    }

    fun destroy(mode: Settings.Provider) = when (mode) {
        Settings.Provider.None -> {}
        Settings.Provider.Shizuku -> ShizukuProvider.destroy()
        Settings.Provider.SuperUser -> SuProvider.destroy()
    }
}