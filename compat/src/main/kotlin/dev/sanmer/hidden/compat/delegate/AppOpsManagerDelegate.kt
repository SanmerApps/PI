package dev.sanmer.hidden.compat.delegate

import android.app.AppOpsManager
import android.app.AppOpsManagerHidden
import android.content.pm.PackageInfo
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.hidden.compat.stub.IAppOpsCallback
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat

class AppOpsManagerDelegate(
    private val appOpsService: IAppOpsServiceCompat
) {
    private val delegates = mutableListOf<AppOpsActiveCallbackDelegate>()

    fun checkOpNoThrow(op: Int, uid: Int, packageName: String): Mode {
        return Mode.from(
            appOpsService.checkOperation(op, uid, packageName)
        )
    }

    fun checkOpNoThrow(op: Int, packageInfo: PackageInfo): Mode {
        return checkOpNoThrow(
            op = op,
            uid = packageInfo.applicationInfo.uid,
            packageName = packageInfo.packageName
        )
    }

    fun getPackagesForOps(ops: IntArray): List<PackageOps> {
        return appOpsService.getPackagesForOps(ops).map(::PackageOps)
    }

    fun getPackagesForOp(op: Int): List<PackageOps> {
        return getPackagesForOps(intArrayOf(op))
    }

    fun getOpsForPackage(uid: Int, packageName: String): List<OpEntry> {
        return appOpsService.getOpsForPackage(uid, packageName, null)
            .firstOrNull()?.ops?.map(::OpEntry) ?: emptyList()
    }

    fun getOpsForPackage(packageInfo: PackageInfo): List<OpEntry> {
        return getOpsForPackage(
            uid = packageInfo.applicationInfo.uid,
            packageName = packageInfo.packageName
        )
    }

    fun getUidOps(uid: Int): List<PackageOps> {
        return appOpsService.getUidOps(uid, null).map(::PackageOps)
    }

    fun setUidMode(op: Int, uid: Int, mode: Mode) {
        appOpsService.setUidMode(op, uid, mode.code)
    }

    fun setMode(op: Int, uid: Int, packageName: String, mode: Mode) {
        appOpsService.setMode(op, uid, packageName, mode.code)
    }

    fun setMode(op: Int, packageInfo: PackageInfo, mode: Mode) {
        setMode(
            op = op,
            uid = packageInfo.applicationInfo.uid,
            packageName = packageInfo.packageName,
            mode = mode
        )
    }

    fun resetAllModes() {
        val userId = UserHandleCompat.myUserId()
        appOpsService.resetAllModes(userId, null)
    }

    fun startWatchingMode(op: Int, packageName: String?, callback: AppOpsCallback) {
        val delegate = AppOpsActiveCallbackDelegate(callback)
        appOpsService.startWatchingMode(op, packageName, delegate)
        delegates.add(delegate)
    }

    fun stopWatchingMode(callback: AppOpsCallback) {
        val delegate = delegates.find { it.callback == callback }
        if (delegate != null) {
            appOpsService.stopWatchingMode(delegate)
        }
    }

    interface AppOpsCallback {
        fun opChanged(op: Int, uid: Int, packageName: String) {}
    }

    internal class AppOpsActiveCallbackDelegate(
        val callback: AppOpsCallback
    ) : IAppOpsCallback.Stub() {
        override fun opChanged(op: Int, uid: Int, packageName: String) {
            callback.opChanged(op, uid, packageName)
        }

    }

    enum class Mode(val code: Int) {
        Allow(MODE_ALLOWED),
        Deny(MODE_DENY),
        Ignore(MODE_IGNORED),
        Default(MODE_DEFAULT),
        Foreground(MODE_FOREGROUND);

        companion object {
            fun from(code: Int) = entries.first { it.code == code }
            fun fromOrNull(code: Int) = entries.firstOrNull { it.code == code }

            fun Mode.isAllowed() = this == Allow
            fun Mode.isDenied() = this == Deny
            fun Mode.isIgnored() = this == Ignore
            fun Mode.isDefaulted() = this == Default
            fun Mode.isForegrounded() = this == Foreground
        }
    }

    class OpEntry internal constructor(
        private val original: AppOpsManagerHidden.OpEntry
    ) {
        val op = original.op
        val opStr = original.opStr
        val mode = original.mode
        val time = original.time
    }

    class PackageOps internal constructor(
        private val original: AppOpsManagerHidden.PackageOps
    ) {
        val packageName = original.packageName
        val uid = original.uid
        val ops by lazy { original.ops.map { OpEntry(it) } }
    }

    companion object {
        val MODE_ALLOWED get() = AppOpsManager.MODE_ALLOWED

        val MODE_IGNORED get() = AppOpsManager.MODE_IGNORED

        val MODE_DENY get() = AppOpsManager.MODE_ERRORED

        val MODE_DEFAULT get() = AppOpsManager.MODE_DEFAULT

        val MODE_FOREGROUND get() = AppOpsManager.MODE_FOREGROUND

        val OP_NONE get() = AppOpsManagerHidden.OP_NONE

        val OP_VIBRATE get() = AppOpsManagerHidden.OP_VIBRATE

        val OP_REQUEST_INSTALL_PACKAGES get() = AppOpsManagerHidden.OP_REQUEST_INSTALL_PACKAGES

        val OP_REQUEST_DELETE_PACKAGES get() = AppOpsManagerHidden.OP_REQUEST_DELETE_PACKAGES

        fun opToName(op: Int): String {
            return AppOpsManagerHidden.opToName(op)
        }

        fun opToPermission(op: Int): String {
            return AppOpsManagerHidden.opToPermission(op)
        }
    }
}