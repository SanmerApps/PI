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
import android.graphics.Bitmap
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class InstallService : LifecycleService(), KoinComponent, PackageInstallerDelegate.SessionCallback {
    private val suRepository by inject<SuRepository>()
    private val pm by lazy { suRepository.getPackageManager() }
    private val pi by lazy { suRepository.getPackageInstaller() }
    private val nm by lazy { NotificationManagerCompat.from(this) }

    private val taskState = MutableStateFlow<TaskState>(TaskState.None)

    private val logger = Logger.Android("InstallService")

    init {
        lifecycleScope.launch {
            taskState.onEach {
                when (it) {
                    is TaskState.Progress -> notify(it.id) {
                        setLargeIcon(it.icon)
                        setContentTitle(it.label)
                        setProgress(100, (100 * it.progress).toInt(), false)
                        setSilent(true)
                        setOngoing(true)
                        setGroup(GROUP_KEY)
                    }

                    is TaskState.Optimizing -> notify(it.id) {
                        setLargeIcon(it.packageInfo.icon)
                        setContentTitle(it.packageInfo.labelOrDefault)
                        setContentText(getString(R.string.optimizing))
                        setSilent(true)
                        setOngoing(true)
                        setGroup(GROUP_KEY)
                    }

                    is TaskState.Success -> notify(it.id) {
                        setLargeIcon(it.packageInfo.icon)
                        setContentTitle(it.packageInfo.labelOrDefault)
                        setContentText(it.packageInfo.versionDisplay())
                        setContentIntent(it.pendingIntent)
                        setSilent(true)
                        setAutoCancel(true)
                    }

                    is TaskState.Failure -> notify(it.id) {
                        setLargeIcon(it.packageInfo.icon)
                        setContentTitle(it.packageInfo.labelOrDefault)
                        setContentText(it.error.message ?: it.error.javaClass.name)
                        setSilent(false)
                        setOngoing(false)
                    }

                    else -> {}
                }
            }.filterIsInstance<TaskState.IoProgress>()
                .sample(500.milliseconds)
                .collect {
                    notify(it.id) {
                        setLargeIcon(it.packageInfo.icon)
                        setContentTitle(it.packageInfo.label)
                        setContentText(it.fileName)
                        setProgress(it.fileSize.toInt(), it.copied.toInt(), false)
                        setSilent(true)
                        setOngoing(true)
                        setGroup(GROUP_KEY)
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    private inline fun notify(id: Int, block: NotificationCompat.Builder.() -> Unit) {
        val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID_INSTALL)
        builder.setSmallIcon(R.drawable.launcher_outline)
        builder.block()
        nm.notify(id, builder.build())
    }

    override fun onCreate() {
        logger.d("onCreate")
        super.onCreate()
        pi.registerCallback(this, userId)

        val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)
            .setContentTitle(getText(R.string.installation_service))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
        ServiceCompat.startForeground(
            this,
            Const.NOTIFICATION_ID_INSTALL,
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
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
            runCatching {
                val fd = contentResolver.openAssetFileDescriptor(task.uri, "r") ?: return@launch
                fd.use { install(task, it) }
            }.onFailure { error ->
                logger.w(error)
                taskState.update {
                    TaskState.Failure(
                        id = task.id,
                        packageInfo = task.packageInfo,
                        error = error
                    )
                }
            }
            pendingPackageNames.remove(task.packageInfo.packageName)
            if (pendingPackageNames.isEmpty()) {
                delay(5.seconds)
                if (pendingPackageNames.isEmpty()) stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onProgressChanged(sessionId: Int, progress: Float) {
        val session = pi.getSessionInfo(sessionId) ?: return
        taskState.update {
            TaskState.Progress(
                id = sessionId,
                icon = session.appIcon,
                label = session.appLabel ?: "",
                progress = progress
            )
        }
    }

    private suspend fun install(
        task: Task,
        fd: AssetFileDescriptor
    ) {
        val params = createSessionParams()
        params.setAppIcon(task.packageInfo.iconOrDefault)
        params.setAppLabel(task.packageInfo.labelOrDefault)
        params.setAppPackageName(task.packageInfo.packageName)
        params.setOriginatingUri(task.uri)

        val ownerPackageName = suRepository.state.value
            .getOrElse({ it.ownerPackageName }) { "" }

        task.id = pi.createSession(
            params = params,
            installerPackageName = ownerPackageName.ifEmpty { task.installerPackageName },
            userId = userId
        )

        val session = pi.openSession(task.id)
        if (task.fileNames.isEmpty()) {
            session.writeFd(task.packageInfo.packageName, fd) { fileSize, copied ->
                taskState.update {
                    TaskState.IoProgress(
                        id = task.id,
                        packageInfo = task.packageInfo,
                        fileName = null,
                        fileSize = fileSize,
                        copied = copied
                    )
                }
            }
        } else {
            session.writeZip(task.fileNames, fd) { fileName, fileSize, copied ->
                taskState.update {
                    TaskState.IoProgress(
                        id = task.id,
                        packageInfo = task.packageInfo,
                        fileName = fileName,
                        fileSize = fileSize,
                        copied = copied
                    )
                }
            }
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
            taskState.update {
                TaskState.Optimizing(
                    id = task.id,
                    packageInfo = task.packageInfo
                )
            }
            optimize(
                packageName = task.packageInfo.packageName
            )
        }

        val launcher = pm.getLaunchIntentForPackage(task.packageInfo.packageName, userId)
        val pendingIntent = launcher?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        taskState.update {
            TaskState.Success(
                id = task.id,
                packageInfo = task.packageInfo,
                pendingIntent = pendingIntent
            )
        }
    }

    private fun optimize(packageName: String) {
        runCatching {
            pm.clearApplicationProfileData(packageName)
            pm.performDexOpt(packageName)
        }.onFailure {
            logger.w(it)
        }
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
        val packageInfo: PackageInfoLite,
        val installerPackageName: String
    ) : Parcelable {
        @IgnoredOnParcel
        var id = uri.hashCode()
    }

    private sealed interface TaskState {
        val id: Int

        object None : TaskState {
            override val id: Int = 0
        }

        class IoProgress(
            override val id: Int,
            val packageInfo: PackageInfoLite,
            val fileName: CharSequence?,
            val fileSize: Long,
            val copied: Long
        ) : TaskState

        class Progress(
            override val id: Int,
            val icon: Bitmap?,
            val label: CharSequence,
            val progress: Float
        ) : TaskState

        class Optimizing(
            override val id: Int,
            val packageInfo: PackageInfoLite,
        ) : TaskState

        class Success(
            override val id: Int,
            val packageInfo: PackageInfoLite,
            val pendingIntent: PendingIntent?
        ) : TaskState

        class Failure(
            override val id: Int,
            val packageInfo: PackageInfoLite,
            val error: Throwable
        ) : TaskState
    }

    companion object Default {
        private const val GROUP_KEY = "dev.sanmer.pi.INSTALL_SERVICE_GROUP_KEY"
        private const val EXTRA_TASK = "dev.sanmer.pi.extra.TASK"

        private fun Intent.putTask(value: Task) =
            putExtra(EXTRA_TASK, value)

        private inline val Intent.taskOrNull: Task?
            get() = parcelable(EXTRA_TASK)

        private val pendingPackageNames = mutableListOf<String>()

        fun start(
            context: Context,
            uri: Uri,
            fileNames: List<String>,
            packageInfo: PackageInfoLite,
            installerPackageName: String
        ) {
            fun start() {
                if (pendingPackageNames.contains(packageInfo.packageName)) return
                pendingPackageNames.add(packageInfo.packageName)
                context.startService(
                    Intent(context, InstallService::class.java).also {
                        it.putTask(
                            Task(
                                uri = uri,
                                fileNames = fileNames,
                                packageInfo = packageInfo,
                                installerPackageName = installerPackageName
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