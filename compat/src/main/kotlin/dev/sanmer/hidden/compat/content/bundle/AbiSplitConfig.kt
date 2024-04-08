package dev.sanmer.hidden.compat.content.bundle

import android.content.pm.PackageParser.ApkLite
import android.os.Build

class AbiSplitConfig(
    val abi: Abi,
    filename: String,
    size: Long,
) : SplitConfig(
    filename,
    size
) {
    override val name = abi.name
        .lowercase()
        .replace("_", "-")

    override fun isRequired(): Boolean {
        return name == Build.SUPPORTED_ABIS[0]
    }

    override fun isDisabled(): Boolean {
        return name !in Build.SUPPORTED_ABIS
    }

    override fun isRecommended(): Boolean {
        return isRequired()
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
            val value = parseSplit(apk.splitName)?.uppercase() ?: return null
            val abi = runCatching { Abi.valueOf(value) }.getOrNull() ?: return null
            return AbiSplitConfig(abi, filename, size)
        }
    }
}