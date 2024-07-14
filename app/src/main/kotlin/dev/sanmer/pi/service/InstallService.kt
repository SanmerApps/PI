package dev.sanmer.pi.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionInfo
import android.graphics.Bitmap
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.Compat
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.delegate.PackageInstallerDelegate
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Companion.commit
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Companion.writeApk
import dev.sanmer.pi.delegate.PackageInstallerDelegate.Companion.writeApks
import dev.sanmer.pi.ktx.dp
import dev.sanmer.pi.ktx.parcelable
import dev.sanmer.pi.ktx.tmpDir
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class InstallService : LifecycleService(), PackageInstallerDelegate.SessionCallback {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val appIconLoader by lazy {
        AppIconLoader(45.dp, true, this)
    }

    private val pm by lazy { Compat.getPackageManager() }
    private val pi by lazy { Compat.getPackageInstaller() }

    private val tasks = mutableListOf<Int>()

    override fun onCreated(sessionId: Int) {
        val session = pi.getSessionInfo(sessionId)
        Timber.i("onCreated<$sessionId>: ${session?.appPackageName}")
        tasks.add(sessionId)

        onProgressChanged(
            id = sessionId,
            appLabel = session?.label ?: sessionId.toString(),
            appIcon = session?.appIcon,
            progress = 0f
        )
    }

    override fun onProgressChanged(sessionId: Int, progress: Float) {
        val session = pi.getSessionInfo(sessionId)

        onProgressChanged(
            id = sessionId,
            appLabel = session?.label ?: sessionId.toString(),
            appIcon = session?.appIcon,
            progress = progress
        )
    }

    override fun onFinished(sessionId: Int, success: Boolean) {
        Timber.i("onFinished<$sessionId>: success = $success")
        tasks.remove(sessionId)

        if (tasks.isEmpty()) {
            stopSelf()
        }
    }

    private val SessionInfo.label get() =
        appLabel ?: appPackageName ?: sessionId.toString()

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()

        pi.registerCallback(this)
        setForeground()
    }

    override fun onDestroy() {
        tmpDir.deleteRecursively()
        pi.unregisterCallback(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)

        Timber.d("onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch(Dispatchers.IO) {
            val archivePath = intent?.archivePathOrNull ?: return@launch
            val archiveInfo = intent.archiveInfoOrNull ?: return@launch
            val filenames = intent.filenames

            val appIcon = archiveInfo.applicationInfo?.let(appIconLoader::loadIcon)
            val appLabel = archiveInfo.applicationInfo?.loadLabel(packageManager)
                ?: archiveInfo.packageName

            val userPreferences = userPreferencesRepository.data.first()
            val originatingUid = getPackageUid(userPreferences.requester)
            pi.setInstallerPackageName(userPreferences.executor)

            val params = createSessionParams()
            params.setAppIcon(appIcon)
            params.setAppLabel(appLabel)
            params.setAppPackageName(archiveInfo.packageName)
            if (originatingUid != Process.INVALID_UID) {
                params.setOriginatingUid(originatingUid)
            }

            val sessionId = pi.createSession(params)
            val session = pi.openSession(sessionId)

            when {
                archivePath.isDirectory -> {
                    session.writeApks(archivePath, filenames)
                }

                archivePath.isFile -> {
                    session.writeApk(archivePath)
                }
            }

            val result = session.commit()
            val status = result.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )

            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    onInstallSucceeded(
                        id = sessionId,
                        appLabel = appLabel,
                        appIcon = appIcon,
                        packageName = archiveInfo.packageName
                    )
                }

                else -> {
                    val msg = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Timber.e("onFailed<${archiveInfo.packageName}>: $msg")
                    onInstallFailed(
                        id = sessionId,
                        appLabel = appLabel,
                        appIcon = appIcon,
                    )
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
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

    private fun getPackageUid(packageName: String): Int =
        runCatching {
            pm.getPackageUid(packageName, 0, userId)
        }.getOrDefault(
            Process.INVALID_UID
        )

    private fun onProgressChanged(
        id: Int,
        appLabel: CharSequence,
        appIcon: Bitmap?,
        progress: Float
    ) {
        val notification = baseNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setProgress(100, (100 * progress).toInt(), false)
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .build()

        notify(id, notification)
    }

    private fun onInstallSucceeded(
        id: Int,
        appLabel: CharSequence,
        appIcon: Bitmap?,
        packageName: String
    ) {
        val intent = pm.getLaunchIntentForPackage(packageName, userId)?.let {
            PendingIntent.getActivity(
                this, 0, it, PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = baseNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setContentText(getString(R.string.message_install_success))
            .setContentIntent(intent)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        notify(id, notification)
    }

    private fun onInstallFailed(
        id: Int,
        appLabel: CharSequence,
        appIcon: Bitmap?
    ) {
        val notification = baseNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setContentText(getString(R.string.message_install_fail))
            .build()

        notify(id, notification)
    }

    private fun setForeground() {
        val notification = baseNotificationBuilder()
            .setContentTitle(getString(R.string.notification_name_install))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()

        startForeground(NotificationUtils.NOTIFICATION_ID_INSTALL, notification)
    }

    private fun baseNotificationBuilder() =
        NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: Notification) {
        val granted = if (BuildCompat.atLeastT) {
            PermissionCompat.checkPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

        NotificationManagerCompat.from(this).apply {
            if (granted) notify(id, notification)
        }
    }

    companion object {
        private const val GROUP_KEY = "INSTALL_SERVICE_GROUP_KEY"

        private const val EXTRA_ARCHIVE_PATH = "dev.sanmer.pi.extra.ARCHIVE_PATH"
        private val Intent.archivePathOrNull: File?
            get() = getStringExtra(EXTRA_ARCHIVE_PATH)?.let(::File)

        private const val EXTRA_ARCHIVE_INFO = "dev.sanmer.pi.extra.ARCHIVE_PACKAGE_INFO"
        private val Intent.archiveInfoOrNull: PackageInfo?
            get() = parcelable(EXTRA_ARCHIVE_INFO)

        private const val EXTRA_ARCHIVE_FILENAMES = "dev.sanmer.pi.extra.ARCHIVE_FILENAMES"
        private val Intent.filenames: List<String>
            get() = getStringArrayExtra(EXTRA_ARCHIVE_FILENAMES)?.toList() ?: emptyList()

        fun start(
            context: Context,
            archivePath: File,
            archiveInfo: PackageInfo,
            filenames: List<String>
        ) {
            val intent = Intent(context, InstallService::class.java)
            intent.putExtra(EXTRA_ARCHIVE_PATH, archivePath.path)
            intent.putExtra(EXTRA_ARCHIVE_INFO, archiveInfo)
            intent.putExtra(EXTRA_ARCHIVE_FILENAMES, filenames.toTypedArray())

            context.startService(intent)
        }
    }
}