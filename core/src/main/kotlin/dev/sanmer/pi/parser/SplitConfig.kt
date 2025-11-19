package dev.sanmer.pi.parser

import android.os.Build
import android.os.Parcelable
import android.text.format.Formatter
import android.util.DisplayMetrics
import dev.sanmer.pi.ContextCompat
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Locale

sealed class SplitConfig : Parcelable {
    abstract val fileName: String
    abstract val sizeBytes: Long

    abstract val name: String
    abstract val configForSplit: String
    abstract val isRequired: Boolean
    abstract val isDisabled: Boolean
    abstract val isRecommended: Boolean

    val size: String by lazy {
        Formatter.formatFileSize(ContextCompat.context, sizeBytes)
    }

    @Parcelize
    data class Target(
        val abi: ABI,
        override val configForSplit: String,
        override val fileName: String,
        override val sizeBytes: Long
    ) : SplitConfig() {
        @IgnoredOnParcel
        override val name by lazy { abi.displayName }

        @IgnoredOnParcel
        override val isRequired by lazy { abi.isRequired }

        @IgnoredOnParcel
        override val isDisabled by lazy { !abi.isEnabled }
        override val isRecommended get() = isRequired
    }

    @Parcelize
    data class Density(
        val dpi: DPI,
        override val configForSplit: String,
        override val fileName: String,
        override val sizeBytes: Long
    ) : SplitConfig() {
        override val name get() = dpi.displayName

        @IgnoredOnParcel
        override val isRequired by lazy { dpi.isRequired }
        override val isDisabled get() = false
        override val isRecommended get() = isRequired
    }

    @Parcelize
    data class Language(
        val locale: Locale,
        override val configForSplit: String,
        override val fileName: String,
        override val sizeBytes: Long
    ) : SplitConfig() {
        override val name get() = locale.localizedDisplayName

        @IgnoredOnParcel
        override val isRequired by lazy { locale.language == Locale.getDefault().language }

        @IgnoredOnParcel
        override val isDisabled by lazy { locale !in Locale.getAvailableLocales() }
        override val isRecommended get() = isRequired
    }

    @Parcelize
    data class Feature(
        override val name: String,
        override val fileName: String,
        override val sizeBytes: Long
    ) : SplitConfig() {
        override val configForSplit get() = ""
        override val isRequired get() = false
        override val isDisabled get() = false
        override val isRecommended get() = true
    }

    @Parcelize
    data class Unspecified(
        override val configForSplit: String,
        override val fileName: String,
        override val sizeBytes: Long
    ) : SplitConfig() {
        override val name get() = fileName
        override val isRequired get() = false
        override val isDisabled get() = false
        override val isRecommended get() = true
    }

    enum class ABI(val value: String) {
        ARM64_V8A("arm64-v8a"),
        ARMEABI_V7A("armeabi-v7a"),
        ARMEABI("armeabi"),
        X86("x86"),
        X86_64("x86_64");

        val displayName: String
            inline get() = value

        val isRequired: Boolean
            inline get() = value == Build.SUPPORTED_ABIS[0]

        val isEnabled: Boolean
            inline get() = value in Build.SUPPORTED_ABIS

        companion object Default {
            fun valueOfOrNull(value: String): ABI? = try {
                valueOf(value)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }

    enum class DPI(val value: Int) {
        LDPI(DisplayMetrics.DENSITY_LOW),
        MDPI(DisplayMetrics.DENSITY_MEDIUM),
        TVDPI(DisplayMetrics.DENSITY_TV),
        HDPI(DisplayMetrics.DENSITY_HIGH),
        XHDPI(DisplayMetrics.DENSITY_XHIGH),
        XXHDPI(DisplayMetrics.DENSITY_XXHIGH),
        XXXHDPI(DisplayMetrics.DENSITY_XXXHIGH);

        val displayName: String
            inline get() = "${value}.dpi"

        val isRequired: Boolean
            get() {
                val context = ContextCompat.context
                val densityDpi = context.resources.displayMetrics.densityDpi
                return this == densityDpi.asDPI()
            }

        companion object Default {
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

    internal companion object Default {
        val Locale.localizedDisplayName: String
            inline get() = getDisplayName(this)
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(this)
                    } else {
                        it.toString()
                    }
                }

        fun SplitConfigLite.tagValue(): String {
            val value = splitName.removeSurrounding("${configForSplit}.", "")
            return value.removeSurrounding("config.", "")
        }

        operator fun invoke(
            lite: SplitConfigLite,
            fileName: String,
            sizeBytes: Long
        ): SplitConfig {
            if (lite.isFeatureSplit) {
                return Feature(
                    name = lite.splitName,
                    fileName = fileName,
                    sizeBytes = sizeBytes
                )
            }

            val value = lite.tagValue()
            val abi = ABI.valueOfOrNull(value.uppercase())
            if (abi != null) {
                return Target(
                    abi = abi,
                    configForSplit = lite.configForSplit,
                    fileName = fileName,
                    sizeBytes = sizeBytes
                )
            }

            val dpi = DPI.valueOfOrNull(value.uppercase())
            if (dpi != null) {
                return Density(
                    dpi = dpi,
                    configForSplit = lite.configForSplit,
                    fileName = fileName,
                    sizeBytes = sizeBytes
                )
            }

            val locale = Locale.forLanguageTag(value)
            if (locale.language.isNotEmpty()) {
                return Language(
                    locale = locale,
                    configForSplit = lite.configForSplit,
                    fileName = fileName,
                    sizeBytes = sizeBytes
                )
            }

            return Unspecified(
                configForSplit = lite.configForSplit,
                fileName = fileName,
                sizeBytes = sizeBytes
            )
        }
    }
}