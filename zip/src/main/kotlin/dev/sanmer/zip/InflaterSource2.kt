package dev.sanmer.zip

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import java.util.zip.Inflater

class InflaterSource2(
    private val source: RawSource,
    private val inflater: Inflater
) : RawSource {
    private val buffer = Buffer()

    @OptIn(UnsafeIoApi::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val read = source.readAtMostTo(buffer, byteCount - buffer.size)
        if (buffer.size == 0L) return read
        return UnsafeBufferOperations.writeToTail(sink, 1) { outBytes, outPos, outLimit ->
            val outLength = outLimit - outPos
            val produced: Int
            UnsafeBufferOperations.readFromHead(buffer) { inBytes, inPos, inLimit ->
                val inLength = inLimit - inPos
                inflater.setInput(inBytes, inPos, inLength)
                produced = inflater.inflate(outBytes, outPos, outLength)
                inLength - inflater.remaining
            }
            produced
        }.toLong()
    }

    override fun close() {
        source.close()
        inflater.end()
    }
}