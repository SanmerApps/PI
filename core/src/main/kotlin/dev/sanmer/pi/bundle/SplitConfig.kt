package dev.sanmer.pi.bundle

import android.content.pm.PackageParser
import android.os.Parcelable
import android.text.format.Formatter
import dev.sanmer.pi.ContextCompat
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.Locale

sealed class SplitConfig : Parcelable {
    abstract val file: File
    abstract val name: String
    abstract val configForSplit: String?
    abstract val isRequired: Boolean
    abstract val isDisabled: Boolean
    abstract val isRecommended: Boolean

    val displaySize: String
        get() = Formatter.formatFileSize(ContextCompat.getContext(), file.length())

    val isConfigForSplit: Boolean
        inline get() = !configForSplit.isNullOrEmpty()

    override fun toString(): String {
        return "name = ${name}, " +
                "required = ${isRequired}, " +
                "disabled = ${isDisabled}, " +
                "recommended = $isRecommended"
    }

    @Parcelize
    data class Target(
        val abi: ABI,
        override val configForSplit: String?,
        override val file: File
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
        override val configForSplit: String?,
        override val file: File
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
        override val configForSplit: String?,
        override val file: File
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
        override val file: File,
    ) : SplitConfig() {
        override val configForSplit get() = null
        override val isRequired get() = false
        override val isDisabled get() = false
        override val isRecommended get() = true
    }

    @Parcelize
    data class Unspecified(
        override val configForSplit: String?,
        override val file: File,
    ) : SplitConfig() {
        override val name get() = file.name
        override val isRequired get() = false
        override val isDisabled get() = false
        override val isRecommended get() = true
    }

    companion object Default {
        private val Locale.localizedDisplayName: String
            inline get() = getDisplayName(this)
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(this)
                    } else {
                        it.toString()
                    }
                }

        private fun PackageParser.ApkLite.splitName(): String {
            val splitName = splitName.removeSurrounding("${configForSplit}.", "")
            return splitName.removeSurrounding("config.", "")
        }

        fun parse(apk: PackageParser.ApkLite, file: File): SplitConfig {
            if (apk.isFeatureSplit) {
                return Feature(
                    name = apk.splitName,
                    file = file
                )
            }

            val value = apk.splitName()
            val abi = ABI.valueOfOrNull(value.uppercase())
            if (abi != null) {
                return Target(
                    abi = abi,
                    configForSplit = apk.configForSplit,
                    file = file
                )
            }

            val dpi = DPI.valueOfOrNull(value.uppercase())
            if (dpi != null) {
                return Density(
                    dpi = dpi,
                    configForSplit = apk.configForSplit,
                    file = file
                )
            }

            val locale = Locale.forLanguageTag(value)
            if (locale.language.isNotEmpty()) {
                return Language(
                    locale = locale,
                    configForSplit = apk.configForSplit,
                    file = file
                )
            }

            return Unspecified(
                configForSplit = apk.configForSplit,
                file = file
            )
        }
    }
}