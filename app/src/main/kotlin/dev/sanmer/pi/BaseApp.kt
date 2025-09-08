package dev.sanmer.pi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dev.sanmer.pi.di.Factories
import dev.sanmer.pi.di.Repositories
import dev.sanmer.pi.di.ViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

abstract class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        HiddenApiBypass.setHiddenApiExemptions("")
        startKoin {
            androidLogger()
            androidContext(this@BaseApp)
            modules(Repositories, Factories, ViewModel)
        }
    }

    private fun createNotificationChannels(context: Context) {
        val channels = listOf(
            NotificationChannel(
                Const.CHANNEL_ID_INSTALL,
                context.getString(R.string.installation_service),
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        NotificationManagerCompat.from(context).apply {
            createNotificationChannels(channels)
            deleteUnlistedNotificationChannels(channels.map { it.id })
        }
    }
}