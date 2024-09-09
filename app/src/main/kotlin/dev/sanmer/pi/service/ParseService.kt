package dev.sanmer.pi.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.Compat
import dev.sanmer.pi.Const
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.PackageParserCompat
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.MediaStoreCompat.copyToFile
import dev.sanmer.pi.compat.MediaStoreCompat.getOwnerPackageNameForUri
import dev.sanmer.pi.compat.MediaStoreCompat.getPathForUri
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.InstallActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class ParseService : LifecycleService() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val nm by lazy { NotificationManagerCompat.from(this) }
    private val pm by lazy { Compat.getPackageManager() }
    private val aom by lazy { Compat.getAppOpsService() }

    init {
        lifecycleScope.launch {
            while (currentCoroutineContext().isActive) {
                if (pendingUris.isEmpty()) stopSelf()
                delay(5.seconds)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setForeground()
    }

    override fun onTimeout(startId: Int) {
        stopSelf(startId)
        super.onTimeout(startId)
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch(Dispatchers.IO) {
            val uri = intent?.data ?: return@launch
            val userPreferences = userPreferencesRepository.data.first()

            if (!Compat.init(userPreferences.provider)) {
                notifyFailure(
                    id = Const.NOTIFICATION_ID_PARSE,
                    title = getText(R.string.parsing_service),
                    text = getText(R.string.message_invalid_provider)
                )
                stopSelf()
                pendingUris.clear()
                return@launch
            }

            val packageName = getOwnerPackageNameForUri(uri)
            val sourceInfo = packageName?.let(::getPackageInfo)
            Timber.i("from: $packageName")

            val path = File(getPathForUri(uri))
            Timber.i("path: $path")

            notifyParsing(
                id = uri.hashCode(),
                filename = path.name
            )

            val archivePath = File(externalCacheDir, UUID.randomUUID().toString())
            copyToFile(uri, archivePath)

            PackageParserCompat.parsePackage(archivePath, 0)?.let { pi ->
                if (sourceInfo?.isAuthorized() == true) {
                    InstallService.apk(
                        context = applicationContext,
                        archivePath = archivePath,
                        archiveInfo = pi
                    )
                } else {
                    InstallActivity.apk(
                        context = applicationContext,
                        archivePath = archivePath,
                        archiveInfo = pi,
                        sourceInfo = sourceInfo
                    )
                }

                nm.cancel(uri.hashCode())
                pendingUris.remove(uri)
                return@launch
            }

            val archiveDir = File(externalCacheDir, UUID.randomUUID().toString()).apply { mkdirs() }
            PackageParserCompat.parseAppBundle(archivePath, 0, archiveDir)?.let { bi ->
                InstallActivity.appBundle(
                    context = applicationContext,
                    archivePath = archiveDir,
                    archiveInfo = bi.baseInfo,
                    splitConfigs = bi.splitConfigs,
                    sourceInfo = sourceInfo
                )

                archivePath.delete()
                nm.cancel(uri.hashCode())
                pendingUris.remove(uri)
                return@launch
            }

            nm.cancel(uri.hashCode())
            pendingUris.remove(uri)
            archiveDir.deleteRecursively()
            notifyFailure(
                id = uri.hashCode(),
                title = path.name,
                text = getText(R.string.message_invalid_package)
            )
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        return runCatching {
            pm.getPackageInfo(
                packageName, 0, userId
            )
        }.getOrNull() ?: PackageInfo()
    }

    private fun PackageInfo.isAuthorized() = aom.checkOpNoThrow(
        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
        packageInfo = this
    ).isAllowed

    private fun setForeground() {
        val notification = newNotificationBuilder()
            .setContentTitle(getText(R.string.parsing_service))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()

        ServiceCompat.startForeground(
            this,
            Const.NOTIFICATION_ID_PARSE,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun notifyParsing(
        id: Int,
        filename: String
    ) {
        val notification = newNotificationBuilder()
            .setContentText(getString(R.string.message_parsing_package, filename))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .build()

        notify(id, notification)
    }

    private fun notifyFailure(
        id: Int,
        title: CharSequence?,
        text: CharSequence
    ) {
        val notification = newNotificationBuilder()
            .setContentTitle(title)
            .setContentText(text)
            .build()

        notify(id, notification)
    }

    private fun newNotificationBuilder() =
        NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID_PARSE)
            .setSmallIcon(R.drawable.launcher_outline)

    @Throws(SecurityException::class)
    private fun notify(id: Int, notification: Notification) {
        val granted = if (BuildCompat.atLeastT) {
            PermissionCompat.checkPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

        if (granted) nm.notify(id, notification)
    }

    companion object {
        private const val GROUP_KEY = "dev.sanmer.pi.PARSE_SERVICE_GROUP_KEY"

        private val pendingUris = mutableListOf<Uri>()

        fun start(context: Context, uri: Uri) {
            pendingUris.add(uri)
            context.startService(
                Intent(context, ParseService::class.java).also {
                    it.data = uri
                    it.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            )
        }
    }
}