package dev.sanmer.pi.coil

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.ui.unit.Dp
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import me.zhanghai.android.appiconloader.AppIconLoader

class AppIconFetcher(
    private val applicationInfo: ApplicationInfo,
    private val appIconLoader: AppIconLoader
) : Fetcher {
    override suspend fun fetch() = ImageFetchResult(
        image = appIconLoader.loadIcon(applicationInfo).asImage(),
        isSampled = false,
        dataSource = DataSource.DISK,
    )

    class Factory(
        private val appIconLoader: AppIconLoader
    ) : Fetcher.Factory<PackageInfo> {
        constructor(
            context: Context,
            iconSize: Dp,
            shrinkNonAdaptiveIcons: Boolean
        ) : this(
            AppIconLoader(
                (iconSize.value * context.resources.displayMetrics.density).toInt(),
                shrinkNonAdaptiveIcons,
                context
            )
        )

        override fun create(
            data: PackageInfo,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            val applicationInfo = data.applicationInfo ?: return null
            return AppIconFetcher(
                applicationInfo = applicationInfo,
                appIconLoader = appIconLoader
            )
        }
    }
}