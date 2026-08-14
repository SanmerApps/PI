package dev.sanmer.pi.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.AssetFileDescriptor
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class InstallService : LifecycleService(), KoinComponent {
    private val suRepository by inject<SuRepository>()
    private val pm by lazy { suRepository.getPackageManager() }
    private val pi by lazy { suRepository.getPackageInstaller() }
    private val nm by lazy { NotificationManagerCompat.from(this) }

    private val runningMutex = Mutex()
    private val runningTask = mutableListOf<String>()

    private val logger = Logger.Android("InstallService")

    private suspend inline fun autoStopSelf(task: Task, block: (Task) -> Unit) {
        if (!runningMutex.withLock {
                runningTask.contains(task.packageInfo.packageName).also {
                    if (!it) runningTask.add(task.packageInfo.packageName)
                }
            }) {
            block(task)
            if (runningMutex.withLock {
                    runningTask.remove(task.packageInfo.packageName)
                    runningTask.isEmpty()
                }) {
                delay(5.seconds)
                if (runningMutex.withLock { runningTask.isEmpty() }) stopSelf()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        id: Int,
        builder: NotificationCompat.Builder,
        block: NotificationCompat.Builder.() -> NotificationCompat.Builder
    ) = nm.notify(id, builder.block().build())

    override fun onCreate() {
        logger.d("onCreate")
        super.onCreate()

        val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)
            .setContentTitle(getText(R.string.installation_service))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
        ServiceCompat.startForeground(
            this,
            builder.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    override fun onDestroy() {
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
            autoStopSelf(intent?.taskOrNull ?: return@launch) { task ->
                val builder = NotificationCompat.Builder(
                    applicationContext,
                    Const.CHANNEL_ID_INSTALL
                ).apply {
                    setSmallIcon(R.drawable.launcher_outline)
                    setLargeIcon(task.packageInfo.icon)
                    setContentTitle(task.packageInfo.labelOrDefault)
                    setOngoing(true)
                    setSilent(true)
                    setGroup(GROUP_KEY)
                }

                runCatching {
                    val fd = contentResolver.openAssetFileDescriptor(task.uri, "r") ?: return@launch
                    install(
                        task = task,
                        fd = fd,
                        startId = startId,
                        builder = builder
                    )
                    fd.close()
                    notify(startId, builder) {
                        setContentText(task.packageInfo.versionDisplay())
                        setContentIntent(launchApp(task.packageInfo.packageName))
                        setOngoing(false)
                        setSilent(false)
                        setAutoCancel(true)
                    }
                }.onFailure { error ->
                    logger.e(error)
                    notify(startId, builder) {
                        setContentText(error.message ?: error.javaClass.name)
                        setOngoing(false)
                        setSilent(false)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun install(
        task: Task,
        fd: AssetFileDescriptor,
        startId: Int,
        builder: NotificationCompat.Builder
    ) {
        val params = createSessionParams()
        params.setAppIcon(task.packageInfo.iconOrDefault)
        params.setAppLabel(task.packageInfo.labelOrDefault)
        params.setAppPackageName(task.packageInfo.packageName)
        params.setOriginatingUri(task.uri)

        val ownerPackageName = suRepository.state.value
            .getOrElse({ it.ownerPackageName }) { "" }

        val session = pi.openSession(
            pi.createSession(
                params = params,
                installerPackageName = ownerPackageName.ifEmpty { task.installerPackageName },
                userId = userId
            )
        )

        val period = 1.seconds
        var lastNotify = TimeSource.Monotonic.markNow()
        val sizeBytes = task.sizeBytes.toInt()
        val onProgress: (Long) -> Unit = { copied ->
            if (lastNotify.elapsedNow() >= period) {
                notify(startId, builder) {
                    setProgress(sizeBytes, copied.toInt(), false)
                }
                lastNotify = TimeSource.Monotonic.markNow()
            }
        }
        if (task.fileNames.isEmpty()) {
            session.writeFd(task.packageInfo.packageName, fd, onProgress)
        } else {
            session.writeZip(task.fileNames, fd, onProgress)
        }

        notify(startId, builder) {
            setProgress(0, 0, false)
            setContentText(getString(R.string.installing))
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
            notify(startId, builder) {
                setContentText(getString(R.string.optimizing))
            }
            optimize(task.packageInfo.packageName)
        }
    }

    private fun optimize(packageName: String) {
        runCatching {
            pm.clearApplicationProfileData(packageName)
            pm.performDexOpt(packageName)
        }.onFailure {
            logger.d(it)
        }
    }

    private fun launchApp(packageName: String): PendingIntent? {
        val intent = pm.getLaunchIntentForPackage(packageName, userId) ?: return null
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
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

    @Parcelize
    private data class Task(
        val uri: Uri,
        val fileNames: List<String>,
        val sizeBytes: Long,
        val packageInfo: PackageInfoLite,
        val installerPackageName: String
    ) : Parcelable

    companion object Default {
        private const val GROUP_KEY = "dev.sanmer.pi.INSTALL_SERVICE_GROUP_KEY"
        private const val EXTRA_TASK = "dev.sanmer.pi.extra.INSTALL_TASK"

        private fun Intent.putTask(value: Task) =
            putExtra(EXTRA_TASK, value)

        private inline val Intent.taskOrNull: Task?
            get() = parcelable(EXTRA_TASK)

        fun start(
            context: Context,
            uri: Uri,
            fileNames: List<String>,
            sizeBytes: Long,
            packageInfo: PackageInfoLite,
            installerPackageName: String
        ) {
            fun start() {
                context.startService(
                    Intent(context, InstallService::class.java).also {
                        it.putTask(
                            Task(
                                uri,
                                fileNames,
                                sizeBytes,
                                packageInfo,
                                installerPackageName
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