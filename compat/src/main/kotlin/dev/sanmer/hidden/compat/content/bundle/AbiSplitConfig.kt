package dev.sanmer.hidden.compat.content.bundle

import android.content.pm.PackageParser.ApkLite
import android.os.Build

class AbiSplitConfig(
    val abi: Abi,
    configForSplit: String?,
    filename: String,
    size: Long,
) : SplitConfig(
    configForSplit,
    filename,
    size
) {
    override val name by lazy {
        abi.name.lowercase().replace("_", "-")
    }

    override val isRequired by lazy {
        name == Build.SUPPORTED_ABIS[0]
    }

    override val isDisabled by lazy {
        name !in Build.SUPPORTED_ABIS
    }

    override val isRecommended by lazy {
        isRequired
    }

    enum class Abi {
        ARM64_V8A,
        ARMEABI_V7A,
        ARMEABI,
        X86,
        X86_64
    }

    companion object {
        fun build(
            apk: ApkLite,
            filename: String,
            size: Long
        ): AbiSplitConfig? {
            val value = parseSplit(apk)?.uppercase() ?: return null
            val abi = runCatching { Abi.valueOf(value) }.getOrNull() ?: return null
            return AbiSplitConfig(
                abi = abi,
                configForSplit = apk.configForSplit,
                filename = filename,
                size = size
            )
        }
    }
}