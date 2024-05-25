package dev.sanmer.hidden.compat.proxy

import android.content.pm.IPackageInstallerCallback
import dev.sanmer.hidden.compat.stub.ISessionCallback

internal class PackageInstallerCallbackProxy(
    private val callback: ISessionCallback
) : IPackageInstallerCallback.Stub() {
    override fun onSessionCreated(sessionId: Int) {
        callback.onCreated(sessionId)
    }

    override fun onSessionBadgingChanged(sessionId: Int) {
        callback.onBadgingChanged(sessionId)
    }

    override fun onSessionActiveChanged(sessionId: Int, active: Boolean) {
        callback.onActiveChanged(sessionId, active)
    }

    override fun onSessionProgressChanged(sessionId: Int, progress: Float) {
        callback.onProgressChanged(sessionId, progress)
    }

    override fun onSessionFinished(sessionId: Int, success: Boolean) {
        callback.onFinished(sessionId, success)
    }
}