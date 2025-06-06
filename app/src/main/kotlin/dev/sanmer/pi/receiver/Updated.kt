package dev.sanmer.pi.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.EntryPointAccessors
import dev.sanmer.pi.Const
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class Updated : BroadcastReceiver() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pending = goAsync()
                coroutineScope.launch {
                    context.deleteExternalCacheDir()
                    context.performDexOpt()
                    pending.finish()
                }
            }
        }
    }

    private suspend fun Context.deleteExternalCacheDir() = withContext(Dispatchers.IO) {
        externalCacheDir?.deleteRecursively()
    }

    private suspend fun Context.performDexOpt() = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BroadcastReceiverEntryPoint::class.java
        )

        val serviceRepository = entryPoint.serviceRepository()
        val state = serviceRepository.state.first { !it.isPending }

        notifyUpdated()
        if (state.isSucceed) {
            runCatching {
                Timber.d("optimize $packageName")
                val pm = serviceRepository.getPackageManager()
                pm.clearApplicationProfileData(packageName)
                pm.performDexOpt(packageName).also {
                    if (!it) Timber.e("Failed to optimize $packageName")
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to optimize $packageName")
            }
        }
    }

    private fun Context.notifyUpdated() {
        if (
            BuildCompat.atLeastT
            && !PermissionCompat.checkPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        ) return

        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pending = PendingIntent.getActivity(this, 0, intent, flag)
        val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)
            .setContentIntent(pending)
            .setContentTitle(getText(R.string.updated_title))
            .setContentText(getText(R.string.updated_text))
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).apply {
            notify(Const.NOTIFICATION_ID_UPDATED, builder.build())
        }
    }
}