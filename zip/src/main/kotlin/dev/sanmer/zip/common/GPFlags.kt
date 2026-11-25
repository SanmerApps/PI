package dev.sanmer.zip.common

@JvmInline
value class GPFlags internal constructor(internal val value: Int) {
    val isEncrypted get() = (value and BIT_ENCRYPTION) != 0
    val isDeferredCrc get() = (value and BIT_DEFERRED_CRC) != 0
    val isEnhancedDeflating get() = (value and BIT_ENHANCED_DEFLATING) != 0
    val isCompressedPatchedData get() = (value and BIT_PATCHED_DATA) != 0
    val isStrongEncrypted get() = (value and BIT_STRONG_ENCRYPTION) != 0
    val isUtf8 get() = (value and BIT_EFS) != 0

    override fun toString() = buildString {
        append("{ ")
        append("isEncrypted = $isEncrypted, ")
        append("isDeferredCrc = $isDeferredCrc, ")
        append("isEnhancedDeflating = $isEnhancedDeflating, ")
        append("isCompressedPatchedData = $isCompressedPatchedData, ")
        append("isStrongEncrypted = $isStrongEncrypted, ")
        append("isUtf8 = $isUtf8")
        append(" }")
    }

    internal companion object Default {
        const val BIT_ENCRYPTION = (1 shl 0)
        const val BIT_DEFERRED_CRC = (1 shl 3)
        const val BIT_ENHANCED_DEFLATING = (1 shl 4)
        const val BIT_PATCHED_DATA = (1 shl 5)
        const val BIT_STRONG_ENCRYPTION = (1 shl 6) or (1 shl 13)
        const val BIT_EFS = (1 shl 11)
    }
}