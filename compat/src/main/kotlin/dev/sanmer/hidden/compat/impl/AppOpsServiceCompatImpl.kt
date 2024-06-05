package dev.sanmer.hidden.compat.impl

import android.app.AppOpsManagerHidden
import android.os.IBinder
import android.os.IInterface
import com.android.internal.app.IAppOpsService
import dev.sanmer.hidden.compat.proxy.AppOpsCallbackProxy
import dev.sanmer.hidden.compat.stub.IAppOpsCallback
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat

internal class AppOpsServiceCompatImpl(
    private val original: IAppOpsService
) : IAppOpsServiceCompat.Stub() {
    private val callbacks = mutableMapOf<IBinder, IInterface>()

    override fun checkOperation(code: Int, uid: Int, packageName: String): Int {
        return original.checkOperation(code, uid, packageName)
    }

    override fun getPackagesForOps(ops: IntArray): List<AppOpsManagerHidden.PackageOps> {
        return original.getPackagesForOps(ops) ?: emptyList()
    }

    override fun getOpsForPackage(
        uid: Int,
        packageName: String,
        ops: IntArray?
    ): List<AppOpsManagerHidden.PackageOps> {
        return original.getOpsForPackage(uid, packageName, ops) ?: emptyList()
    }

    override fun getUidOps(uid: Int, ops: IntArray?): List<AppOpsManagerHidden.PackageOps> {
        return original.getUidOps(uid, ops) ?: emptyList()
    }

    override fun setUidMode(code: Int, uid: Int, mode: Int) {
        original.setUidMode(code, uid, mode)
    }

    override fun setMode(code: Int, uid: Int, packageName: String, mode: Int) {
        original.setMode(code, uid, packageName, mode)
    }

    override fun resetAllModes(reqUserId: Int, reqPackageName: String?) {
        original.resetAllModes(reqUserId, reqPackageName)
    }

    override fun startWatchingMode(op: Int, packageName: String?, callback: IAppOpsCallback) {
        val binder = callback.asBinder()
        val proxy = AppOpsCallbackProxy(callback)
        callbacks[binder] = proxy
        original.startWatchingMode(op, packageName, proxy)
    }

    override fun stopWatchingMode(callback: IAppOpsCallback) {
        val binder = callback.asBinder()
        val proxy = callbacks.remove(binder)
        if (proxy is com.android.internal.app.IAppOpsCallback) {
            original.stopWatchingMode(proxy)
        }
    }
}