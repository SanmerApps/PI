package dev.sanmer.hidden.compat.content.bundle

import android.text.format.Formatter
import dev.sanmer.hidden.compat.delegate.ContextDelegate

abstract class SplitConfig(
    val filename: String,
    val size: Long
) {
    abstract val name: String

    fun formattedSize(): String {
        val context = ContextDelegate.getContext()
        return Formatter.formatFileSize(context, size)
    }

    abstract fun isRequired(): Boolean
    abstract fun isDisabled(): Boolean
    abstract fun isRecommended(): Boolean

    override fun toString(): String {
        return "name = ${name}, " +
                "required = ${isRequired()}, " +
                "disabled = ${isDisabled()}, " +
                "recommended = ${isRecommended()}"
    }

    internal companion object {
        fun parseSplit(splitName: String) : String? {
            val split = splitName.removeSurrounding("config.", "")
            return if (split != splitName) {
                split
            } else {
                null
            }
        }
    }
}