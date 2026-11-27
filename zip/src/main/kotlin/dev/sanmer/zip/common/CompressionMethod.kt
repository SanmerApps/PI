package dev.sanmer.zip.common

@JvmInline
value class CompressionMethod internal constructor(internal val value: Int) {
    override fun toString() = when (this) {
        Store -> "Store"
        Deflate -> "Deflate"
        else -> "CompressionMethod($value)"
    }

    companion object Default {
        val Store = CompressionMethod(0)
        val Deflate = CompressionMethod(8)
    }
}