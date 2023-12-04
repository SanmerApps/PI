package dev.sanmer.pi.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ArchiveInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.compat.ContextCompat.userId
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.repository.SettingsRepository
import dev.sanmer.pi.utils.extensions.dp
import dev.sanmer.pi.utils.extensions.tmpDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class InstallService: LifecycleService() {
    private val context: Context by lazy { applicationContext }
    private val pmCompat get() = ProviderCompat.packageManagerCompat
    private val tasks = MutableStateFlow(0)

    @Inject
    lateinit var settingsRepository: SettingsRepository

    init {
        tasks.drop(1)
            .onEach {
                if (it == 0) {
                    delay(10_000L)
                    if (tasks.value == 0) stopSelf()
                }
            }.launchIn(lifecycleScope)
    }

    override fun onCreate() {
        Timber.d("InstallService onCreate")
        super.onCreate()
        setForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch(Dispatchers.IO) {
            val packagePath = intent?.packagePath ?: return@launch
            val originating = settingsRepository.getRequesterOrDefault()
            val installer = settingsRepository.getExecutorOrDefault()

            tasks.value += 1
            val id = tasks.value

            val archiveInfo = getArchiveInfo(packagePath) ?: return@launch
            val label = archiveInfo.applicationInfo
                .loadLabel(context.packageManager)
                .toString()
            val appIcon = AppIconLoader(40.dp, true, context)
                .loadIcon(archiveInfo.applicationInfo)

            notifyInstalling(id, label, appIcon)

            val state = pmCompat.install(
                ArchiveInfo(packagePath, archiveInfo.packageName, originating),
                installer,
                userId
            )

            when (state) {
                PackageInstaller.STATUS_SUCCESS -> notifySuccess(id, label, appIcon)
                else -> notifyFailure(id, label, appIcon)
            }

            tasks.value -= 1
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.d("InstallService onDestroy")
        tmpDir.deleteRecursively()

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun getArchiveInfo(packagePath: String): PackageInfo? {
        return context.packageManager.getPackageArchiveInfo(
            packagePath, 0
        )?.also {
            it.applicationInfo.sourceDir = packagePath
            it.applicationInfo.publicSourceDir = packagePath
        }
    }

    private fun setForeground() {
        val notification = NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)
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
    ) = NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_INSTALL)
        .setSmallIcon(R.drawable.launcher_outline)
        .setContentTitle(title)
        .setContentText(message)
        .setSilent(silent)
        .setOngoing(ongoing)
        .setGroup(GROUP_KEY)
        .apply {
            largeIcon?.let { setLargeIcon(it) }
        }
        .build()

    private fun notify(id: Int, notification: Notification) {
        val notificationId = NotificationUtils.NOTIFICATION_ID_INSTALL + id
        NotificationManagerCompat.from(this).apply {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
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

        notify(id, notification)
    }

    private fun notifySuccess(id: Int, title: String, largeIcon: Bitmap) {
        val message = context.getString(R.string.message_install_success)
        val notification = buildNotification(
            title = title,
            message = message,
            largeIcon = largeIcon,
            silent = true
        )

        notify(id, notification)
    }

    private fun notifyFailure(id: Int, title: String, largeIcon: Bitmap) {
        val message = context.getString(R.string.message_install_fail)
        val notification = buildNotification(
            title = title,
            message = message,
            largeIcon = largeIcon,
            silent = false
        )

        notify(id, notification)
    }

    companion object {
        private const val GROUP_KEY = "INSTALL_SERVICE_GROUP_KEY"

        private const val PARAM_PACKAGE_PATH = "PACKAGE_PATH"
        private val Intent.packagePathOrNull get() = getStringExtra(PARAM_PACKAGE_PATH)
        private val Intent.packagePath get() = checkNotNull(packagePathOrNull)

        fun Context.startInstallService(packageFile: File) {
            val intent = Intent(this, InstallService::class.java)
            intent.putExtra(PARAM_PACKAGE_PATH, packageFile.path)

            startService(intent)
        }
    }
}