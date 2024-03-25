package dev.sanmer.pi.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.graphics.Bitmap
import android.net.Uri
import android.os.Process
import android.system.Os
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.delegate.PackageInstallerDelegate
import dev.sanmer.hidden.compat.delegate.SessionCallbackDelegate
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.utils.extensions.dp
import dev.sanmer.pi.utils.extensions.parcelable
import dev.sanmer.pi.utils.extensions.tmpDir
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class InstallService: LifecycleService() {
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private val context: Context by lazy { applicationContext }
    private val pmCompat get() = ProviderCompat.packageManagerCompat
    private val delegate by lazy {
        PackageInstallerDelegate(
            pmCompat.packageInstallerCompat
        )
    }

    private val mCallback = object : SessionCallbackDelegate() {
        override fun onCreated(sessionId: Int) {
            Timber.d("onCreated: sessionId = $sessionId")
            val session = delegate.getSessionInfo(sessionId) ?: return
            if (session.appLabel.isNullOrEmpty()) return

            onProgressChanged(
                id = sessionId,
                title = session.appLabel.toString(),
                largeIcon = session.appIcon,
                progress = 0f
            )
        }

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            Timber.d("onProgressChanged: sessionId = $sessionId, progress = $progress")
            val session = delegate.getSessionInfo(sessionId) ?: return

            onProgressChanged(
                id = sessionId,
                title = session.appLabel.toString(),
                largeIcon = session.appIcon,
                progress = progress
            )
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            Timber.d("onFinished: sessionId = $sessionId, success = $success")
            val sessions = delegate.getMySessions()
            if (sessions.isEmpty()) {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        Timber.d("InstallService onCreate")
        super.onCreate()

        delegate.registerCallback(mCallback)
        setForeground()
    }

    override fun onDestroy() {
        tmpDir.deleteRecursively()
        delegate.unregisterCallback(mCallback)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)

        Timber.d("InstallService onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch {
            val archiveUri = intent?.archiveUriOrNull ?: return@launch
            val archiveInfo = intent.archiveInfoOrNull ?: return@launch

            Timber.i("onCreated: packageName = ${archiveInfo.packageName}")
            val params = PackageInstallerDelegate.createSessionParams()
            val sessionId = delegate.createSession(params)

            val userPreferences = userPreferencesRepository.data.first()
            val originating = userPreferences.requester
            delegate.installerPackageName = userPreferences.executor

            val uid = getPackageUid(originating)
            if (uid != Process.INVALID_UID) {
                params.setOriginatingUid(uid)
            }

            val appIcon = AppIconLoader(45.dp, true, context)
                .loadIcon(archiveInfo.applicationInfo)
            delegate.setAppIcon(sessionId, appIcon)

            val appLabel = archiveInfo.applicationInfo
                .loadLabel(packageManager).toString()
            delegate.setAppLabel(sessionId, appLabel)

            val cr = contentResolver
            val (filename, statSize) = checkNotNull(
                cr.openFileDescriptor(archiveUri, "r")
            ).use {
                val path = Os.readlink("/proc/self/fd/${it.fd}")
                val file = File(path)
                Timber.d("path = $path")

                file.name to it.statSize
            }

            val input = checkNotNull(
                cr.openInputStream(archiveUri)?.buffered()
            )
            delegate.openWrite(
                sessionId, filename, 0, statSize
            ).use {
                input.copyTo(it)
                input.close()
            }

            val result = delegate.commit(sessionId)
            val status = result.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )

            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    Timber.i("onSucceeded: packageName = ${archiveInfo.packageName}")
                    onInstallSucceeded(
                        id = sessionId,
                        title = appLabel,
                        largeIcon = appIcon,
                        packageName = archiveInfo.packageName
                    )
                }
                else -> {
                    val msg = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Timber.e("onFailed: packageName = ${archiveInfo.packageName}, msg = $msg")
                    onInstallFailed(
                        id = sessionId,
                        title = appLabel,
                        largeIcon = appIcon,
                    )
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getPackageUid(packageName: String): Int =
        runCatching {
            pmCompat.getPackageUid(packageName, 0, userId)
        }.getOrDefault(
            Process.INVALID_UID
        )

    private fun onProgressChanged(
        id: Int,
        title: String,
        largeIcon: Bitmap?,
        progress: Float
    ) {
        val p = (100 * progress).toInt()
        val notification = buildNotification(
            title = title,
            message = null,
            largeIcon = largeIcon,
            silent = true,
            ongoing = true
        ).apply {
            setProgress(100, p, false)
        }

        notify(id, notification.build())
    }

    private fun onInstallSucceeded(
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
            silent = true,
            ongoing = false
        ).apply {
            setContentIntent(intent)
        }

        notify(id, notification.build())
    }

    private fun onInstallFailed(id: Int, title: String, largeIcon: Bitmap) {
        val message = context.getString(R.string.message_install_fail)
        val notification = buildNotification(
            title = title,
            message = message,
            largeIcon = largeIcon,
            silent = false,
            ongoing = false
        )

        notify(id, notification.build())
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
        message: String?,
        largeIcon: Bitmap?,
        silent: Boolean,
        ongoing: Boolean,
    ) = NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_INSTALL)
        .setSmallIcon(R.drawable.launcher_outline)
        .setContentTitle(title)
        .setContentText(message)
        .setSilent(silent)
        .setOngoing(ongoing)
        .setGroup(GROUP_KEY)
        .setLargeIcon(largeIcon)

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: Notification) {
        val granted = if (BuildCompat.atLeastT) {
            PermissionCompat.checkPermissions(
                context,
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            ).allGranted
        } else {
            true
        }

        NotificationManagerCompat.from(this).apply {
            if (granted) notify(id, notification)
        }
    }

    companion object {
        private const val GROUP_KEY = "INSTALL_SERVICE_GROUP_KEY"
        private const val PARAM_ARCHIVE_URI = "ARCHIVE_URI"
        private const val PARAM_ARCHIVE_INFO = "ARCHIVE_PACKAGE_INFO"
        private val Intent.archiveUriOrNull: Uri? get() =
            parcelable(PARAM_ARCHIVE_URI)
        private val Intent.archiveInfoOrNull: PackageInfo? get() =
            parcelable(PARAM_ARCHIVE_INFO)

        fun start(
            context: Context,
            archiveUri: Uri,
            archiveInfo: PackageInfo
        ) {
            val intent = Intent(context, InstallService::class.java)
            intent.putExtra(PARAM_ARCHIVE_URI, archiveUri)
            intent.putExtra(PARAM_ARCHIVE_INFO, archiveInfo)

            context.startService(intent)
        }
    }
}