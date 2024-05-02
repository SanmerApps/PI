package dev.sanmer.hidden.compat.delegate

import android.app.AppOpsManager
import android.app.AppOpsManagerHidden
import android.os.RemoteException
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat

class AppOpsManagerDelegate(
    private val mService: IAppOpsServiceCompat
) {

    fun checkOpNoThrow(op: String, uid: Int, packageName: String): Int {
        return checkOpNoThrow(strOpToOp(op), uid, packageName)
    }

    fun checkOpNoThrow(op: Int, uid: Int, packageName: String): Int {
        return try {
            val mode = mService.checkOperation(op, uid, packageName)
            if (mode == AppOpsManager.MODE_FOREGROUND) AppOpsManager.MODE_ALLOWED else mode
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun getPackagesForOps(ops: IntArray?): List<PackageOps> {
        return try {
            mService.getPackagesForOps(ops).map { PackageOps(it) }
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun getOpsForPackage(uid: Int, packageName: String, ops: IntArray?): List<PackageOps> {
        return try {
            mService.getOpsForPackage(uid, packageName, ops).map { PackageOps(it) }
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun getUidOps(uid: Int, ops: IntArray?): List<PackageOps> {
        return try {
            mService.getUidOps(uid, ops).map { PackageOps(it) }
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun setUidMode(appOp: String, uid: Int, mode: Int) {
        try {
            mService.setUidMode(strOpToOp(appOp), uid, mode)
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun setUidMode(code: Int, uid: Int, mode: Int) {
        try {
            mService.setUidMode(code, uid, mode)
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun setMode(appOp: String, uid: Int, packageName: String, mode: Int) {
        try {
            mService.setMode(strOpToOp(appOp), uid, packageName, mode)
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun setMode(code: Int, uid: Int, packageName: String, mode: Int) {
        try {
            mService.setMode(code, uid, packageName, mode)
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
        }
    }

    fun resetAllModes() {
        try {
            mService.resetAllModes(UserHandleCompat.myUserId(), null)
        } catch (e: RemoteException) {
            throw e.rethrowFromSystemServer()
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
        fun strOpToOp(op: String): Int {
            return AppOpsManagerHidden.strOpToOp(op)
        }

        fun opToPermission(op: Int): String {
            return AppOpsManagerHidden.opToPermission(op)
        }

        fun opToPermission(op: String): String? {
            return AppOpsManagerHidden.opToPermission(op)
        }
    }
}