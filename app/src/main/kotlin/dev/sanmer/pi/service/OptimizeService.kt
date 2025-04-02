package dev.sanmer.pi.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.Const
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.ktx.dp
import dev.sanmer.pi.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class OptimizeService : LifecycleService() {
    @Inject
    lateinit var serviceRepository: ServiceRepository

    private val appIconLoader by lazy { AppIconLoader(45.dp, true, this) }
    private val nm by lazy { NotificationManagerCompat.from(this) }
    private val pm by lazy { serviceRepository.getPackageManager() }

    init {
        lifecycleScope.launch {
            while (currentCoroutineContext().isActive) {
                if (pendingPackages.isEmpty()) stopSelf()
                delay(5.seconds)
            }
        }
    }

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()
        setForeground()
    }

    override fun onTimeout(startId: Int) {
        stopSelf(startId)
        super.onTimeout(startId)
    }

    override fun onDestroy() {
        jobStateFlow.update { JobState.Empty }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        Timber.d("onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch(Dispatchers.IO) {
            val packageName = intent?.`package` ?: return@launch
            val state = serviceRepository.state.first { !it.isPending }
            if (state.isSucceed) {
                getPackageInfoOrNull(packageName)?.let { optimize(it) }
            }
            pendingPackages.remove(packageName)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun optimize(packageInfo: PackageInfo) = withContext(Dispatchers.IO) {
        Timber.d("optimize ${packageInfo.packageName}")
        val uid = requireNotNull(packageInfo.applicationInfo?.uid) { "Expect UID" }
        val appIcon = packageInfo.applicationInfo?.let(appIconLoader::loadIcon)
        val appLabel = packageInfo.applicationInfo?.loadLabel(packageManager)
            ?: packageInfo.packageName

        jobStateFlow.update { JobState.Running(packageInfo.packageName) }
        notifyOptimizing(uid, appLabel, appIcon)

        runCatching {
            pm.clearApplicationProfileData(packageInfo.packageName)
            if (pm.performDexOpt(packageInfo.packageName)) {
                Timber.d("${packageInfo.packageName} optimized")
                jobStateFlow.update { JobState.Success(packageInfo.packageName) }
                nm.cancel(uid)
            } else {
                jobStateFlow.update { JobState.Failure(packageInfo.packageName, null) }
                notifyFailure(uid, appLabel, appIcon)
            }

        }.onFailure { error ->
            Timber.e(error)
            jobStateFlow.update { JobState.Failure(packageInfo.packageName, error) }
            notifyFailure(uid, appLabel, appIcon)
        }
    }

    private fun getPackageInfoOrNull(packageName: String) =
        runCatching {
            pm.getPackageInfo(packageName, 0, userId)
        }.getOrNull()

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

    private fun notifyFailure(
        id: Int,
        appLabel: CharSequence,
        appIcon: Bitmap?
    ) {
        val notification = newNotificationBuilder()
            .setLargeIcon(appIcon)
            .setContentTitle(appLabel)
            .setContentText(getText(R.string.message_optimize_failed))
            .build()

        notify(id, notification)
    }

    private fun setForeground() {
        val notification = newNotificationBuilder()
            .setContentTitle(getText(R.string.optimize_service))
            .setSilent(true)
            .setOngoing(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()

        ServiceCompat.startForeground(
            this,
            Const.NOTIFICATION_ID_OPTIMIZE,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun newNotificationBuilder() =
        NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID_OPTIMIZE)
            .setSmallIcon(R.drawable.launcher_outline)

    private fun notify(id: Int, notification: Notification) {
        val granted = if (BuildCompat.atLeastT) {
            PermissionCompat.checkPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

        if (granted) nm.notify(id, notification)
    }

    sealed class JobState(val packageName: String) {
        data object Empty : JobState("")
        class Pending(packageName: String) : JobState(packageName)
        class Running(packageName: String) : JobState(packageName)
        class Success(packageName: String) : JobState(packageName)
        class Failure(packageName: String, val error: Throwable?) : JobState(packageName)

        val isRunning inline get() = this is Pending || this is Running
        val isSucceed inline get() = this is Success
    }

    companion object Default {
        private const val GROUP_KEY = "dev.sanmer.pi.OPTIMIZE_SERVICE_GROUP_KEY"

        private val pendingPackages = mutableListOf<String>()

        private val jobStateFlow = MutableStateFlow<JobState>(JobState.Empty)

        fun getJobState(packageName: String): Flow<JobState> {
            return jobStateFlow.filter { it.packageName == packageName }
        }

        fun start(context: Context, packageName: String) {
            pendingPackages.add(packageName)
            jobStateFlow.update { JobState.Pending(packageName) }
            context.startService(
                Intent(context, OptimizeService::class.java).also {
                    it.`package` = packageName
                }
            )
        }
    }
}