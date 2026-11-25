package dev.sanmer.zip

import dalvik.annotation.optimization.FastNative
import dev.sanmer.zip.io.checkBounds

@JvmInline
value class CRC32 private constructor(val value: UInt) {
    constructor() : this(value = 0U)

    fun update(bytes: ByteArray, offset: Int, length: Int): CRC32 {
        checkBounds(bytes.size, offset, length)
        return CRC32(Native.crc32(value.toInt(), bytes, offset, length).toUInt())
    }

    fun update(bytes: ByteArray) = update(bytes, 0, bytes.size)

    private object Native {
        init {
            System.loadLibrary("z_rs")
        }

        @FastNative
        @JvmStatic
        external fun crc32(start: Int, bytes: ByteArray, offset: Int, length: Int): Int
    }
}