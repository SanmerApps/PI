package dev.sanmer.pi.bundle

import android.os.Build

enum class ABI(val value: String) {
    ARM64_V8A("arm64-v8a"),
    ARMEABI_V7A("armeabi-v7a"),
    ARMEABI("armeabi"),
    X86("x86"),
    X86_64("x86_64");

    val displayName: String
        inline get() = value

    val isRequired: Boolean
        inline get() = value == Build.SUPPORTED_ABIS[0]

    val isEnabled: Boolean
        inline get() = value in Build.SUPPORTED_ABIS

    companion object Default {
        fun valueOfOrNull(value: String): ABI? = try {
            valueOf(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}