package dev.sanmer.pi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import dev.sanmer.pi.di.Repositories
import dev.sanmer.pi.di.ViewModel
import dev.sanmer.pi.ktx.dp
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        HiddenApiBypass.setHiddenApiExemptions("")
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(Repositories, ViewModel)
        }
    }

    override fun newImageLoader() =
        ImageLoader.Builder(this)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(40.dp, true, this@App))
            }
            .build()

    private fun createNotificationChannels(context: Context) {
        val channels = listOf(
            NotificationChannel(
                Const.CHANNEL_ID_PARSE,
                context.getString(R.string.parsing_service),
                NotificationManager.IMPORTANCE_HIGH
            ),
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