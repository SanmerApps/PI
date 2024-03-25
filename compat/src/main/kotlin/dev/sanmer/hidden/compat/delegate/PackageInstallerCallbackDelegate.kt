package dev.sanmer.hidden.compat.delegate

import android.content.pm.IPackageInstallerCallback
import dev.sanmer.hidden.compat.stub.ISessionCallback

internal class PackageInstallerCallbackDelegate(
    private val mCallback: ISessionCallback
) : IPackageInstallerCallback.Stub() {
    override fun onSessionCreated(sessionId: Int) {
        mCallback.onCreated(sessionId)
    }

    override fun onSessionBadgingChanged(sessionId: Int) {
        mCallback.onBadgingChanged(sessionId)
    }

    override fun onSessionActiveChanged(sessionId: Int, active: Boolean) {
        mCallback.onActiveChanged(sessionId, active)
    }

    override fun onSessionProgressChanged(sessionId: Int, progress: Float) {
        mCallback.onProgressChanged(sessionId, progress)
    }

    override fun onSessionFinished(sessionId: Int, success: Boolean) {
        mCallback.onFinished(sessionId, success)
    }
}