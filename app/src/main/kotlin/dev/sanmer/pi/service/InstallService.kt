package dev.sanmer.pi.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.sanmer.pi.Const
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.Logger
import dev.sanmer.pi.R
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Default.commit
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Default.write
import dev.sanmer.pi.ktx.dp
import dev.sanmer.pi.model.Task
import dev.sanmer.pi.model.Task.Default.putTask
import dev.sanmer.pi.model.Task.Default.taskOrNull
import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.time.Duration.Companion.seconds

class InstallService : LifecycleService(), KoinComponent, PackageInstallerDelegate.SessionCallback {
    private val preferenceRepository by inject<PreferenceRepository>()
    private val serviceRepository by inject<ServiceRepository>()
    private val appIconLoader by lazy { AppIconLoader(45.dp, true, this) }
    private val nm by lazy { NotificationManagerCompat.from(this) }
    private val pm by lazy { serviceRepository.getPackageManager() }
    private val pi by lazy { serviceRepository.getPackageInstaller() }

    private val logger = Logger.Android("InstallService")

    init {
        lifecycleScope.launch {
            while (currentCoroutineContext().isActive) {
                if (pendingTasks.isEmpty()) stopSelf()
                delay(5.seconds)
            }
        }
    }

    override fun onCreated(sessionId: Int) {
        val session = pi.getSessionInfo(sessionId)
        logger.i("onCreated<$sessionId>: ${session?.appPackageName}")

        notifyProgress(
            id = sessionId,
            appLabel = session?.label ?: sessionId.toString(),
            appIcon = session?.appIcon,
            progress = 0f
        )
    }

    override fun onProgressChanged(sessionId: Int, progress: Float) {
        val session = pi.getSessionInfo(sessionId)

        notifyProgress(
            id = sessionId,
            appLabel = session?.label ?: sessionId.toString(),
            appIcon = session?.appIcon,
            progress = progress
        )
    }

    private val SessionInfo.label
        inline get() = appLabel ?: appPackageName ?: sessionId.toString()

    override fun onCreate() {
        logger.d("onCreate")
        super.onCreate()
        pi.registerCallback(this)
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

            install(task)
            task.archivePath.deleteRecursively()
            pendingTasks.remove(task.archivePath)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun install(task: Task) = withContext(Dispatchers.IO) {
        val appIcon = task.archiveInfo.applicationInfo?.let(appIconLoader::loadIcon)
        val appLabel = task.archiveInfo.applicationInfo?.loadLabel(packageManager)
            ?: task.archiveInfo.packageName

        val preference = preferenceRepository.data.first()
        val originatingUid = getPackageUid(
            preference.requester.ifEmpty { task.sourceInfo.packageName }
        )
        pi.setInstallerPackageName(preference.executor)
        pi.setUserId(task.userId)

        val params = createSessionParams()
        params.setAppIcon(appIcon)
        params.setAppLabel(appLabel)
        params.setAppPackageName(task.archiveInfo.packageName)
        if (originatingUid != Process.INVALID_UID) {
            params.setOriginatingUid(originatingUid)
        }

        val sessionId = pi.createSession(params)
        val session = pi.openSession(sessionId)

        when (task) {
            is Task.Apk -> session.write(task.archivePath)
            is Task.AppBundle -> session.write(task.archiveFiles)
        }

        val result = session.commit()
        val status = result.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                notifyOptimizing(
                    id = sessionId,
                    appLabel = appLabel,
                    appIcon = appIcon
                )

                optimize(task.archiveInfo.packageName)

                notifySuccess(
                    id = sessionId,
                    appLabel = appLabel,
                    appIcon = appIcon,
                    packageName = task.archiveInfo.packageName
                )
            }

            else -> {
                val msg = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                logger.e("onFailed<${task.archiveInfo.packageName}>: $msg")
                notifyFailure(
                    id = sessionId,
                    appLabel = appLabel,
                    appIcon = appIcon
                )
            }
        }
    }

    private suspend fun optimize(packageName: String) = withContext(Dispatchers.IO) {
        runCatching {
            pm.clearApplicationProfileData(packageName)
            pm.performDexOpt(packageName).also {
                if (!it) logger.e("Failed to optimize $packageName")
            }
        }.onFailure { error ->
            logger.e(error)
        }.getOrDefault(false)
    }

    private fun createSessionParams(): PackageInstaller.SessionParams {
        val params = PackageInstallerDelegate.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )

        params.installFlags = with(PackageInstallerDelegate.SessionParams) {
            val flags = params.installFlags or
                    INSTALL_ALLOW_TEST or
                    INSTALL_REPLACE_EXISTING or
                    INSTALL_REQUEST_DOWNGRADE

            if (BuildCompat.atLeastU) {
                flags or INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
            } else {
                flags
            }
        }

        return params
    }

    private fun getPackageUid(packageName: String): Int {
        if (packageName.isEmpty()) return Process.INVALID_UID
        return runCatching {
            pm.getPackageUid(packageName, 0, userId)
        }.getOrDefault(
            Process.INVALID_UID
        )
    }

    private fun setForeground() {
        val notification = newNotificationBuilder()
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
        appLabel: CharSequence,
        appIcon: Bitmap?,
        progress: Float
    ) {
        val notification = newNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setProgress(100, (100 * progress).toInt(), false)
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .build()

        notify(id, notification)
    }

    private fun notifyOptimizing(
        id: Int,
        appLabel: CharSequence,
        appIcon: Bitmap?
    ) {
        val notification = newNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setContentText(getString(R.string.message_optimizing))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .build()

        notify(id, notification)
    }

    private fun notifySuccess(
        id: Int,
        appLabel: CharSequence,
        appIcon: Bitmap?,
        packageName: String
    ) {
        val pending = pm.getLaunchIntentForPackage(packageName, userId)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = newNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setContentText(getText(R.string.message_install_succeed))
            .setContentIntent(pending)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        notify(id, notification)
    }

    private fun notifyFailure(
        id: Int,
        appLabel: CharSequence,
        appIcon: Bitmap?
    ) {
        val notification = newNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setContentText(getText(R.string.message_install_failed))
            .build()

        notify(id, notification)
    }

    private fun newNotificationBuilder() =
        NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)

    private fun notify(id: Int, notification: Notification) {
        if (
            !BuildCompat.atLeastT
            || PermissionCompat.checkPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        ) nm.notify(id, notification)
    }

    companion object Default {
        private const val GROUP_KEY = "dev.sanmer.pi.INSTALL_SERVICE_GROUP_KEY"

        private val pendingTasks = mutableListOf<File>()

        fun apk(
            context: Context,
            archivePath: File,
            archiveInfo: PackageInfo,
            sourceInfo: PackageInfo,
            userId: Int
        ) {
            val task = Task.Apk(
                archivePath = archivePath,
                archiveInfo = archiveInfo,
                userId = userId,
                sourceInfo = sourceInfo
            )
            pendingTasks.add(task.archivePath)
            context.startService(
                Intent(context, InstallService::class.java).also {
                    it.putTask(task)
                }
            )
        }

        fun appBundle(
            context: Context,
            archivePath: File,
            archiveInfo: PackageInfo,
            splitConfigs: List<SplitConfig>,
            userId: Int,
            sourceInfo: PackageInfo
        ) {
            val task = Task.AppBundle(
                archivePath = archivePath,
                archiveInfo = archiveInfo,
                splitConfigs = splitConfigs,
                userId = userId,
                sourceInfo = sourceInfo
            )
            pendingTasks.add(task.archivePath)
            context.startService(
                Intent(context, InstallService::class.java).also {
                    it.putTask(task)
                }
            )
        }
    }
}