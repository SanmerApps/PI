package dev.sanmer.pi.bundle

import android.content.pm.PackageParser
import java.util.Locale

class LanguageSplitConfig(
    val locale: Locale,
    configForSplit: String?,
    filename: String,
    size: Long,
) : SplitConfig(
    configForSplit,
    filename,
    size
) {
    override val name: String by lazy {
        locale.displayLanguage
    }

    override val isRequired by lazy {
        val default = Locale.getDefault()
        locale.language == default.language
    }

    override val isDisabled by lazy {
        locale !in Locale.getAvailableLocales()
    }

    override val isRecommended = false

    companion object {
        fun build(apk: PackageParser.ApkLite, filename: String, size: Long): LanguageSplitConfig? {
            val value = parseSplit(apk)?.uppercase() ?: return null
            val locale = Locale.forLanguageTag(value)
            if (locale.language.isEmpty()) return null

            return LanguageSplitConfig(
                locale = locale,
                configForSplit = apk.configForSplit,
                filename = filename,
                size = size
            )
        }
    }
}