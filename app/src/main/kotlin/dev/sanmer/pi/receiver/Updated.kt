package dev.sanmer.pi.receiver

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.sanmer.pi.Const
import dev.sanmer.pi.R

class Updated : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action ?: return) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                context.deleteExternalCacheDir()
                context.notifyUpdated()
            }
        }
    }

    private fun Context.deleteExternalCacheDir() {
        externalCacheDir?.deleteRecursively()
    }

    @SuppressLint("MissingPermission")
    private fun Context.notifyUpdated() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pending = PendingIntent.getActivity(this, 0, launchIntent, flag)
        val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID_INSTALL)
            .setSmallIcon(R.drawable.launcher_outline)
            .setContentIntent(pending)
            .setContentTitle(getText(R.string.updated_title))
            .setContentText(getText(R.string.updated_text))
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).apply {
            notify(builder.hashCode(), builder.build())
        }
    }
}