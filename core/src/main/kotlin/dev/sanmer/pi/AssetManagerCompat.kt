package dev.sanmer.pi

import android.content.res.ApkAssets
import android.content.res.AssetManager
import android.content.res.AssetManagerHidden
import android.content.res.Resources
import dev.rikka.tools.refine.Refine

object AssetManagerCompat {
    fun new(): AssetManager = Refine.unsafeCast(AssetManagerHidden())

    fun AssetManager.setApkAssets(apkAssets: Array<ApkAssets>, invalidateCaches: Boolean) =
        Refine.unsafeCast<AssetManagerHidden>(this)
            .setApkAssets(apkAssets, invalidateCaches)

    val AssetManager.resources: Resources
        get() {
            val sys = Resources.getSystem()
            @Suppress("DEPRECATION")
            return Resources(this, sys.displayMetrics, sys.configuration)
        }
}