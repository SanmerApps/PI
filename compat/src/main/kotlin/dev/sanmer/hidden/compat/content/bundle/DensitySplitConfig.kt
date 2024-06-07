package dev.sanmer.hidden.compat.content.bundle

import android.content.pm.PackageParser
import android.util.DisplayMetrics
import dev.sanmer.hidden.compat.delegate.ContextDelegate

class DensitySplitConfig(
    val dpi: Dpi,
    configForSplit: String?,
    filename: String,
    size: Long,
) : SplitConfig(
    configForSplit,
    filename,
    size
) {
    override val name by lazy {
        "${parseDpiEnum(dpi)}.dpi"
    }

    override val isRequired by lazy {
        val context = ContextDelegate.getContext()
        val densityDpi = context.resources.displayMetrics.densityDpi
        dpi == parseDpiValue(densityDpi)
    }

    override val isDisabled = false

    override val isRecommended by lazy {
        isRequired || isConfigForSplit
    }

    enum class Dpi {
        LDPI, // 120
        MDPI, // 160
        TVDPI, // 213
        HDPI, // 240
        XHDPI, // 320
        XXHDPI, // 480
        XXXHDPI, // 640
    }

    companion object {
        private fun parseDpiEnum(dpi: Dpi): Int {
            return when (dpi) {
                Dpi.LDPI -> DisplayMetrics.DENSITY_LOW
                Dpi.MDPI -> DisplayMetrics.DENSITY_MEDIUM
                Dpi.TVDPI -> DisplayMetrics.DENSITY_TV
                Dpi.HDPI -> DisplayMetrics.DENSITY_HIGH
                Dpi.XHDPI -> DisplayMetrics.DENSITY_XHIGH
                Dpi.XXHDPI -> DisplayMetrics.DENSITY_XXHIGH
                Dpi.XXXHDPI -> DisplayMetrics.DENSITY_XXXHIGH
            }
        }

        private fun parseDpiValue(dpi: Int): Dpi {
            return when {
                dpi <= DisplayMetrics.DENSITY_LOW -> Dpi.LDPI
                dpi <= DisplayMetrics.DENSITY_MEDIUM -> Dpi.MDPI
                dpi <= DisplayMetrics.DENSITY_TV -> Dpi.TVDPI
                dpi <= DisplayMetrics.DENSITY_HIGH -> Dpi.HDPI
                dpi <= DisplayMetrics.DENSITY_XHIGH -> Dpi.XHDPI
                dpi <= DisplayMetrics.DENSITY_XXHIGH -> Dpi.XXHDPI
                else -> Dpi.XXXHDPI
            }
        }

        fun build(
            apk: PackageParser.ApkLite,
            filename: String,
            size: Long
        ): DensitySplitConfig? {
            val value = parseSplit(apk)?.uppercase() ?: return null
            val dpi = runCatching { Dpi.valueOf(value) }.getOrNull() ?: return null
            return DensitySplitConfig(
                dpi = dpi,
                configForSplit = apk.configForSplit,
                filename = filename,
                size = size
            )
        }
    }
}