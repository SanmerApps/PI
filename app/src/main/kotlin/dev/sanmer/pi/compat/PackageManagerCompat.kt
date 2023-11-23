package dev.sanmer.pi.compat

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageInstallerHidden.SessionParamsHidden
import android.content.pm.PackageManagerHidden
import android.content.pm.ParceledListSlice
import android.os.Process
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object PackageManagerCompat {
    private val packageManager: IPackageManager by lazy {
        IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
    }

    private val installer: IPackageInstaller by lazy {
        IPackageInstaller.Stub.asInterface(
            ShizukuBinderWrapper(packageManager.packageInstaller.asBinder())
        )
    }

    private fun getPackageInstaller(installerPackageName: String): PackageInstaller {
        return if (BuildCompat.atLeastS) {
            Refine.unsafeCast(
                PackageInstallerHidden(installer, installerPackageName, null, 0)
            )
        } else {
            Refine.unsafeCast(
                PackageInstallerHidden(installer, installerPackageName, 0)
            )
        }
    }

    private fun createSession(
        installerPackageName: String,
        params: PackageInstaller.SessionParams
    ): PackageInstaller.Session {
        val packageInstaller = getPackageInstaller(installerPackageName)
        val sessionId = packageInstaller.createSession(params)
        val iSession = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(installer.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }

    fun getPackageUid(packageName: String, flags: Int, userId: Int): Int {
        return if (BuildCompat.atLeastT) {
            packageManager.getPackageUid(packageName, flags.toLong(), userId)
        } else {
            packageManager.getPackageUid(packageName, flags, userId)
        }
    }

    fun getPackageInfo(packageName: String, flags: Int, userId: Int): PackageInfo {
        return if (BuildCompat.atLeastT) {
            packageManager.getPackageInfo(packageName, flags.toLong(), userId)
        } else {
            packageManager.getPackageInfo(packageName, flags, userId)
        }
    }

    fun getApplicationInfo(packageName: String, flags: Int, userId: Int): ApplicationInfo {
        return if (BuildCompat.atLeastT) {
            packageManager.getApplicationInfo(packageName, flags.toLong(), userId)
        } else {
            packageManager.getApplicationInfo(packageName, flags, userId)
        }
    }

    fun getInstalledPackages(flags: Int, userId: Int): List<PackageInfo> {
        val packages: ParceledListSlice<PackageInfo>? = if (BuildCompat.atLeastT) {
            packageManager.getInstalledPackages(flags.toLong(), userId)
        } else {
            packageManager.getInstalledPackages(flags, userId)
        }

        return if (packages != null) {
            packages.list
        } else {
            emptyList()
        }
    }

    suspend fun install(
        packageFile: File,
        packageName: String,
        installer: String,
        originating: String
    ): Int = withContext(Dispatchers.IO) {
        try {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

            val originatingUid = runCatching {
                    getPackageUid(originating, 0, 0)
                }.getOrDefault(Process.INVALID_UID)

            if (originatingUid != Process.INVALID_UID) {
                params.setOriginatingUid(originatingUid)
            }

            var flags = Refine.unsafeCast<SessionParamsHidden>(params).installFlags
            flags = flags or PackageManagerHidden.INSTALL_ALLOW_TEST or PackageManagerHidden.INSTALL_REPLACE_EXISTING
            Refine.unsafeCast<SessionParamsHidden>(params).installFlags = flags

            val input = packageFile.inputStream()
            createSession(installer, params).use { session ->
                session.openWrite(packageName, 0, input.available().toLong()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)

                    input.close()
                }

                val intent = suspendCoroutine { cont ->
                    val adapter = IntentSenderCompat.IIntentSenderAdaptor {
                        cont.resume(it)
                    }

                    val intentSender = IntentSenderCompat.newInstance(adapter)
                    session.commit(intentSender)
                }

                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                if (msg != null) Timber.d("Install finished: $msg")

                intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            }
        } catch (e: Exception) {
            Timber.e(e, "Install failed: $packageName")
            PackageInstaller.STATUS_FAILURE
        }
    }
}