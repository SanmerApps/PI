package dev.sanmer.pi

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.utils.extensions.dp
import dev.sanmer.pi.utils.timber.DebugTree
import dev.sanmer.pi.utils.timber.ReleaseTree
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import org.lsposed.hiddenapibypass.HiddenApiBypass
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

        HiddenApiBypass.addHiddenApiExemptions("")
        NotificationUtils.init(this)
    }

    override fun newImageLoader() =
        ImageLoader.Builder(this)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(40.dp, true, this@App))
            }
            .build()
}