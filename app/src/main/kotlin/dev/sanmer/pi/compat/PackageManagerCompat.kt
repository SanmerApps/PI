package dev.sanmer.pi.compat

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageInstallerHidden.SessionParamsHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.ParceledListSlice
import android.content.pm.ResolveInfo
import android.content.pm.VersionedPackage
import dev.rikka.tools.refine.Refine
import dev.sanmer.pi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object PackageManagerCompat {
    private const val INSTALLER_PACKAGE_NAME= BuildConfig.APPLICATION_ID

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

    private val packageInstaller: PackageInstaller by lazy {
        if (BuildCompat.atLeastS) {
            Refine.unsafeCast(
                PackageInstallerHidden(installer, INSTALLER_PACKAGE_NAME, null, 0)
            )
        } else {
            Refine.unsafeCast(
                PackageInstallerHidden(installer, INSTALLER_PACKAGE_NAME, 0)
            )
        }
    }

    private fun createSession(params: PackageInstaller.SessionParams): PackageInstaller.Session {
        val sessionId = packageInstaller.createSession(params)
        val iSession = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(installer.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }

    private fun uninstallPackage(packageName: String, intentSender: IntentSender) {
        installer.uninstall(
            VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST),
            INSTALLER_PACKAGE_NAME, 0, intentSender, 0
        )
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

    fun addPreferredActivity(filter: IntentFilter, match: Int, set: Array<ComponentName>, activity: ComponentName, userId: Int) {
        if (BuildCompat.atLeastS) {
            packageManager.addPreferredActivity(filter, match, set, activity, userId, true)
        } else {
            packageManager.addPreferredActivity(filter, match, set, activity, userId)
        }
    }

    fun clearPackagePreferredActivities(packageName: String) {
        packageManager.clearPackagePreferredActivities(packageName)
    }

    fun getPreferredActivities(packageName: String?): List<Pair<ComponentName, IntentFilter>> {
        val outFilters = ArrayList<IntentFilter>()
        val outActivities = ArrayList<ComponentName>()
        packageManager.getPreferredActivities(outFilters, outActivities, packageName)

        return outActivities.zip(outFilters)
    }

    fun queryIntentActivities(
        intent: Intent,
        resolvedType: String,
        flags: Int,
        userId: Int
    ): List<ResolveInfo> {
        val resolveInfo: ParceledListSlice<ResolveInfo>? = if (BuildCompat.atLeastT) {
            packageManager.queryIntentActivities(intent, resolvedType, flags.toLong(), userId)
        } else {
            packageManager.queryIntentActivities(intent, resolvedType, flags, userId)
        }

        return if (resolveInfo != null) {
            resolveInfo.list
        } else {
            emptyList()
        }
    }

    fun getHomeActivities(): ComponentName {
        val outHomeCandidates = ArrayList<ResolveInfo>()
        return packageManager.getHomeActivities(outHomeCandidates)
    }

    suspend fun install(packageFile: File, packageName: String, originatingPackageName: String?): Int = withContext(Dispatchers.IO) {
        try {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

            if (originatingPackageName != null) {
                val originatingUid = getPackageUid(originatingPackageName, 0, 0)
                params.setOriginatingUid(originatingUid)
            }

            var flags = Refine.unsafeCast<SessionParamsHidden>(params).installFlags
            flags = flags or PackageManagerHidden.INSTALL_ALLOW_TEST or PackageManagerHidden.INSTALL_REPLACE_EXISTING
            Refine.unsafeCast<SessionParamsHidden>(params).installFlags = flags

            val input = packageFile.inputStream()
            createSession(params).use { session ->
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

                Timber.i("Install successful: $packageName")
                intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            }
        } catch (e: Exception) {
            Timber.e(e, "Install failed: $packageName")
            PackageInstaller.STATUS_FAILURE
        }
    }

    suspend fun uninstall(packageName: String): Int = withContext(Dispatchers.IO) {
        try {
            val intent = suspendCoroutine { cont ->
                val adapter = IntentSenderCompat.IIntentSenderAdaptor {
                    cont.resume(it)
                }
                val intentSender = IntentSenderCompat.newInstance(adapter)
                uninstallPackage(packageName, intentSender)
            }

            Timber.i("Uninstall failed: $packageName")
            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        } catch (e: Exception) {
            Timber.e(e, "Uninstall failed: $packageName")
            PackageInstaller.STATUS_FAILURE
        }
    }
}