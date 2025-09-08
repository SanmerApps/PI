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
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Parcelable
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
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Default.commit
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Default.writeFd
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Default.writeZip
import dev.sanmer.pi.factory.BundleFactory
import dev.sanmer.pi.ktx.parcelable
import dev.sanmer.pi.parser.PackageInfoLite
import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

class InstallService : LifecycleService(), KoinComponent, PackageInstallerDelegate.SessionCallback {
    private val preferenceRepository by inject<PreferenceRepository>()
    private val serviceRepository by inject<ServiceRepository>()
    private val bundleFactory by inject<BundleFactory>()
    private val nm by lazy { NotificationManagerCompat.from(this) }
    private val pm by lazy { serviceRepository.getPackageManager() }
    private val pi by lazy { serviceRepository.getPackageInstaller() }

    private val logger = Logger.Android("InstallService")

    init {
        lifecycleScope.launch {
            while (currentCoroutineContext().isActive) {
                if (pendingUris.isEmpty()) stopSelf()
                delay(5.seconds)
            }
        }
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
            bundleFactory.openFd(task.uri).use {
                install(task, it)
            }
            pendingUris.remove(task.uri)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun install(
        task: Task,
        fd: ParcelFileDescriptor
    ) = withContext(Dispatchers.IO) {
        val preference = preferenceRepository.data.first()
        val originating = preference.requester.ifEmpty { task.sourceInfo?.packageName }
        val originatingUid = originating?.let(::getPackageUid) ?: Process.INVALID_UID

        pi.setInstallerPackageName(preference.executor)
        pi.setUserId(task.userId)

        val params = createSessionParams()
        params.setAppIcon(task.archiveInfo.iconOrDefault)
        params.setAppLabel(task.archiveInfo.labelOrDefault)
        params.setAppPackageName(task.archiveInfo.packageName)
        if (originatingUid != Process.INVALID_UID) {
            params.setOriginatingUid(originatingUid)
        }

        val sessionId = pi.createSession(params)
        notifyProgress(
            id = sessionId,
            appLabel = task.archiveInfo.labelOrDefault,
            appIcon = task.archiveInfo.iconOrDefault,
            progress = 0f
        )

        val session = pi.openSession(sessionId)
        if (task.fileNames.isEmpty()) {
            session.writeFd(task.archiveInfo.packageName, fd)
        } else {
            session.writeZip(task.fileNames, fd)
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
                    appLabel = task.archiveInfo.labelOrDefault,
                    appIcon = task.archiveInfo.iconOrDefault
                )

                optimize(task.archiveInfo.packageName)

                notifySuccess(
                    id = sessionId,
                    appLabel = task.archiveInfo.labelOrDefault,
                    appIcon = task.archiveInfo.iconOrDefault,
                    packageName = task.archiveInfo.packageName
                )
            }

            else -> {
                val msg = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                logger.e("Failed to install ${task.archiveInfo.packageName}, $msg")
                notifyFailure(
                    id = sessionId,
                    appLabel = task.archiveInfo.labelOrDefault,
                    appIcon = task.archiveInfo.iconOrDefault,
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

    @Parcelize
    private data class Task(
        val uri: Uri,
        val archiveInfo: PackageInfoLite,
        val fileNames: List<String>,
        val sourceInfo: PackageInfoLite?,
        val userId: Int
    ) : Parcelable

    companion object Default {
        private const val GROUP_KEY = "dev.sanmer.pi.INSTALL_SERVICE_GROUP_KEY"
        private const val EXTRA_TASK = "dev.sanmer.pi.extra.TASK"

        private fun Intent.putTask(value: Task) =
            putExtra(EXTRA_TASK, value)

        private inline val Intent.taskOrNull: Task?
            get() = parcelable(EXTRA_TASK)

        private val pendingUris = mutableListOf<Uri>()

        fun start(
            context: Context,
            uri: Uri,
            archiveInfo: PackageInfoLite,
            fileNames: List<String>,
            sourceInfo: PackageInfoLite? = null,
            userId: Int = context.userId
        ) {
            pendingUris.add(uri)
            context.startService(
                Intent(context, InstallService::class.java).also {
                    it.putTask(Task(uri, archiveInfo, fileNames, sourceInfo, userId))
                }
            )
        }
    }
}