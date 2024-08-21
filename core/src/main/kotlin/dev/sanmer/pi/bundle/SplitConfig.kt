package dev.sanmer.pi.bundle

import android.content.pm.PackageParser
import android.text.format.Formatter
import dev.sanmer.pi.ContextCompat
import dev.sanmer.pi.bundle.ABI.Default.displayName
import dev.sanmer.pi.bundle.ABI.Default.isEnabled
import dev.sanmer.pi.bundle.ABI.Default.isRequired
import dev.sanmer.pi.bundle.DPI.Default.displayName
import dev.sanmer.pi.bundle.DPI.Default.isRequired
import java.io.File
import java.util.Locale

sealed class SplitConfig {
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

    data class Target(
        val abi: ABI,
        override val configForSplit: String?,
        override val file: File
    ) : SplitConfig() {
        override val name by lazy { abi.displayName }
        override val isRequired by lazy { abi.isRequired }
        override val isDisabled by lazy { !abi.isEnabled }
        override val isRecommended get() = isRequired
    }

    data class Density(
        val dpi: DPI,
        override val configForSplit: String?,
        override val file: File
    ) : SplitConfig() {
        override val name get() = dpi.displayName
        override val isRequired by lazy { dpi.isRequired }
        override val isDisabled = false
        override val isRecommended get() = isRequired
    }

    data class Language(
        val locale: Locale,
        override val configForSplit: String?,
        override val file: File
    ) : SplitConfig() {
        override val name: String get() = locale.localizedDisplayName
        override val isRequired by lazy { locale.language == Locale.getDefault().language }
        override val isDisabled by lazy { locale !in Locale.getAvailableLocales() }
        override val isRecommended get() = isRequired
    }

    data class Feature(
        override val name: String,
        override val file: File
    ) : SplitConfig() {
        override val configForSplit = null
        override val isRequired = false
        override val isDisabled = false
        override val isRecommended = true
    }

    data class Unspecified(
        override val configForSplit: String?,
        override val file: File
    ) : SplitConfig() {
        override val name: String get() = file.name
        override val isRequired = false
        override val isDisabled = false
        override val isRecommended = true
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