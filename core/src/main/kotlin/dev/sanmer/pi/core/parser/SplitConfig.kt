package dev.sanmer.pi.core.parser

import android.content.res.Resources
import android.os.Build
import android.os.Parcelable
import android.util.DisplayMetrics
import dev.sanmer.pi.core.compat.ContextCompat
import dev.sanmer.pi.core.compat.LocaleListCompat
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
data class SplitConfig(
    val fileName: String,
    val sizeBytes: Long,
    val type: Type,
    val name: String,
    val configForSplit: String,
    val requiredSplitTypes: List<Type>,
    val isDisabled: Boolean,
    val isRecommended: Boolean
) : Parcelable {
    sealed interface Type : Parcelable, Comparable<Type> {
        infix fun like(other: Type) = when (this) {
            is Feature -> other is Feature
            is Abi -> other is Abi
            is Density -> other is Density
            is Language -> other is Language
            is Unspecified -> other is Unspecified
        }

        @Parcelize
        @JvmInline
        value class Feature(val splitName: String) : Type {
            override fun compareTo(other: Type) = -1
        }

        @Parcelize
        @JvmInline
        value class Abi(val abi: SplitConfig.Abi) : Type {
            override fun compareTo(other: Type) = when (other) {
                is Feature -> 1
                is Abi -> abi.compareTo(other.abi)
                else -> -1
            }
        }

        @Parcelize
        @JvmInline
        value class Density(val density: SplitConfig.Density) : Type {
            override fun compareTo(other: Type) = when (other) {
                is Feature, is Abi -> 1
                is Density -> density.compareTo(other.density)
                else -> -1
            }
        }

        @Parcelize
        @JvmInline
        value class Language(val locale: Locale) : Type {
            override fun compareTo(other: Type) = when (other) {
                is Feature, is Abi, is Density -> 1
                is Language -> locale.language.compareTo(other.locale.language)
                else -> -1
            }
        }

        @Parcelize
        @JvmInline
        value class Unspecified(val splitName: String) : Type {
            override fun compareTo(other: Type) = 1
        }
    }

    enum class Abi(val value: String) {
        ARM64_V8A("arm64-v8a"),
        ARMEABI_V7A("armeabi-v7a"),
        ARMEABI("armeabi"),
        X86("x86"),
        X86_64("x86_64");

        fun isRequired() = this == systemAbis[0]
        fun isEnabled() = this in systemAbis

        companion object Default {
            internal val systemAbis by lazy {
                Build.SUPPORTED_ABIS.map {
                    when (it) {
                        "arm64-v8a" -> ARM64_V8A
                        "armeabi-v7a" -> ARMEABI_V7A
                        "armeabi" -> ARMEABI
                        "x86" -> X86
                        "x86_64" -> X86_64
                        else -> throw IllegalArgumentException(it)
                    }
                }
            }

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

        fun isRequired() = this == systemDensity

        companion object Default {
            internal val systemDensity by lazy {
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
        internal val systemLocales by lazy {
            LocaleListCompat.getSystemLocales(ContextCompat.getContext())
        }

        val Locale.localizedDisplayName: String
            inline get() = getDisplayName(this)
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(this)
                    } else {
                        it.toString()
                    }
                }

        internal fun SplitConfigLite.typeName(): String {
            val value = splitName.removeSurrounding("${configForSplit}.", "")
            return value.removeSurrounding("config.", "")
        }

        fun from(
            splitConfig: SplitConfigLite,
            fileName: String,
            sizeBytes: Long
        ): SplitConfig {
            val requiredSplitTypes = splitConfig.requiredSplitTypes.map {
                when (it) {
                    "${splitConfig.splitName}__abi" -> Type.Abi(Abi.systemAbis[0])
                    "${splitConfig.splitName}__density" -> Type.Density(Density.systemDensity)
                    else -> Type.Unspecified(splitConfig.splitName)
                }
            }

            if (splitConfig.isFeatureSplit) return SplitConfig(
                fileName = fileName,
                sizeBytes = sizeBytes,
                type = Type.Feature(splitConfig.splitName),
                name = splitConfig.splitName,
                configForSplit = "",
                requiredSplitTypes = requiredSplitTypes,
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
                requiredSplitTypes = requiredSplitTypes,
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
                requiredSplitTypes = requiredSplitTypes,
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
                requiredSplitTypes = requiredSplitTypes,
                isDisabled = false,
                isRecommended = systemLocales.any { it.language == locale.language }
            )

            return SplitConfig(
                fileName = fileName,
                sizeBytes = sizeBytes,
                type = Type.Unspecified(splitConfig.splitName),
                name = splitConfig.splitName,
                configForSplit = splitConfig.configForSplit,
                requiredSplitTypes = requiredSplitTypes,
                isDisabled = false,
                isRecommended = true
            )
        }
    }
}