package dev.sanmer.zip.common

@JvmInline
value class CompressionMethod internal constructor(internal val code: Int) {
    companion object Default {
        val STORE = CompressionMethod(0)
        val DEFLATE = CompressionMethod(8)
    }
}