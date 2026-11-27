package dev.sanmer.zip.io

import kotlinx.io.RawSource

interface DataSource : AutoCloseable {
    val length: Long

    fun source(offset: Long = 0): RawSource

    fun read(bytes: ByteArray, byteOffset: Int, byteCount: Int, offset: Long = 0): Int

    fun source(offset: Long = 0, byteCount: Long = length): RawSource {
        checkBounds(length, offset, byteCount)
        return source(offset).limited(byteCount)
    }

    fun slice(offset: Long = 0, byteCount: Long = length): DataSource {
        checkBounds(length, offset, byteCount)
        return SliceDataSource(this, offset, byteCount)
    }
}

private class SliceDataSource(
    private val dataSource: DataSource,
    private val offset: Long,
    override val length: Long
) : DataSource {
    override fun source(offset: Long): RawSource {
        checkBounds(length, offset)
        return dataSource.source(this.offset + offset).limited(length - offset)
    }

    override fun read(
        bytes: ByteArray,
        byteOffset: Int,
        byteCount: Int,
        offset: Long
    ): Int {
        checkBounds(length, offset)
        checkByteCount(byteCount.toLong())
        if (byteCount == 0) return 0
        return dataSource.read(bytes, byteOffset, byteCount, this.offset + offset)
    }

    override fun close() = dataSource.close()

    override fun toString() = "$dataSource[$offset..${offset + length}]"
}