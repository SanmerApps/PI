package dev.sanmer.hidden.compat

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat
import dev.sanmer.hidden.compat.stub.IProvider
import dev.sanmer.hidden.compat.stub.IServiceManager
import dev.sanmer.hidden.compat.stub.IUserManagerCompat
import dev.sanmer.hidden.compat.su.SuService
import dev.sanmer.hidden.compat.su.SuShellInitializer
import kotlinx.coroutines.flow.MutableStateFlow

object SuProvider : IProvider {
    private const val TAG = "SuProvider"
    private var mServiceOrNull: IServiceManager? = null
    private val mService get() = checkNotNull(mServiceOrNull) {
        "IServiceManager haven't been received"
    }

    override val uid: Int get() = mService.uid
    override val pid: Int get() = mService.pid
    override val version: Int get() = mService.version
    override val seLinuxContext: String get() = mService.seLinuxContext
    override val packageManagerCompat: IPackageManagerCompat get() = mService.packageManagerCompat
    override val userManagerCompat: IUserManagerCompat get() = mService.userManagerCompat

    override val isAlive = MutableStateFlow(false)

    init {
        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setInitializers(SuShellInitializer::class.java)
                .setTimeout(15)
        )
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mServiceOrNull = IServiceManager.Stub.asInterface(service)
            isAlive.value = true
            Log.i(TAG, "IServiceManager created")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceOrNull = null
            isAlive.value = false
            Log.w(TAG, "IServiceManager destroyed")
        }

    }

    override fun init() {
        RootService.bind(SuService.intent, connection)
    }

    override fun destroy() {
        RootService.stop(SuService.intent)
    }

}