package dev.sanmer.zip.io

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered

private class LimitedSource(
    private val source: RawSource,
    private val byteCount: Long
) : RawSource {
    private var remaining = byteCount

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        checkByteCount(byteCount)
        if (byteCount == 0L) return 0L
        if (remaining == 0L) return -1L
        val maxToCopy = minOf(byteCount, remaining)
        val read = source.readAtMostTo(sink, maxToCopy)
        if (read == -1L) remaining = 0L else remaining -= read
        return read
    }

    override fun close() = source.close()

    override fun toString() = "limited($source, $byteCount)"
}

fun RawSource.limited(byteCount: Long): RawSource = LimitedSource(this, byteCount)

fun RawSource.asSource(): Source = this as? Source ?: buffered()