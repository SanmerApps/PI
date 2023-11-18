package dev.sanmer.pi.app.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.app.NotificationManagerCompat
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.sanmer.pi.R

object NotificationUtils {
    const val CHANNEL_ID_INSTALL = "INSTALL"
    const val NOTIFICATION_ID_INSTALL = 1024

    fun init(context: Context) {
        val channels = listOf(
            NotificationChannel(CHANNEL_ID_INSTALL,
                context.getString(R.string.notification_name_install),
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        NotificationManagerCompat.from(context).apply {
            createNotificationChannels(channels)
            deleteUnlistedNotificationChannels(channels.map { it.id })
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun PermissionState() {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

        SideEffect {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}