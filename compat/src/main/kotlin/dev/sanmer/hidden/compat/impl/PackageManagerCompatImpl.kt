package dev.sanmer.hidden.compat.impl

import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSenderHidden
import android.content.pm.ApplicationInfo
import android.content.pm.ArchiveInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManagerHidden
import android.content.pm.ParceledListSlice
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.Process
import androidx.collection.LruCache
import dev.rikka.tools.refine.Refine
import dev.sanmer.hidden.compat.BuildCompat
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class PackageManagerCompatImpl(
    private val original: IPackageManager
) : IPackageManagerCompat.Stub() {
    private val installer by lazy { original.packageInstaller }

    private val lruCache = LruCache<LruCacheKey, List<Parcelable>>(3)

    private data class LruCacheKey(
        private val key0: Any,
        private val key1: Any,
        private val key2: Any
    )

    override fun getApplicationInfo(
        packageName: String,
        flags: Int,
        userId: Int
    ): ApplicationInfo {
        return if (BuildCompat.atLeastT) {
            original.getApplicationInfo(packageName, flags.toLong(), userId)
        } else {
            original.getApplicationInfo(packageName, flags, userId)
        }
    }

    override fun getPackageInfo(
        packageName: String,
        flags: Int,
        userId: Int
    ): PackageInfo {
        return if (BuildCompat.atLeastT) {
            original.getPackageInfo(packageName, flags.toLong(), userId)
        } else {
            original.getPackageInfo(packageName, flags, userId)
        }
    }

    override fun getPackageUid(
        packageName: String,
        flags: Int,
        userId: Int
    ): Int {
        return if (BuildCompat.atLeastT) {
            original.getPackageUid(packageName, flags.toLong(), userId)
        } else {
            original.getPackageUid(packageName, flags, userId)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getInstalledPackages(
        flags: Int,
        userId: Int
    ): ParceledListSlice<PackageInfo> {
        val key = LruCacheKey("getInstalledPackages", flags, userId)
        if (lruCache[key] == null) {
            val parceledList = if (BuildCompat.atLeastT) {
                original.getInstalledPackages(flags.toLong(), userId)
            } else {
                original.getInstalledPackages(flags, userId)
            }

            lruCache.put(key, parceledList.list)
        }

        return ParceledListSlice(lruCache[key] as List<PackageInfo>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getInstalledApplications(
        flags: Int,
        userId: Int
    ): ParceledListSlice<ApplicationInfo> {
        val key = LruCacheKey("getInstalledApplications", flags, userId)
        if (lruCache[key] == null) {
            val parceledList = if (BuildCompat.atLeastT) {
                original.getInstalledApplications(flags.toLong(), userId)
            } else {
                original.getInstalledApplications(flags, userId)
            }

            lruCache.put(key, parceledList.list)
        }

        return ParceledListSlice(lruCache[key] as List<ApplicationInfo>)
    }

    override fun getPackagesForUid(uid: Int): Array<String> {
        return original.getPackagesForUid(uid)
    }

    override fun install(
        archiveInfo: ArchiveInfo,
        installerPackageName: String,
        userId: Int
    ): Int = runBlocking {
        val state = async(Dispatchers.IO) {
            install(
                packageFile = archiveInfo.packageFile,
                packageName = archiveInfo.packageName,
                originating = archiveInfo.originating,
                installerName = installerPackageName,
                userId = userId
            )
        }

        state.await()
    }

    private fun createSession(
        params: PackageInstaller.SessionParams,
        installerPackageName: String,
        userId: Int
    ): PackageInstaller.Session {
        val packageInstaller: PackageInstaller = if (BuildCompat.atLeastS) {
            Refine.unsafeCast(
                PackageInstallerHidden(installer, installerPackageName, null, userId)
            )
        } else {
            Refine.unsafeCast(
                PackageInstallerHidden(installer, installerPackageName, userId)
            )
        }

        val sessionId = packageInstaller.createSession(params)
        val session = installer.openSession(sessionId)
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(session))
    }

    private suspend fun install(
        packageFile: File,
        packageName: String,
        originating: String,
        installerName: String,
        userId: Int
    ): Int = runCatching {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        val originatingUid = runCatching {
            getPackageUid(originating, 0, 0)
        }.getOrDefault(Process.INVALID_UID)

        if (originatingUid != Process.INVALID_UID) {
            params.setOriginatingUid(originatingUid)
        }

        var flags = Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags
        flags = flags or PackageManagerHidden.INSTALL_ALLOW_TEST or
                PackageManagerHidden.INSTALL_REPLACE_EXISTING

        Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags = flags

        val input = packageFile.inputStream()
        createSession(params, installerName, userId).use { session ->
            session.openWrite(packageName, 0, input.available().toLong()).use { output ->
                input.copyTo(output)
                session.fsync(output)

                input.close()
            }

            val intent = suspendCoroutine { cont ->
                val target = object : IIntentSender.Stub() {
                    override fun send(
                        code: Int,
                        intent: Intent,
                        resolvedType: String?,
                        whitelistToken: IBinder?,
                        finishedReceiver: IIntentReceiver?,
                        requiredPermission: String?,
                        options: Bundle?
                    ) {
                        cont.resume(intent)
                    }
                }

                val statusReceiver: IntentSender = Refine.unsafeCast(IntentSenderHidden(target))
                session.commit(statusReceiver)
            }

            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        }
    }.getOrDefault(
        PackageInstaller.STATUS_FAILURE
    )
}