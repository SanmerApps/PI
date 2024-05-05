package dev.sanmer.hidden.compat.delegate

import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSenderHidden
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.VersionedPackage
import android.graphics.Bitmap
import android.os.Bundle
import android.os.FileBridge
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.system.Os
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerSessionCompat
import dev.sanmer.hidden.compat.stub.ISessionCallback
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class PackageInstallerDelegate(
    private val installer: IPackageInstallerCompat,
    private val userId: Int,
    private var installerPackageName: String,
    private var installerAttributionTag: String,
) {
    constructor(
        installer: IPackageInstallerCompat,
        userId: Int
    ) : this(
        installer = installer,
        installerPackageName = DEFAULT_INSTALLER,
        installerAttributionTag = DEFAULT_INSTALLER,
        userId = userId
    )

    constructor(
        installer: IPackageInstallerCompat
    ) : this(
        installer = installer,
        installerPackageName = DEFAULT_INSTALLER,
        installerAttributionTag = DEFAULT_INSTALLER,
        userId = UserHandleCompat.myUserId()
    )

    private val mDelegates = mutableListOf<SessionCallbackDelegate>()

    fun setInstallerPackageName(packageName: String) {
        installerPackageName = packageName
        installerAttributionTag = packageName
    }

    fun createSession(params: PackageInstaller.SessionParams): Int {
        return installer.createSession(
            params,
            installerPackageName,
            installerAttributionTag,
            userId
        )
    }

    fun openSession(sessionId: Int): Session {
        return Session(
            session = installer.openSession(sessionId)
        )
    }

    fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo? {
        return installer.getSessionInfo(sessionId)
    }


    fun getAllSessions(): List<PackageInstaller.SessionInfo> {
        return installer.getAllSessions(userId).list
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
        installer.registerCallback(delegate, userId)
        mDelegates.add(delegate)
    }

    fun unregisterCallback(callback: SessionCallback) {
        val delegate = mDelegates.find { it.mCallback == callback }
        if (delegate != null) {
            installer.unregisterCallback(delegate)
        }
    }

    fun uninstall(packageName: String): Intent {
        val receiver = LocalIntentReceiver()
        installer.uninstall(
            VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST),
            installerPackageName,
            0,
            receiver.intentSender,
            userId
        )

        return receiver.result
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

        fun commit(): Intent {
            val receiver = LocalIntentReceiver()
            session.commit(receiver.intentSender, false)

            return receiver.result
        }

        fun abandon() {
            session.abandon()
        }

        fun updateAppIcon(appIcon: Bitmap) {
            session.updateAppIcon(appIcon)
        }

        fun updateAppLabel(appLabel: String) {
            session.updateAppLabel(appLabel)
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

    internal class LocalIntentReceiver {
        private val mResult = LinkedBlockingQueue<Intent>()
        private val mLocalSender: IIntentSender.Stub = object : IIntentSender.Stub() {
            override fun send(
                code: Int,
                intent: Intent,
                resolvedType: String?,
                whitelistToken: IBinder?,
                finishedReceiver: IIntentReceiver?,
                requiredPermission: String?,
                options: Bundle?
            ) {
                try {
                    mResult.offer(intent, 5, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
        }

        val intentSender: IntentSender get() =
            Refine.unsafeCast(IntentSenderHidden(mLocalSender))

        val result: Intent get() =
            try {
                mResult.take()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
    }

    companion object {
        const val DEFAULT_INSTALLER = "com.android.shell"
    }
}