package dev.sanmer.pi.bundle

import android.content.pm.PackageParser.ApkLite
import android.text.format.Formatter
import dev.sanmer.pi.ContextCompat

abstract class SplitConfig(
    val configForSplit: String?,
    val filename: String,
    val size: Long
) {
    val isConfigForSplit by lazy {
        !configForSplit.isNullOrEmpty()
    }

    val formattedSize: String by lazy {
        val context = ContextCompat.getContext()
        Formatter.formatFileSize(context, size)
    }

    abstract val name: String
    abstract val isRequired: Boolean
    abstract val isDisabled: Boolean
    abstract val isRecommended: Boolean

    override fun toString(): String {
        return "name = ${name}, " +
                "required = ${isRequired}, " +
                "disabled = ${isDisabled}, " +
                "recommended = $isRecommended"
    }

    internal companion object {
        fun parseSplit(apk: ApkLite): String? {
            val splitName = apk.splitName.removeSurrounding("${apk.configForSplit}.", "")
            val name = splitName.removeSurrounding("config.", "")
            return if (name != splitName) {
                name
            } else {
                null
            }
        }
    }
}