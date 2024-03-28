package dev.sanmer.hidden.compat.delegate

import dev.sanmer.hidden.compat.stub.ISessionCallback

abstract class SessionCallbackDelegate : ISessionCallback.Stub() {
    override fun onCreated(sessionId: Int) {}

    override fun onBadgingChanged(sessionId: Int) {}

    override fun onActiveChanged(sessionId: Int, active: Boolean) {}

    override fun onProgressChanged(sessionId: Int, progress: Float) {}

    override fun onFinished(sessionId: Int, success: Boolean) {}

}