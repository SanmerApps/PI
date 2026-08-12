package dev.sanmer.pi.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.sanmer.pi.Const
import dev.sanmer.pi.Logger
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.core.compat.ContextCompat.userId
import dev.sanmer.pi.core.delegate.PackageInstallerDelegate
import dev.sanmer.pi.core.delegate.PackageInstallerDelegate.Default.commit
import dev.sanmer.pi.core.delegate.PackageInstallerDelegate.Default.writeFd
import dev.sanmer.pi.core.delegate.PackageInstallerDelegate.Default.writeZip
import dev.sanmer.pi.core.parser.PackageInfoLite
import dev.sanmer.pi.ktx.parcelable
import dev.sanmer.pi.ktx.versionDisplay
import dev.sanmer.pi.repository.SuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

class InstallService : LifecycleService(), KoinComponent, PackageInstallerDelegate.SessionCallback {
    private val suRepository by inject<SuRepository>()
    private val pm by lazy { suRepository.getPackageManager() }
    private val pi by lazy { suRepository.getPackageInstaller() }
    private val nm by lazy { NotificationManagerCompat.from(this) }

    private val logger = Logger.Android("InstallService")

    override fun onProgressChanged(sessionId: Int, progress: Float) {
        val session = pi.getSessionInfo(sessionId) ?: return
        notifyProgress(
            id = sessionId,
            icon = session.appIcon,
            label = session.label,
            progress = progress
        )
    }

    private val SessionInfo.label
        inline get() = appLabel ?: appPackageName ?: sessionId.toString()

    override fun onCreate() {
        logger.d("onCreate")
        super.onCreate()
        pi.registerCallback(this, userId)
        setForeground()
    }

    override fun onDestroy() {
        pi.unregisterCallback(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        logger.d("onDestroy")
        super.onDestroy()
    }

    override fun onTimeout(startId: Int) {
        stopSelf(startId)
        super.onTimeout(startId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch(Dispatchers.IO) {
            val task = intent?.taskOrNull ?: return@launch
            runCatching {
                val fd = contentResolver.openAssetFileDescriptor(task.uri, "r") ?: return@launch
                fd.use { install(task, it) }
            }.onFailure {
                notifyFailure(
                    id = task.uri.hashCode(),
                    packageInfo = task.packageInfo,
                    error = it
                )
            }
            pendingPackageNames.remove(task.packageInfo.packageName)
            if (pendingPackageNames.isEmpty()) {
                delay(5.seconds)
                if (pendingPackageNames.isEmpty()) stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun install(
        task: Task,
        fd: AssetFileDescriptor
    ) {
        val params = createSessionParams()
        params.setAppIcon(task.packageInfo.iconOrDefault)
        params.setAppLabel(task.packageInfo.labelOrDefault)
        params.setAppPackageName(task.packageInfo.packageName)
        params.setOriginatingUri(task.uri)

        val ownerPackageName = suRepository.state.value
            .getOrElse({ it.ownerPackageName }) { "" }

        val sessionId = pi.createSession(
            params = params,
            installerPackageName = ownerPackageName.ifEmpty { task.installerPackageName },
            userId = userId
        )
        notifyProgress(
            id = sessionId,
            icon = task.packageInfo.iconOrDefault,
            label = task.packageInfo.labelOrDefault,
            progress = 0f
        )

        val session = pi.openSession(sessionId)
        if (task.fileNames.isEmpty()) {
            session.writeFd(task.packageInfo.packageName, fd)
        } else {
            session.writeZip(task.fileNames, fd)
        }

        val result = session.commit()
        val status = result.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        check(status == PackageInstaller.STATUS_SUCCESS) {
            result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        }

        if (ownerPackageName.isEmpty()) {
            notifyOptimizing(
                id = sessionId,
                packageInfo = task.packageInfo
            )
            optimize(
                packageName = task.packageInfo.packageName
            )
        }
        notifySuccess(
            id = sessionId,
            packageInfo = task.packageInfo
        )
    }

    private fun optimize(packageName: String) {
        runCatching {
            pm.clearApplicationProfileData(packageName)
            pm.performDexOpt(packageName)
        }.onFailure {
            logger.e(it)
        }.getOrDefault(false)
    }

    private fun createSessionParams(): PackageInstaller.SessionParams {
        val params = PackageInstallerDelegate.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )

        params.setInstallReason(PackageManager.INSTALL_REASON_USER)
        params.installFlags = with(PackageInstallerDelegate.SessionParams) {
            val flags = params.installFlags or
                    INSTALL_ALLOW_TEST or
                    INSTALL_REPLACE_EXISTING or
                    INSTALL_REQUEST_DOWNGRADE

            if (BuildCompat.atLeastU) {
                flags or INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK or
                        INSTALL_REQUEST_UPDATE_OWNERSHIP
            } else {
                flags
            }
        }

        return params
    }

    private fun setForeground() {
        val notification = notificationBuilder()
            .setContentTitle(getText(R.string.installation_service))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()

        ServiceCompat.startForeground(
            this,
            Const.NOTIFICATION_ID_INSTALL,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun notifyProgress(
        id: Int,
        icon: Bitmap?,
        label: CharSequence,
        progress: Float
    ) {
        val notification = notificationBuilder()
            .setLargeIcon(icon)
            .setContentTitle(label)
            .setProgress(100, (100 * progress).toInt(), false)
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .build()

        notify(id, notification)
    }

    private fun notifyOptimizing(
        id: Int,
        packageInfo: PackageInfoLite
    ) {
        val notification = notificationBuilder()
            .setLargeIcon(packageInfo.icon)
            .setContentTitle(packageInfo.labelOrDefault)
            .setContentText(getString(R.string.optimizing))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .build()

        notify(id, notification)
    }

    private fun notifySuccess(
        id: Int,
        packageInfo: PackageInfoLite
    ) {
        val pending = pm.getLaunchIntentForPackage(packageInfo.packageName, userId)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = notificationBuilder()
            .setLargeIcon(packageInfo.icon)
            .setContentTitle(packageInfo.labelOrDefault)
            .setContentText(packageInfo.versionDisplay())
            .setContentIntent(pending)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        notify(id, notification)
    }

    private fun notifyFailure(
        id: Int,
        packageInfo: PackageInfoLite,
        error: Throwable
    ) {
        val notification = notificationBuilder()
            .setLargeIcon(packageInfo.icon)
            .setContentTitle(packageInfo.labelOrDefault)
            .setContentText(error.message ?: error.javaClass.name)
            .setSilent(false)
            .setOngoing(false)
            .build()

        notify(id, notification)
    }

    private fun notificationBuilder() =
        NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)

    private fun notify(id: Int, notification: Notification) {
        if (
            !BuildCompat.atLeastT
            || PermissionCompat.checkPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        ) nm.notify(id, notification)
    }

    @Parcelize
    private data class Task(
        val uri: Uri,
        val fileNames: List<String>,
        val packageInfo: PackageInfoLite,
        val installerPackageName: String
    ) : Parcelable

    companion object Default {
        private const val GROUP_KEY = "dev.sanmer.pi.INSTALL_SERVICE_GROUP_KEY"
        private const val EXTRA_TASK = "dev.sanmer.pi.extra.TASK"

        private fun Intent.putTask(value: Task) =
            putExtra(EXTRA_TASK, value)

        private inline val Intent.taskOrNull: Task?
            get() = parcelable(EXTRA_TASK)

        private val pendingPackageNames = mutableListOf<String>()

        fun start(
            context: Context,
            uri: Uri,
            fileNames: List<String>,
            packageInfo: PackageInfoLite,
            installerPackageName: String
        ) {
            fun start() {
                if (pendingPackageNames.contains(packageInfo.packageName)) return
                pendingPackageNames.add(packageInfo.packageName)
                context.startService(
                    Intent(context, InstallService::class.java).also {
                        it.putTask(
                            Task(
                                uri = uri,
                                fileNames = fileNames,
                                packageInfo = packageInfo,
                                installerPackageName = installerPackageName
                            )
                        )
                    }
                )
            }
            if (BuildCompat.atLeastT) {
                PermissionCompat.requestPermission(
                    context = context,
                    permission = Manifest.permission.POST_NOTIFICATIONS
                ) { allowed ->
                    if (allowed) start()
                }
            } else {
                start()
            }
        }
    }
}