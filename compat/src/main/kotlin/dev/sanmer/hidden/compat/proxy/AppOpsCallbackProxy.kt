package dev.sanmer.hidden.compat.proxy

import com.android.internal.app.IAppOpsCallback

internal class AppOpsCallbackProxy(
    private val callback: dev.sanmer.hidden.compat.stub.IAppOpsCallback
) : IAppOpsCallback.Stub() {
    override fun opChanged(op: Int, uid: Int, packageName: String) {
        callback.opChanged(op, uid, packageName)
    }
}