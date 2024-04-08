package dev.sanmer.hidden.compat.content.bundle

import android.content.pm.PackageParser

class FeatureSplitConfig(
    override val name: String,
    filename: String,
    size: Long
) : SplitConfig(
    filename,
    size
) {
    override fun isRequired(): Boolean {
        return false
    }

    override fun isDisabled(): Boolean {
        return false
    }

    override fun isRecommended(): Boolean {
        return true
    }

    companion object {
        fun build(
            apk: PackageParser.ApkLite,
            filename: String,
            size: Long
        ): FeatureSplitConfig? {
            if (!apk.isFeatureSplit) return null
            return FeatureSplitConfig(apk.splitName, filename, size)
        }
    }
}