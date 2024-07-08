package dev.sanmer.pi.delegate

import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerCallback
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.VersionedPackage
import androidx.annotation.RequiresApi
import dev.rikka.tools.refine.Refine
import dev.sanmer.pi.BuildCompat
import dev.sanmer.pi.ContextCompat
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.IntentReceiverCompat
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat.getSystemService
import dev.sanmer.su.ServiceManagerCompat.proxyBy
import java.io.File

class PackageInstallerDelegate(
    private val service: IServiceManager
) {
    private val context = ContextCompat.getContext()
    private var installerPackageName = context.packageName
    private var installerAttributionTag = context.packageName
    private val delegates = mutableListOf<SessionCallbackDelegate>()

    private val packageManager by lazy {
        IPackageManager.Stub.asInterface(
            service.getSystemService("package")
        )
    }

    private val packageInstaller by lazy {
        IPackageInstaller.Stub.asInterface(
            packageManager.packageInstaller.proxyBy(service)
        )
    }

    fun setInstallerPackageName(packageName: String) {
        installerPackageName = packageName
        installerAttributionTag = packageName
    }

    fun createSession(params: PackageInstaller.SessionParams): Int {
        return if (BuildCompat.atLeastS) {
            packageInstaller.createSession(
                params,
                installerPackageName,
                installerAttributionTag,
                context.userId
            )
        } else {
            packageInstaller.createSession(
                params,
                installerPackageName,
                context.userId
            )
        }
    }

    fun openSession(sessionId: Int): PackageInstaller.Session {
        val session = IPackageInstallerSession.Stub.asInterface(
            packageInstaller.openSession(sessionId).proxyBy(service)
        )

        return Refine.unsafeCast(
            PackageInstallerHidden.SessionHidden(session)
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

    suspend fun uninstall(packageName: String) = IntentReceiverCompat.onDelegate { sender ->
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

        companion object {
            val INSTALL_REPLACE_EXISTING get() = PackageManagerHidden.INSTALL_REPLACE_EXISTING

            val INSTALL_ALLOW_TEST get() = PackageManagerHidden.INSTALL_ALLOW_TEST

            val INSTALL_REQUEST_DOWNGRADE get() = PackageManagerHidden.INSTALL_REQUEST_DOWNGRADE

            @get:RequiresApi(34)
            val INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK get() = PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
        }
    }

    companion object {
        suspend fun PackageInstaller.Session.commit() = IntentReceiverCompat.onDelegate { sender ->
            commit(sender)
        }

        fun PackageInstaller.Session.writeApk(path: File) {
            openWrite(path.name, 0, path.length()).use { output ->
                path.inputStream().buffered().use { input ->
                    input.copyTo(output)
                    fsync(output)
                }
            }
        }

        fun PackageInstaller.Session.writeApks(path: File, filenames: List<String>) {
            filenames.forEach { name ->
                val file = File(path, name)
                if (file.exists()) writeApk(file)
            }
        }
    }
}