package dev.sanmer.pi.compat


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sanmer.hidden.compat.ShizukuProvider
import dev.sanmer.hidden.compat.SuProvider
import dev.sanmer.hidden.compat.stub.IProvider
import dev.sanmer.pi.app.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object ProviderCompat {
    private lateinit var mProvider: IProvider
    var isAlive by mutableStateOf(false)
        private set

    val packageManagerCompat get() = mProvider.packageManagerCompat
    val userManagerCompat get() = mProvider.userManagerCompat

    val version get() = mProvider.version
    val platform get() = when (mProvider.uid) {
        IProvider.ROOT_UID -> "root"
        IProvider.ADB_UID -> "adb"
        else -> "unknown"
    }

    private var mMode = Settings.Provider.None
    private var mScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun init(
        mode: Settings.Provider? = null,
        scope: CoroutineScope? = null
    ) {
        mMode = mode ?: mMode
        mScope = scope ?: mScope

        when (mMode) {
            Settings.Provider.None -> {}
            Settings.Provider.Shizuku -> {
                ShizukuProvider.apply {
                    mProvider = this
                    init()
                }

                ShizukuProvider.isAlive
                    .onEach { isAlive = it }
                    .launchIn(mScope)
            }
            Settings.Provider.SuperUser -> {
                SuProvider.apply {
                    mProvider = this
                    init()
                }

                SuProvider.isAlive
                    .onEach { isAlive = it }
                    .launchIn(mScope)
            }
        }
    }

    fun destroy() = when (mMode) {
        Settings.Provider.None -> {}
        Settings.Provider.Shizuku -> ShizukuProvider.destroy()
        Settings.Provider.SuperUser -> SuProvider.destroy()
    }
}