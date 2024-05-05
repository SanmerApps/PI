package dev.sanmer.hidden.compat.impl

import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerCallback
import android.content.pm.PackageInstaller
import android.content.pm.ParceledListSlice
import android.content.pm.VersionedPackage
import android.os.IBinder
import android.os.IInterface
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.delegate.PackageInstallerCallbackDelegate
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerSessionCompat
import dev.sanmer.hidden.compat.stub.ISessionCallback

internal class PackageInstallerCompatImpl(
    private val original: IPackageInstaller
) : IPackageInstallerCompat.Stub() {
    private val mCallbacks = mutableMapOf<IBinder, IInterface>()

    override fun createSession(
        params: PackageInstaller.SessionParams,
        installerPackageName: String,
        installerAttributionTag: String,
        userId: Int
    ): Int {
        return if (BuildCompat.atLeastS) {
            original.createSession(params, installerPackageName, installerAttributionTag, userId)
        } else {
            original.createSession(params, installerPackageName, userId)
        }
    }

    override fun openSession(sessionId: Int): IPackageInstallerSessionCompat {
        return PackageInstallerSessionCompatImpl(
            sessionId = sessionId,
            original = original.openSession(sessionId),
            installer = original
        )
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

    override fun uninstall(
        versionedPackage: VersionedPackage,
        callerPackageName: String,
        flags: Int,
        statusReceiver: IntentSender,
        userId: Int
    ) {
        original.uninstall(versionedPackage, callerPackageName, flags, statusReceiver, userId)
    }
}