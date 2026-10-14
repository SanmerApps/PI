package dev.sanmer.pi.coil

import android.content.pm.PackageInfo
import coil3.key.Keyer
import coil3.request.Options
import me.zhanghai.android.appiconloader.AppIconLoader

class AppIconKeyer : Keyer<PackageInfo> {
    override fun key(
        data: PackageInfo,
        options: Options
    ) = AppIconLoader.getIconKey(data, options.context)
}