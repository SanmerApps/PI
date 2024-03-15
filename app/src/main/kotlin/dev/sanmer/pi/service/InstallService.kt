package dev.sanmer.pi.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.content.ArchiveInfo
import dev.sanmer.hidden.compat.stub.IInstallCallback
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.utils.extensions.dp
import dev.sanmer.pi.utils.extensions.parcelable
import dev.sanmer.pi.utils.extensions.tmpDir
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class InstallService: LifecycleService() {
    private val context: Context by lazy { applicationContext }
    private val pmCompat get() = ProviderCompat.packageManagerCompat
    private val tasks = mutableListOf<PackageInfo>()

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    init {
        lifecycleScope.launch {
            while (isActive) {
                delay(10_000L)
                if (tasks.isEmpty()) stopSelf()
            }
        }
    }

    override fun onCreate() {
        Timber.d("InstallService onCreate")
        super.onCreate()
        setForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch {
            val archivePath = intent?.archiveFilePathOrNull ?: return@launch
            val archiveInfo = intent.archivePackageInfoOrNull ?: return@launch

            val userPreferences = userPreferencesRepository.data.first()
            val originating = userPreferences.requester
            val installer = userPreferences.executor

            val label = archiveInfo.applicationInfo
                .loadLabel(context.packageManager)
                .toString()

            val appIcon = AppIconLoader(40.dp, true, context)
                .loadIcon(archiveInfo.applicationInfo)

            val id = System.currentTimeMillis().toInt()
            val callback = object : IInstallCallback.Stub() {
                override fun onSuccess(intent: Intent) {
                    Timber.d("onSuccess: ${archiveInfo.packageName}")
                    notifySuccess(
                        id = id,
                        title = label,
                        largeIcon = appIcon,
                        packageName = archiveInfo.packageName
                    )

                    tasks.remove(archiveInfo)
                }

                override fun onFailure(intent: Intent?) {
                    val msg = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Timber.e("onFailure: ${archiveInfo.packageName}, $msg")

                    notifyFailure(
                        id = id,
                        title = label,
                        largeIcon = appIcon,
                    )

                    tasks.remove(archiveInfo)
                }
            }

            Timber.d("installPackage: ${archiveInfo.packageName}")
            tasks.add(archiveInfo)
            notifyInstalling(id, label, appIcon)

            val info = ArchiveInfo(archivePath, originating, archiveInfo)
            pmCompat.installPackage(info, installer, callback, userId)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.d("InstallService onDestroy")
        tmpDir.deleteRecursively()

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun setForeground() {
        val notification = NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)
            .setContentTitle(getString(R.string.notification_name_install))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()

        startForeground(NotificationUtils.NOTIFICATION_ID_INSTALL, notification)
    }

    private fun buildNotification(
        title: String,
        message: String,
        largeIcon: Bitmap? = null,
        silent: Boolean = false,
        ongoing: Boolean = false,
    ) = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_INSTALL)
        .setSmallIcon(R.drawable.launcher_outline)
        .setContentTitle(title)
        .setContentText(message)
        .setSilent(silent)
        .setOngoing(ongoing)
        .setGroup(GROUP_KEY)
        .setLargeIcon(largeIcon)

    private fun notify(id: Int, notification: Notification) {
        val notificationId = NotificationUtils.NOTIFICATION_ID_INSTALL + id
        NotificationManagerCompat.from(context).apply {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return

            notify(notificationId, notification)
        }
    }

    private fun notifyInstalling(id: Int, title: String, largeIcon: Bitmap) {
        val message = context.getString(R.string.message_installing)
        val notification = buildNotification(
            title = title,
            message = message,
            largeIcon = largeIcon,
            silent = true,
            ongoing = true
        )

        notify(id, notification.build())
    }

    private fun notifySuccess(
        id: Int,
        title: String,
        largeIcon: Bitmap,
        packageName: String
    ) {
        val message = context.getString(R.string.message_install_success)
        val intent = pmCompat.getLaunchIntentForPackage(packageName, userId)?.let {
            PendingIntent.getActivity(
                context, 0, it, PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = buildNotification(
            title = title,
            message = message,
            largeIcon = largeIcon,
            silent = true
        ).apply {
            setContentIntent(intent)
        }

        notify(id, notification.build())
    }

    private fun notifyFailure(id: Int, title: String, largeIcon: Bitmap) {
        val message = context.getString(R.string.message_install_fail)
        val notification = buildNotification(
            title = title,
            message = message,
            largeIcon = largeIcon,
            silent = false
        )

        notify(id, notification.build())
    }

    companion object {
        private const val GROUP_KEY = "INSTALL_SERVICE_GROUP_KEY"

        private const val PARAM_ARCHIVE_FILE_PATH = "ARCHIVE_FILE_PATH"
        private const val PARAM_ARCHIVE_PACKAGE_INFO = "ARCHIVE_PACKAGE_INFO"
        private val Intent.archiveFilePathOrNull get() =
            getStringExtra(PARAM_ARCHIVE_FILE_PATH)
        private val Intent.archivePackageInfoOrNull: PackageInfo? get() =
            parcelable(PARAM_ARCHIVE_PACKAGE_INFO)

        fun start(
            context: Context,
            archiveFilePath: String,
            archivePackageInfo: PackageInfo
        ) {
            val intent = Intent(context, InstallService::class.java)
            intent.putExtra(PARAM_ARCHIVE_FILE_PATH, archiveFilePath)
            intent.putExtra(PARAM_ARCHIVE_PACKAGE_INFO, archivePackageInfo)

            context.startService(intent)
        }
    }
}