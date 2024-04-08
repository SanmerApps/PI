package dev.sanmer.hidden.compat.content.bundle

import android.content.pm.PackageParser
import android.util.DisplayMetrics
import dev.sanmer.hidden.compat.delegate.ContextDelegate

class DensitySplitConfig(
    val dpi: Dpi,
    filename: String,
    size: Long,
) : SplitConfig(
    filename,
    size
) {
    override val name = with(dpi) {
        val value = parseDpiEnum(this)
        "$value (DPI)"
    }

    override fun isRequired(): Boolean {
        val context = ContextDelegate.getContext()
        val densityDpi = context.resources.displayMetrics.densityDpi
        return dpi == parseDpiValue(densityDpi)
    }

    override fun isDisabled(): Boolean {
        return false
    }

    override fun isRecommended(): Boolean {
        return isRequired()
    }

    enum class Dpi {
        LDPI,
        MDPI,
        HDPI,
        XHDPI,
        XXHDPI,
        XXXHDPI
    }

    companion object {
        private fun parseDpiEnum(dpi: Dpi): Int {
            return when (dpi) {
                Dpi.LDPI -> DisplayMetrics.DENSITY_LOW
                Dpi.MDPI -> DisplayMetrics.DENSITY_MEDIUM
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
            val value = parseSplit(apk.splitName)?.uppercase() ?: return null
            val dpi = runCatching { Dpi.valueOf(value) }.getOrNull() ?: return null
            return DensitySplitConfig(dpi, filename, size)
        }
    }
}