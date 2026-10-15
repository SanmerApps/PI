package dev.sanmer.pi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dev.sanmer.pi.coil.AppIconFetcher
import dev.sanmer.pi.coil.AppIconKeyer
import dev.sanmer.pi.di.ViewModelsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        HiddenApiBypass.setHiddenApiExemptions("")
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.INFO)
            androidContext(this@App)
            modules(ViewModelsModule)
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

    override fun newImageLoader(context: Context) = ImageLoader.Builder(context)
        .crossfade(true)
        .components {
            add(AppIconKeyer())
            add(AppIconFetcher.Factory(context, 48.dp, false))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image"))
                .maxSizePercent(0.02)
                .build()
        }
        .build()
}