package dev.sanmer.pi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import dev.sanmer.pi.ktx.dp
import dev.sanmer.su.ServiceManagerCompat
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import timber.log.Timber

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannels(this)
        ServiceManagerCompat.setHiddenApiExemptions("")
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
                Const.CHANNEL_ID_INSTALL,
                context.getString(R.string.notification_name_install),
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        NotificationManagerCompat.from(context).apply {
            createNotificationChannels(channels)
            deleteUnlistedNotificationChannels(channels.map { it.id })
        }
    }

    class DebugTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            super.log(priority, "<PI_DEBUG>$tag", message, t)
        }

        override fun createStackElementTag(element: StackTraceElement): String {
            return super.createStackElementTag(element) + "(L${element.lineNumber})"
        }
    }

    class ReleaseTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            super.log(priority, "<PI_REL>$tag", message, t)
        }
    }
}