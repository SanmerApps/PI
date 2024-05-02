package dev.sanmer.hidden.compat.impl

import android.app.AppOpsManagerHidden
import com.android.internal.app.IAppOpsService
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat

internal class AppOpsServiceCompatImpl(
    private val original: IAppOpsService
) : IAppOpsServiceCompat.Stub() {
    override fun checkOperation(code: Int, uid: Int, packageName: String): Int {
        return original.checkOperation(code, uid, packageName)
    }

    override fun getPackagesForOps(ops: IntArray?): List<AppOpsManagerHidden.PackageOps> {
        return original.getPackagesForOps(ops)
    }

    override fun getOpsForPackage(
        uid: Int,
        packageName: String,
        ops: IntArray?
    ): List<AppOpsManagerHidden.PackageOps> {
        return original.getOpsForPackage(uid, packageName, ops)
    }

    override fun getUidOps(uid: Int, ops: IntArray?): List<AppOpsManagerHidden.PackageOps> {
        return original.getUidOps(uid, ops)
    }

    override fun setUidMode(code: Int, uid: Int, mode: Int) {
        original.setUidMode(code, uid, mode)
    }

    override fun setMode(code: Int, uid: Int, packageName: String, mode: Int) {
        original.setMode(code, uid, packageName, mode)
    }

    override fun resetAllModes(reqUserId: Int, reqPackageName: String) {
        original.resetAllModes(reqUserId, reqPackageName)
    }
}