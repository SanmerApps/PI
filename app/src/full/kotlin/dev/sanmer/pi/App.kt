package dev.sanmer.pi

import coil.ImageLoader
import coil.ImageLoaderFactory
import dev.sanmer.pi.di.Factories
import dev.sanmer.pi.di.Repositories
import dev.sanmer.pi.ktx.dp
import dev.sanmer.pi.ui.di.Navigation
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer

class App : BaseApp(Factories, Repositories, Navigation), ImageLoaderFactory {
    override fun newImageLoader() =
        ImageLoader.Builder(this)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(40.dp, true, this@App))
            }
            .build()
}