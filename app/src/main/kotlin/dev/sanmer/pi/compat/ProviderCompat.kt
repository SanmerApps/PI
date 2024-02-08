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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object ProviderCompat {
    private var mMode = Provider.None
    private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mProvider: IProvider

    var isAlive by mutableStateOf(false)
        private set

    val packageManagerCompat get() = mProvider.packageManagerCompat
    val userManagerCompat get() = mProvider.userManagerCompat

    val version get() = mProvider.version
    val platform get() = when (mProvider.uid) {
        Process.ROOT_UID -> "root"
        Process.SHELL_UID -> "adb"
        else -> "unknown"
    }

    fun init(mode: Provider? = null) {
        mMode = mode ?: mMode

        when (mMode) {
            Provider.Shizuku -> {
                ShizukuProvider.apply {
                    mProvider = this
                    init()
                }

                ShizukuProvider.isAlive
                    .onEach { isAlive = it }
                    .launchIn(mScope)
            }
            Provider.Superuser -> {
                SuProvider.apply {
                    mProvider = this
                    init()
                }

                SuProvider.isAlive
                    .onEach { isAlive = it }
                    .launchIn(mScope)
            }
            else -> {}
        }
    }

    fun destroy() = when (mMode) {
        Provider.Shizuku -> {
            isAlive = false // No wait for provider
            ShizukuProvider.destroy()
        }
        Provider.Superuser -> {
            isAlive = false
            SuProvider.destroy()
        }
        else -> {}
    }
}