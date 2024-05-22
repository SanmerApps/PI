package dev.sanmer.hidden.compat.delegate

import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.VersionedPackage
import android.os.FileBridge
import android.os.ParcelFileDescriptor
import android.system.Os
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.IntentReceiverCompat
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerSessionCompat
import dev.sanmer.hidden.compat.stub.ISessionCallback
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class PackageInstallerDelegate(
    private val get: () -> IPackageInstallerCompat
) {
    private val userId = UserHandleCompat.myUserId()
    private var installerPackageName = DEFAULT_INSTALLER
    private var installerAttributionTag = DEFAULT_INSTALLER

    private val mDelegates = mutableListOf<SessionCallbackDelegate>()

    fun setInstallerPackageName(packageName: String) {
        installerPackageName = packageName
        installerAttributionTag = packageName
    }

    fun createSession(params: PackageInstaller.SessionParams): Int {
        return get().createSession(
            params,
            installerPackageName,
            installerAttributionTag,
            userId
        )
    }

    fun openSession(sessionId: Int): Session {
        return Session(
            session = get().openSession(sessionId)
        )
    }

    fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo? {
        return get().getSessionInfo(sessionId)
    }

    fun getAllSessions(): List<PackageInstaller.SessionInfo> {
        return get().getAllSessions(userId).list
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
        get().registerCallback(delegate, userId)
        mDelegates.add(delegate)
    }

    fun unregisterCallback(callback: SessionCallback) {
        val delegate = mDelegates.find { it.mCallback == callback }
        if (delegate != null) {
            get().unregisterCallback(delegate)
        }
    }

    suspend fun uninstall(packageName: String) = IntentReceiverCompat.build { sender ->
        get().uninstall(
            VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST),
            installerPackageName,
            0,
            sender,
            userId
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
        val mCallback: SessionCallback
    ) : ISessionCallback.Stub() {
        override fun onCreated(sessionId: Int) {
            mCallback.onCreated(sessionId)
        }

        override fun onBadgingChanged(sessionId: Int) {
            mCallback.onBadgingChanged(sessionId)
        }

        override fun onActiveChanged(sessionId: Int, active: Boolean) {
            mCallback.onActiveChanged(sessionId, active)
        }

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            mCallback.onProgressChanged(sessionId, progress)
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            mCallback.onFinished(sessionId, success)
        }

    }

    class SessionParams(
        private val mode: Int
    ): PackageInstaller.SessionParams(mode) {
        var installFlags: Int
            get() = Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(this).installFlags
            set(flags: Int) = with(Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(this)) {
                installFlags = installFlags or flags
            }

        companion object {
            val INSTALL_REPLACE_EXISTING get() = PackageManagerHidden.INSTALL_REPLACE_EXISTING

            val INSTALL_ALLOW_TEST get() = PackageManagerHidden.INSTALL_ALLOW_TEST

            val INSTALL_REQUEST_DOWNGRADE  get() = PackageManagerHidden.INSTALL_REQUEST_DOWNGRADE

            @get:RequiresApi(34)
            val INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK get() = PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
        }
    }

    class Session(
        private val session: IPackageInstallerSessionCompat
    ) {
        fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream {
            return if (PackageInstallerHidden.ENABLE_REVOCABLE_FD) {
                ParcelFileDescriptor.AutoCloseOutputStream(
                    session.openWrite(name, offsetBytes, lengthBytes)
                )
            } else {
                FileBridge.FileBridgeOutputStream(
                    session.openWrite(name, offsetBytes, lengthBytes)
                )
            }
        }

        fun fsync(out: OutputStream) {
            if (PackageInstallerHidden.ENABLE_REVOCABLE_FD) {
                if (out is ParcelFileDescriptor.AutoCloseOutputStream) {
                    Os.fsync(out.fd)
                } else {
                    throw IllegalArgumentException("Unrecognized stream")
                }
            } else {
                if (out is FileBridge.FileBridgeOutputStream) {
                    out.fsync()
                } else {
                    throw IllegalArgumentException("Unrecognized stream")
                }
            }
        }

        fun write(name: String, offsetBytes: Long, lengthBytes: Long, fd: ParcelFileDescriptor) {
            session.write(name, offsetBytes, lengthBytes, fd);
        }

        fun openRead(name: String): InputStream {
            val pfd: ParcelFileDescriptor = session.openRead(name)
            return ParcelFileDescriptor.AutoCloseInputStream(pfd)
        }

        fun close() {
            session.close()
        }

        suspend fun commit() = IntentReceiverCompat.build { sender ->
            session.commit(sender, false)
        }

        fun abandon() {
            session.abandon()
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
                if (file.exists() && file.length() != 0L) {
                    writeApk(file)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_INSTALLER = "com.android.shell"
    }
}