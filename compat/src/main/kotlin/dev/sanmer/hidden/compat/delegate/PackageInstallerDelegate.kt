package dev.sanmer.hidden.compat.delegate

import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSenderHidden
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManagerHidden
import android.graphics.Bitmap
import android.os.Bundle
import android.os.FileBridge
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.UserHandleHidden
import android.system.Os
import dev.rikka.tools.refine.Refine
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat
import dev.sanmer.hidden.compat.stub.IPackageInstallerSessionCompat
import dev.sanmer.hidden.compat.stub.ISessionCallback
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class PackageInstallerDelegate(
    private val installer: IPackageInstallerCompat,
    private val userId: Int,
    var installerPackageName: String,
    var installerAttributionTag: String,
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
        userId = UserHandleHidden.myUserId()
    )

    fun createSession(params: PackageInstaller.SessionParams): Int {
        return installer.createSession(
            params,
            installerPackageName,
            installerAttributionTag,
            userId
        )
    }

    fun openSession(sessionId: Int): SessionDelegate {
        return SessionDelegate(
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

    fun registerCallback(callback: ISessionCallback) {
        installer.registerCallback(callback, userId)
    }

    fun unregisterCallback(callback: ISessionCallback) {
        installer.unregisterCallback(callback)
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

    class SessionDelegate(
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
    }

    companion object {
        const val DEFAULT_INSTALLER = "com.android.shell"

        fun createSessionParams(): PackageInstaller.SessionParams {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

            var flags = Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags
            flags = flags or PackageManagerHidden.INSTALL_ALLOW_TEST or
                    PackageManagerHidden.INSTALL_REPLACE_EXISTING or
                    PackageManagerHidden.INSTALL_REQUEST_DOWNGRADE

            if (BuildCompat.atLeastU) {
                flags = flags or PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
            }

            Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags = flags
            return params
        }
    }
}