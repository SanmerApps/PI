package dev.sanmer.hidden.compat.delegate

import android.app.AppOpsManager
import android.app.AppOpsManagerHidden
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat
import kotlinx.coroutines.flow.MutableStateFlow

class AppOpsManagerDelegate(
    private val service: IAppOpsServiceCompat
) {
    fun checkOpNoThrow(op: Int, uid: Int, packageName: String): Int {
        return service.checkOperation(op, uid, packageName)
    }

    fun getPackagesForOps(ops: IntArray?): List<PackageOps> {
        return service.getPackagesForOps(ops).map { PackageOps(it) }
    }

    fun getOpsForPackage(uid: Int, packageName: String, ops: IntArray?): List<PackageOps> {
        return service.getOpsForPackage(uid, packageName, ops).map { PackageOps(it) }
    }

    fun getUidOps(uid: Int, ops: IntArray?): List<PackageOps> {
        return service.getUidOps(uid, ops).map { PackageOps(it) }
    }

    fun setUidMode(op: Int, uid: Int, mode: Int) {
        service.setUidMode(op, uid, mode)
    }

    fun setMode(op: Int, uid: Int, packageName: String, mode: Int) {
        service.setMode(op, uid, packageName, mode)
    }

    fun resetAllModes() {
        service.resetAllModes(UserHandleCompat.myUserId(), null)
    }

    enum class Mode(val code: Int) {
        Allow(MODE_ALLOWED),
        Deny(MODE_DENY),
        Ignore(MODE_IGNORED),
        Default(MODE_DEFAULT),
        Foreground(MODE_FOREGROUND);

        companion object {
            fun Mode.isAllowed() = this == Allow
            fun Mode.isDenied() = this == Deny
            fun Mode.isIgnored() = this == Ignore
            fun Mode.isDefaulted() = this == Default
            fun Mode.isForegrounded() = this == Foreground
        }
    }

    interface Ops {
        val op: Int
        val name: String
        val mode: Mode
        val modeFlow: MutableStateFlow<Mode>

        fun allow()
        fun deny()
        fun ignore()
        fun default()
        fun foreground()
    }

    fun opPermission(
        op: Int,
        uid: Int,
        packageName: String,
    ) = object : Ops {
        override val op = op
        override val name = opToName(op)

        override val mode: Mode get() {
            val code = checkOpNoThrow(op, uid, packageName)
            return Mode.entries.find { it.code == code } ?: Mode.Default
        }

        override val modeFlow = MutableStateFlow(mode)

        override fun allow() = setMode(Mode.Allow.code)

        override fun deny() = setMode(Mode.Deny.code)

        override fun ignore() = setMode(Mode.Ignore.code)

        override fun default() = setMode(Mode.Default.code)

        override fun foreground() = setMode(Mode.Foreground.code)

        private fun setMode(mode: Int) {
            setMode(op, uid, packageName, mode)
            modeFlow.value = this.mode
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