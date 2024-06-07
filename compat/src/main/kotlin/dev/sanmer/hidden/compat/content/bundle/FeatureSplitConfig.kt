package dev.sanmer.hidden.compat.content.bundle

import android.content.pm.PackageParser

class FeatureSplitConfig(
    override val name: String,
    filename: String,
    size: Long
) : SplitConfig(
    null,
    filename,
    size
) {
    override val isRequired = false

    override val isDisabled = false

    override val isRecommended = true

    companion object {
        fun build(
            apk: PackageParser.ApkLite,
            filename: String,
            size: Long
        ): FeatureSplitConfig? {
            if (!apk.isFeatureSplit) return null
            return FeatureSplitConfig(
                name = apk.splitName,
                filename = filename,
                size = size
            )
        }
    }
}