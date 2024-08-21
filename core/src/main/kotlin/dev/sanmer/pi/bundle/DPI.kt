package dev.sanmer.pi.bundle

import android.util.DisplayMetrics
import dev.sanmer.pi.ContextCompat

enum class DPI(val value: Int) {
    LDPI(DisplayMetrics.DENSITY_LOW),
    MDPI(DisplayMetrics.DENSITY_MEDIUM),
    TVDPI(DisplayMetrics.DENSITY_TV),
    HDPI(DisplayMetrics.DENSITY_HIGH),
    XHDPI(DisplayMetrics.DENSITY_XHIGH),
    XXHDPI(DisplayMetrics.DENSITY_XXHIGH),
    XXXHDPI(DisplayMetrics.DENSITY_XXXHIGH);

    companion object Default {
        val DPI.displayName: String
            inline get() = "${value}.dpi"

        val DPI.isRequired: Boolean
            get() {
                val context = ContextCompat.getContext()
                val densityDpi = context.resources.displayMetrics.densityDpi
                return this == densityDpi.asDPI()
            }

        private fun Int.asDPI() = when {
            this <= DisplayMetrics.DENSITY_LOW -> LDPI
            this <= DisplayMetrics.DENSITY_MEDIUM -> MDPI
            this <= DisplayMetrics.DENSITY_TV -> TVDPI
            this <= DisplayMetrics.DENSITY_HIGH -> HDPI
            this <= DisplayMetrics.DENSITY_XHIGH -> XHDPI
            this <= DisplayMetrics.DENSITY_XXHIGH -> XXHDPI
            else -> XXXHDPI
        }

        fun valueOfOrNull(value: String): DPI? = try {
            valueOf(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}