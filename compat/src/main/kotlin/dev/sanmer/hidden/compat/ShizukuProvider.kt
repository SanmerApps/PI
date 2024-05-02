package dev.sanmer.hidden.compat

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import dev.sanmer.hidden.compat.shizuku.ShizukuService
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat
import dev.sanmer.hidden.compat.stub.IProvider
import dev.sanmer.hidden.compat.stub.IServiceManager
import dev.sanmer.hidden.compat.stub.IUserManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import rikka.shizuku.Shizuku

object ShizukuProvider : IProvider, ServiceConnection, Shizuku.OnRequestPermissionResultListener {
    private const val TAG = "ShizukuProvider"
    private var mServiceOrNull: IServiceManager? = null
    private val mService get() = checkNotNull(mServiceOrNull) {
        "IServiceManager haven't been received"
    }

    override val uid: Int get() = mService.uid
    override val pid: Int get() = mService.pid
    override val version: Int get() = mService.version
    override val seLinuxContext: String get() = mService.seLinuxContext
    override val appOpsService: IAppOpsServiceCompat get() = mService.appOpsService
    override val packageManager: IPackageManagerCompat get() = mService.packageManager
    override val userManager: IUserManagerCompat get() = mService.userManager

    override val isAlive = MutableStateFlow(false)

    private var isGranted = false
    private val isBinderAlive get() = Shizuku.pingBinder()

    init {
        if (isBinderAlive) {
            isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        mServiceOrNull = IServiceManager.Stub.asInterface(service)
        isAlive.value = true

        Log.i(TAG, "IServiceManager created")
        Log.d(TAG, "uid = $uid, pid = $pid, context = $seLinuxContext")
    }

    override fun onServiceDisconnected(name: ComponentName) {
        isAlive.value = false
        mServiceOrNull = null

        Log.w(TAG, "IServiceManager destroyed")
    }

    override fun init() {
        if (!isBinderAlive) return

        if (isGranted) {
            Shizuku.bindUserService(ShizukuService(), this)
        } else {
            Shizuku.addRequestPermissionResultListener(this)
            Shizuku.requestPermission(0)
        }
    }

    override fun destroy() {
        if (!isBinderAlive) return

        Shizuku.unbindUserService(ShizukuService(), this, true)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        isGranted = grantResult == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            Shizuku.removeRequestPermissionResultListener(this)
            init()
        }
    }
}