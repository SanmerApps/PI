package dev.sanmer.zip

import kotlinx.io.Buffer
import kotlinx.io.RawSource

class InflaterSource(
    private val source: RawSource,
    private val inflater: Inflater
) : RawSource {
    private val buffer = Buffer()

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val read = source.readAtMostTo(buffer, byteCount - buffer.size)
        if (buffer.size == 0L) return read
        val (ret, outLen) = inflater.inflate(buffer, sink, Inflater.Flush.NoFlush)
        check(ret == Inflater.ReturnCode.Ok || ret == Inflater.ReturnCode.StreamEnd) { ret }
        return outLen.toLong()
    }

    override fun close() = source.close()

    override fun toString() = "InflaterSource($source)"
}