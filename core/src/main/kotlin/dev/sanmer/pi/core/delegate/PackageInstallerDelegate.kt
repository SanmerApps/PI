package dev.sanmer.pi.core.delegate

import android.annotation.SuppressLint
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerCallback
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.VersionedPackage
import android.content.res.AssetFileDescriptor
import android.os.IBinder
import android.os.ServiceManager
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine
import dev.sanmer.pi.core.compat.BuildCompat
import dev.sanmer.pi.core.compat.IntentReceiverCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.InputStream
import java.io.OutputStream

class PackageInstallerDelegate(
    private val proxy: IBinder.() -> IBinder = { this }
) {
    private val delegates = mutableListOf<SessionCallbackDelegate>()

    private val packageManager by lazy {
        IPackageManager.Stub.asInterface(
            ServiceManager.getService("package").proxy()
        )
    }

    private val packageInstaller by lazy {
        IPackageInstaller.Stub.asInterface(
            packageManager.packageInstaller.asBinder().proxy()
        )
    }

    fun createSession(
        params: PackageInstaller.SessionParams,
        installerPackageName: String,
        userId: Int
    ): Int {
        return if (BuildCompat.atLeastS) {
            packageInstaller.createSession(
                params,
                installerPackageName,
                installerPackageName,
                userId
            )
        } else {
            packageInstaller.createSession(
                params,
                installerPackageName,
                userId
            )
        }
    }

    fun openSession(sessionId: Int): PackageInstaller.Session {
        val session = IPackageInstallerSession.Stub.asInterface(
            packageInstaller.openSession(sessionId).asBinder().proxy()
        )

        return Refine.unsafeCast(
            PackageInstallerHidden.SessionHidden(session)
        )
    }

    fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo? {
        return packageInstaller.getSessionInfo(sessionId)
    }

    fun getAllSessions(userId: Int): List<PackageInstaller.SessionInfo> {
        return packageInstaller.getAllSessions(userId).list
    }

    fun registerCallback(callback: SessionCallback, userId: Int) {
        val delegate = SessionCallbackDelegate(callback)
        packageInstaller.registerCallback(delegate, userId)
        delegates.add(delegate)
    }

    fun unregisterCallback(callback: SessionCallback) {
        val delegate = delegates.find { it.callback == callback }
        if (delegate != null) {
            packageInstaller.unregisterCallback(delegate)
        }
    }

    suspend fun uninstall(
        packageName: String,
        installerPackageName: String,
        userId: Int
    ) = IntentReceiverCompat.onDelegate { sender ->
        packageInstaller.uninstall(
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
        internal val callback: SessionCallback
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

    class SessionParams(
        mode: Int
    ) : PackageInstaller.SessionParams(mode) {
        private val original by lazy {
            Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(this)
        }

        var installFlags: Int
            get() = original.installFlags
            set(flags) {
                original.installFlags = installFlags or flags
            }

        companion object Default {
            val INSTALL_REPLACE_EXISTING get() = PackageManagerHidden.INSTALL_REPLACE_EXISTING

            val INSTALL_ALLOW_TEST get() = PackageManagerHidden.INSTALL_ALLOW_TEST

            val INSTALL_REQUEST_DOWNGRADE get() = PackageManagerHidden.INSTALL_REQUEST_DOWNGRADE

            @get:RequiresApi(34)
            val INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK get() = PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK

            @get:RequiresApi(34)
            val INSTALL_REQUEST_UPDATE_OWNERSHIP get() = PackageManagerHidden.INSTALL_REQUEST_UPDATE_OWNERSHIP
        }
    }

    companion object Default {
        @SuppressLint("RequestInstallPackagesPolicy")
        suspend fun PackageInstaller.Session.commit() = IntentReceiverCompat.onDelegate { sender ->
            commit(sender)
        }

        private inline fun InputStream.copyTo(
            out: OutputStream,
            bufferSize: Int = DEFAULT_BUFFER_SIZE,
            onProgress: (Long) -> Unit
        ): Long {
            var bytesCopied: Long = 0
            val buffer = ByteArray(bufferSize)
            var bytes = read(buffer)
            while (bytes >= 0) {
                out.write(buffer, 0, bytes)
                bytesCopied += bytes
                onProgress(bytesCopied)
                bytes = read(buffer)
            }
            return bytesCopied
        }

        suspend fun PackageInstaller.Session.writeFd(
            name: String,
            fd: AssetFileDescriptor,
            onProgress: (Long, Long) -> Unit = { _, _ -> }
        ) = withContext(Dispatchers.IO) {
            fd.createInputStream().use { input ->
                val length = fd.length
                openWrite(name, 0, length).use { output ->
                    input.copyTo(output) { onProgress(length, it) }
                    fsync(output)
                }
            }
        }

        suspend fun PackageInstaller.Session.writeZip(
            names: List<String>,
            fd: AssetFileDescriptor,
            onProgress: (String, Long, Long) -> Unit = { _, _, _ -> }
        ) = withContext(Dispatchers.IO) {
            ZipFile.builder()
                .setIgnoreLocalFileHeader(true)
                .setSeekableByteChannel(fd.createInputStream().channel)
                .get()
                .use { zip ->
                    zip.entries.iterator().forEach { entry ->
                        if (entry.name in names) {
                            zip.getInputStream(entry).use { input ->
                                openWrite(entry.name, 0, entry.size).use { output ->
                                    input.copyTo(output) { onProgress(entry.name, entry.size, it) }
                                    fsync(output)
                                }
                            }
                        }
                    }
                }
        }
    }
}