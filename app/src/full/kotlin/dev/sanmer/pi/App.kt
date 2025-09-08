package dev.sanmer.pi

import coil.ImageLoader
import coil.ImageLoaderFactory
import dev.sanmer.pi.ktx.dp
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer

class App : BaseApp(), ImageLoaderFactory {
    override fun newImageLoader() =
        ImageLoader.Builder(this)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(40.dp, true, this@App))
            }
            .build()
}