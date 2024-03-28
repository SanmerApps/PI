package dev.sanmer.hidden.compat.impl

import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerCallback
import android.content.pm.PackageInstaller
import android.content.pm.ParceledListSlice
import android.graphics.Bitmap
import android.os.IBinder
import android.os.IInterface
import android.os.ParcelFileDescriptor
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.delegate.PackageInstallerCallbackDelegate
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.ISessionCallback

internal class PackageInstallerCompatImpl(
    private val original: IPackageInstaller
) : IPackageInstallerCompat.Stub() {
    private val mCallbacks = mutableMapOf<IBinder, IInterface>()

    override fun createSession(
        params: PackageInstaller.SessionParams,
        installerPackageName: String,
        installerAttributionTag: String?,
        userId: Int
    ): Int {
        return if (BuildCompat.atLeastS) {
            original.createSession(params, installerPackageName, installerAttributionTag, userId)
        } else {
            original.createSession(params, installerPackageName, userId)
        }
    }

    override fun updateSessionAppIcon(sessionId: Int, appIcon: Bitmap?) {
        original.updateSessionAppIcon(sessionId, appIcon)
    }

    override fun updateSessionAppLabel(sessionId: Int, appLabel: String?) {
        original.updateSessionAppLabel(sessionId, appLabel)
    }

    override fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo {
        return original.getSessionInfo(sessionId)
    }

    override fun getAllSessions(userId: Int): ParceledListSlice<PackageInstaller.SessionInfo> {
        val sessions = original.getAllSessions(userId).list
        return ParceledListSlice(sessions)
    }

    override fun registerCallback(callback: ISessionCallback, userId: Int) {
        val binder = callback.asBinder()
        val delegate = PackageInstallerCallbackDelegate(callback)
        mCallbacks[binder] = delegate
        original.registerCallback(delegate, userId)
    }

    override fun unregisterCallback(callback: ISessionCallback) {
        val binder = callback.asBinder()
        val delegate = mCallbacks.remove(binder)
        if (delegate is IPackageInstallerCallback) {
            original.unregisterCallback(delegate)
        }
    }

    override fun openWrite(
        sessionId: Int,
        name: String,
        offsetBytes: Long,
        lengthBytes: Long
    ): ParcelFileDescriptor {
        val session = original.openSession(sessionId)
        return session.openWrite(name, offsetBytes, lengthBytes)
    }

    override fun openRead(sessionId: Int, name: String): ParcelFileDescriptor {
        val session = original.openSession(sessionId)
        return session.openRead(name)
    }

    override fun write(
        sessionId: Int,
        name: String,
        offsetBytes: Long,
        lengthBytes: Long,
        fd: ParcelFileDescriptor
    ) {
        val session = original.openSession(sessionId)
        session.write(name, offsetBytes, lengthBytes, fd)
    }

    override fun close(sessionId: Int) {
        val session = original.openSession(sessionId)
        session.close()
    }

    override fun commit(sessionId: Int, statusReceiver: IntentSender, forTransferred: Boolean) {
        val session = original.openSession(sessionId)
        session.commit(statusReceiver, forTransferred)
    }
}