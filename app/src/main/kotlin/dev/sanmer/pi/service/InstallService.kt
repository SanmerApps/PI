package dev.sanmer.pi.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
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
import dev.sanmer.pi.compat.PackageManagerCompat
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.utils.extensions.dp
import kotlinx.coroutines.async
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
    private val taskCount = MutableStateFlow(0)

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    init {
        taskCount.drop(1)
            .onEach {
                if (it == 0) {
                    delay(5000L)
                    stopSelf()
                }
            }.launchIn(lifecycleScope)
    }

    override fun onCreate() {
        Timber.d("InstallService onCreate")
        super.onCreate()
        setForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch {
            val packageFile = intent?.packageFile ?: return@launch
            val originating = userPreferencesRepository.getRequesterPackageNameOrDefault()
            val installer = userPreferencesRepository.getExecutorPackageNameOrDefault()

            taskCount.value += 1
            val id = taskCount.value

            val archiveInfo = getArchiveInfo(packageFile) ?: return@launch
            val label = archiveInfo.applicationInfo
                .loadLabel(context.packageManager)
                .toString()
            val appIcon = AppIconLoader(40.dp, true, context)
                .loadIcon(archiveInfo.applicationInfo)

            notifyInstalling(id, label, appIcon)

            val install = async {
                PackageManagerCompat.install(
                    packageFile = packageFile,
                    packageName = archiveInfo.packageName,
                    installer = installer,
                    originating = originating
                )
            }

            val state = install.await()
            when (state) {
                PackageInstaller.STATUS_SUCCESS -> notifySuccess(id, label, appIcon)
                PackageInstaller.STATUS_FAILURE -> notifyFailure(id, label)
            }

            packageFile.delete()
            taskCount.value -= 1
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.d("InstallService onDestroy")
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun getArchiveInfo(archiveFile: File): PackageInfo? {
        return context.packageManager.getPackageArchiveInfo(
            archiveFile.path, 0
        )?.also {
            it.applicationInfo.sourceDir = archiveFile.path
            it.applicationInfo.publicSourceDir = archiveFile.path
        }
    }

    private fun setForeground() {
        val notification = NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)
            .setSilent(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()

        startForeground(NotificationUtils.NOTIFICATION_ID_INSTALL, notification)
    }

    private fun buildNotification(
        title: String,
        message: String,
        silent: Boolean = false,
        largeIcon: Bitmap? = null
    ) = NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_INSTALL)
        .setSmallIcon(R.drawable.launcher_outline)
        .setContentTitle(title)
        .setContentText(message)
        .setSilent(silent)
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
        val notification = buildNotification(title, message, true, largeIcon)

        notify(id, notification)
    }

    private fun notifySuccess(id: Int, title: String, largeIcon: Bitmap) {
        val message = context.getString(R.string.message_install_success)
        val notification = buildNotification(title, message, true, largeIcon)

        notify(id, notification)
    }

    private fun notifyFailure(id: Int, title: String) {
        val message = context.getString(R.string.message_install_fail)
        val notification = buildNotification(title, message, false)

        notify(id, notification)
    }

    companion object {
        private const val GROUP_KEY = "INSTALL_SERVICE_GROUP_KEY"

        private const val PARAM_PACKAGE_PATH = "PACKAGE_PATH"
        private val Intent.packagePathOrNull get() = getStringExtra(PARAM_PACKAGE_PATH)
        private val Intent.packageFile get() = checkNotNull(packagePathOrNull).let(::File)

        fun Context.startInstallService(packageFile: File, ) {
            val intent = Intent(this, InstallService::class.java)
            intent.putExtra(PARAM_PACKAGE_PATH, packageFile.path)

            startService(intent)
        }
    }
}