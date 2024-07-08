package dev.sanmer.pi.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dev.sanmer.pi.R

object NotificationUtils {
    const val CHANNEL_ID_INSTALL = "INSTALL"
    const val NOTIFICATION_ID_INSTALL = 1024

    fun init(context: Context) {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_ID_INSTALL,
                context.getString(R.string.notification_name_install),
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        NotificationManagerCompat.from(context).apply {
            createNotificationChannels(channels)
            deleteUnlistedNotificationChannels(channels.map { it.id })
        }
    }
}