package dev.sanmer.hidden.compat.delegate

import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.VersionedPackage
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.IntentReceiverCompat
import dev.sanmer.hidden.compat.proxy.PackageInstallerSessionProxy
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerSessionCompat
import dev.sanmer.hidden.compat.stub.ISessionCallback
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class PackageInstallerDelegate(
    private val packageInstaller: IPackageInstallerCompat
) {
    private val context = ContextDelegate.getContext()
    private var installerPackageName = context.packageName
    private var installerAttributionTag = context.packageName

    private val delegates = mutableListOf<SessionCallbackDelegate>()

    fun setInstallerPackageName(packageName: String) {
        installerPackageName = packageName
        installerAttributionTag = packageName
    }

    fun createSession(params: PackageInstaller.SessionParams): Int {
        return packageInstaller.createSession(
            params,
            installerPackageName,
            installerAttributionTag,
            context.userId
        )
    }

    fun openSession(sessionId: Int): Session {
        return Session(
            session = packageInstaller.openSession(sessionId)
        )
    }

    fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo? {
        return packageInstaller.getSessionInfo(sessionId)
    }

    fun getAllSessions(): List<PackageInstaller.SessionInfo> {
        return packageInstaller.getAllSessions(context.userId).list
    }

    fun getMySessions(): List<PackageInstaller.SessionInfo> {
        val sessions = getAllSessions()
        return if (BuildCompat.atLeastS) {
            sessions.filter {
                it.installerPackageName == installerPackageName
                        && it.installerAttributionTag == installerAttributionTag
            }
        } else {
            sessions.filter {
                it.installerPackageName == installerPackageName
            }
        }
    }

    fun registerCallback(callback: SessionCallback) {
        val delegate = SessionCallbackDelegate(callback)
        packageInstaller.registerCallback(delegate, context.userId)
        delegates.add(delegate)
    }

    fun unregisterCallback(callback: SessionCallback) {
        val delegate = delegates.find { it.callback == callback }
        if (delegate != null) {
            packageInstaller.unregisterCallback(delegate)
        }
    }

    suspend fun uninstall(packageName: String) = IntentReceiverCompat.build { sender ->
        packageInstaller.uninstall(
            VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST),
            installerPackageName,
            0,
            sender,
            context.userId
        )
    }

    interface SessionCallback {
        fun onCreated(sessionId: Int) {}

        fun onBadgingChanged(sessionId: Int) {}

        fun onActiveChanged(sessionId: Int, active: Boolean) {}

        fun onProgressChanged(sessionId: Int, progress: Float) {}

        fun onFinished(sessionId: Int, success: Boolean) {}
    }

    internal class SessionCallbackDelegate(
        val callback: SessionCallback
    ) : ISessionCallback.Stub() {
        override fun onCreated(sessionId: Int) {
            callback.onCreated(sessionId)
        }

        override fun onBadgingChanged(sessionId: Int) {
            callback.onBadgingChanged(sessionId)
        }

        override fun onActiveChanged(sessionId: Int, active: Boolean) {
            callback.onActiveChanged(sessionId, active)
        }

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            callback.onProgressChanged(sessionId, progress)
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            callback.onFinished(sessionId, success)
        }
    }

    class SessionParams(
        mode: Int
    ): PackageInstaller.SessionParams(mode) {
        private val original by lazy {
            Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(this)
        }

        var installFlags: Int
            get() = original.installFlags
            set(flags) {
                original.installFlags = installFlags or flags
            }

        companion object {
            val INSTALL_REPLACE_EXISTING get() = PackageManagerHidden.INSTALL_REPLACE_EXISTING

            val INSTALL_ALLOW_TEST get() = PackageManagerHidden.INSTALL_ALLOW_TEST

            val INSTALL_REQUEST_DOWNGRADE get() = PackageManagerHidden.INSTALL_REQUEST_DOWNGRADE

            @get:RequiresApi(34)
            val INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK get() = PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
        }
    }

    class Session(
        private val session: IPackageInstallerSessionCompat
    ) {
        private val proxy by lazy {
            PackageInstallerSessionProxy(session)
        }

        private val original: PackageInstaller.Session by lazy {
            Refine.unsafeCast(
                PackageInstallerHidden.SessionHidden(proxy)
            )
        }

        fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream {
            return original.openWrite(name, offsetBytes, lengthBytes)
        }

        fun fsync(out: OutputStream) {
            original.fsync(out)
        }

        fun write(name: String, offsetBytes: Long, lengthBytes: Long, fd: ParcelFileDescriptor) {
            session.write(name, offsetBytes, lengthBytes, fd)
        }

        fun openRead(name: String): InputStream {
            return original.openRead(name)
        }

        fun close() {
            original.close()
        }

        suspend fun commit() = IntentReceiverCompat.build { sender ->
            original.commit(sender)
        }

        fun abandon() {
            original.abandon()
        }

        fun writeApk(path: File) {
            openWrite(path.name, 0, path.length()).use { output ->
                path.inputStream().buffered().use { input ->
                    input.copyTo(output)
                    fsync(output)
                }
            }
        }

        fun writeApks(path: File, filenames: List<String>) {
            filenames.forEach { name ->
                val file = File(path, name)
                if (file.exists()) writeApk(file)
            }
        }
    }
}