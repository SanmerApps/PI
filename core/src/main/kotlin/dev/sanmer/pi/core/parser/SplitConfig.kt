package dev.sanmer.pi.core.parser

import android.content.res.Resources
import android.os.Build
import android.os.Parcelable
import android.util.DisplayMetrics
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
data class SplitConfig(
    val fileName: String,
    val sizeBytes: Long,
    val type: Type,
    val name: String,
    val configForSplit: String,
    val isDisabled: Boolean,
    val isRecommended: Boolean
) : Parcelable {
    sealed interface Type : Parcelable, Comparable<Type> {
        @Parcelize
        data object Feature : Type {
            override fun compareTo(other: Type) = -1
        }

        @Parcelize
        data class Abi(val abi: SplitConfig.Abi) : Type {
            override fun compareTo(other: Type) = when (other) {
                Feature -> 1
                is Abi -> abi.compareTo(other.abi)
                else -> -1
            }
        }

        @Parcelize
        data class Density(val density: SplitConfig.Density) : Type {
            override fun compareTo(other: Type) = when (other) {
                Feature, is Abi -> 1
                is Density -> density.compareTo(other.density)
                else -> -1
            }
        }

        @Parcelize
        data class Language(val locale: Locale) : Type {
            override fun compareTo(other: Type) = when (other) {
                Feature, is Abi, is Density -> 1
                is Language -> locale.language.compareTo(other.locale.language)
                else -> -1
            }
        }

        @Parcelize
        data object Unspecified : Type {
            override fun compareTo(other: Type) = 1
        }
    }

    enum class Abi(val value: String) {
        ARM64_V8A("arm64-v8a"),
        ARMEABI_V7A("armeabi-v7a"),
        ARMEABI("armeabi"),
        X86("x86"),
        X86_64("x86_64");

        fun isRequired() = value == Build.SUPPORTED_ABIS[0]
        fun isEnabled() = value in Build.SUPPORTED_ABIS

        companion object Default {
            fun valueOfOrNull(value: String) = try {
                valueOf(value)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }

    enum class Density(val value: String) {
        LDPI("${DisplayMetrics.DENSITY_LOW} dpi"),
        MDPI("${DisplayMetrics.DENSITY_MEDIUM} dpi"),
        TVDPI("${DisplayMetrics.DENSITY_TV} dpi"),
        HDPI("${DisplayMetrics.DENSITY_HIGH} dpi"),
        XHDPI("${DisplayMetrics.DENSITY_XHIGH} dpi"),
        XXHDPI("${DisplayMetrics.DENSITY_XXHIGH} dpi"),
        XXXHDPI("${DisplayMetrics.DENSITY_XXXHIGH} dpi");

        fun isRequired() = this == screenDensity

        companion object Default {
            val screenDensity by lazy {
                val densityDpi = Resources.getSystem().displayMetrics.densityDpi
                when {
                    densityDpi <= DisplayMetrics.DENSITY_LOW -> LDPI
                    densityDpi <= DisplayMetrics.DENSITY_MEDIUM -> MDPI
                    densityDpi <= DisplayMetrics.DENSITY_TV -> TVDPI
                    densityDpi <= DisplayMetrics.DENSITY_HIGH -> HDPI
                    densityDpi <= DisplayMetrics.DENSITY_XHIGH -> XHDPI
                    densityDpi <= DisplayMetrics.DENSITY_XXHIGH -> XXHDPI
                    else -> XXXHDPI
                }
            }

            fun valueOfOrNull(value: String) = try {
                valueOf(value)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }

    companion object Default {
        val Locale.localizedDisplayName: String
            inline get() = getDisplayName(this)
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(this)
                    } else {
                        it.toString()
                    }
                }

        fun SplitConfigLite.typeName(): String {
            val value = splitName.removeSurrounding("${configForSplit}.", "")
            return value.removeSurrounding("config.", "")
        }

        fun from(
            splitConfig: SplitConfigLite,
            fileName: String,
            sizeBytes: Long
        ): SplitConfig {
            if (splitConfig.isFeatureSplit) return SplitConfig(
                fileName = fileName,
                sizeBytes = sizeBytes,
                type = Type.Feature,
                name = splitConfig.splitName,
                configForSplit = "",
                isDisabled = false,
                isRecommended = true
            )

            val type = splitConfig.typeName()
            val abi = Abi.valueOfOrNull(type.uppercase())
            if (abi != null) return SplitConfig(
                fileName = fileName,
                sizeBytes = sizeBytes,
                type = Type.Abi(abi),
                name = abi.value,
                configForSplit = splitConfig.configForSplit,
                isDisabled = !abi.isEnabled(),
                isRecommended = abi.isRequired()
            )

            val density = Density.valueOfOrNull(type.uppercase())
            if (density != null) return SplitConfig(
                fileName = fileName,
                sizeBytes = sizeBytes,
                type = Type.Density(density),
                name = density.value,
                configForSplit = splitConfig.configForSplit,
                isDisabled = false,
                isRecommended = density.isRequired()
            )

            val locale = Locale.forLanguageTag(type)
            if (locale.language.isNotEmpty()) return SplitConfig(
                fileName = fileName,
                sizeBytes = sizeBytes,
                type = Type.Language(locale),
                name = locale.localizedDisplayName,
                configForSplit = splitConfig.configForSplit,
                isDisabled = locale !in Locale.getAvailableLocales(),
                isRecommended = locale.language == Locale.getDefault().language
            )

            return SplitConfig(
                fileName = fileName,
                sizeBytes = sizeBytes,
                type = Type.Unspecified,
                name = splitConfig.splitName,
                configForSplit = splitConfig.configForSplit,
                isDisabled = false,
                isRecommended = true
            )
        }
    }
}