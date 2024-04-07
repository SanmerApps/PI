package dev.sanmer.hidden.compat.content.bundle

import android.content.pm.PackageParser
import java.util.Locale

class LanguageSplitConfig(
    val locale: Locale,
    filename: String,
    size: Long,
) : SplitConfig(
    filename,
    size
) {
    override val name = locale.displayLanguage

    override fun isRequired(): Boolean {
        val default = Locale.getDefault()
        return locale.language == default.language
    }

    override fun isDisabled(): Boolean {
        return locale !in Locale.getAvailableLocales()
    }

    override fun isRecommended(): Boolean {
        return false
    }

    companion object {
        fun build(apk: PackageParser.ApkLite, filename: String, size: Long): LanguageSplitConfig? {
            val value = parseSplit(apk.splitName)?.uppercase() ?: return null
            val locale = Locale.forLanguageTag(value)
            if (locale.language.isEmpty()) return null

            return LanguageSplitConfig(locale, filename, size)
        }
    }
}