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
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.Const
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.Logger
import dev.sanmer.pi.PackageInfoCompat.orEmpty
import dev.sanmer.pi.PackageParserCompat
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.MediaStoreCompat.copyToFile
import dev.sanmer.pi.compat.MediaStoreCompat.getOwnerPackageNameForUri
import dev.sanmer.pi.compat.MediaStoreCompat.getPathForUri
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.ktx.temp
import dev.sanmer.pi.repository.ServiceRepository
import dev.sanmer.pi.ui.InstallActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import java.io.File
import kotlin.time.Duration.Companion.seconds

class ParseService : LifecycleService(), KoinComponent {
    private val serviceRepository by inject<ServiceRepository>()

    private val nm by lazy { NotificationManagerCompat.from(this) }
    private val pm by lazy { serviceRepository.getPackageManager() }
    private val aom by lazy { serviceRepository.getAppOpsManager() }

    private val logger = Logger.Android("ParseService")

    init {
        lifecycleScope.launch {
            while (currentCoroutineContext().isActive) {
                if (pendingUris.isEmpty()) stopSelf()
                delay(5.seconds)
            }
        }
    }

    override fun onCreate() {
        logger.d("onCreate")
        super.onCreate()
        setForeground()
    }

    override fun onTimeout(startId: Int) {
        stopSelf(startId)
        super.onTimeout(startId)
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        logger.d("onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch(Dispatchers.IO) {
            val uri = intent?.data ?: return@launch
            val state = serviceRepository.state.first { !it.isPending }
            if (state.isSucceed) {
                parse(uri)
                pendingUris.remove(uri)
            } else {
                notifyFailure(
                    id = Const.NOTIFICATION_ID_PARSE,
                    title = getText(R.string.parsing_service),
                    text = getText(R.string.settings_service_not_running)
                )
                pendingUris.clear()
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun parse(uri: Uri) = withContext(Dispatchers.IO) {
        val packageName = getOwnerPackageNameForUri(uri)
        val sourceInfo = packageName?.let(::getPackageInfo).orEmpty()
        val path = File(getPathForUri(uri))
        logger.i("from: $packageName, path: $path")

        notifyParsing(
            id = uri.hashCode(),
            filename = path.name
        )

        val archivePath = externalCacheDir.temp()
        copyToFile(uri, archivePath)

        val isSucceed = if (parseApk(archivePath, sourceInfo)) true
        else parseZip(archivePath, sourceInfo)

        if (isSucceed) {
            nm.cancel(uri.hashCode())
        } else {
            notifyFailure(
                id = uri.hashCode(),
                title = path.name,
                text = getText(R.string.message_parsing_failed)
            )
        }
    }

    private fun parseApk(archivePath: File, sourceInfo: PackageInfo): Boolean {
        return PackageParserCompat.parsePackage(archivePath, 0)?.let { pi ->
            val isPIUpdate = pi.packageName == BuildConfig.APPLICATION_ID
                    && pi.longVersionCode > BuildConfig.VERSION_CODE
            val isSelfUpdate = pi.packageName == sourceInfo.packageName
                    && pi.longVersionCode > sourceInfo.longVersionCode
            val isAuthorizedUpdate = sourceInfo.isAuthorized()
                    && pi.longVersionCode > getPackageInfo(pi.packageName).longVersionCode

            if (isPIUpdate || isSelfUpdate || isAuthorizedUpdate) {
                InstallService.apk(
                    context = applicationContext,
                    archivePath = archivePath,
                    archiveInfo = pi,
                    userId = userId,
                    sourceInfo = sourceInfo
                )
            } else {
                InstallActivity.apk(
                    context = applicationContext,
                    archivePath = archivePath,
                    archiveInfo = pi,
                    sourceInfo = sourceInfo
                )
            }
        } != null
    }

    private fun parseZip(archivePath: File, sourceInfo: PackageInfo): Boolean {
        val archiveDir = externalCacheDir.temp().apply { mkdirs() }
        val isSucceed = PackageParserCompat.parseAppBundle(archivePath, 0, archiveDir)?.let { bi ->
            InstallActivity.appBundle(
                context = applicationContext,
                archivePath = archiveDir,
                archiveInfo = bi.baseInfo,
                splitConfigs = bi.splitConfigs,
                sourceInfo = sourceInfo
            )
        } != null

        archivePath.delete()
        return if (!isSucceed) {
            val isNew = archiveDir.listFiles { f -> f.extension == "apk" }?.firstOrNull()
                ?.renameTo(archivePath) == true
            if (isNew) parseApk(archivePath, sourceInfo) else false
            archiveDir.deleteRecursively()
        } else true
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        return runCatching {
            pm.getPackageInfo(packageName, 0, userId)
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
            .setContentTitle(filename)
            .setContentText(getString(R.string.message_parsing))
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

    private fun notify(id: Int, notification: Notification) {
        if (
            !BuildCompat.atLeastT
            || PermissionCompat.checkPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        ) nm.notify(id, notification)
    }

    companion object Default {
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
