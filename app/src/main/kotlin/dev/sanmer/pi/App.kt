package dev.sanmer.pi

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import dev.sanmer.pi.app.utils.NotificationUtils
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

        ServiceManagerCompat.setHiddenApiExemptions("")
        NotificationUtils.init(this)
    }

    override fun newImageLoader() =
        ImageLoader.Builder(this)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(40.dp, true, this@App))
            }
            .build()

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