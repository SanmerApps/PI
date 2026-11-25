package dev.sanmer.zip

import dev.sanmer.zip.io.DataSource
import dev.sanmer.zip.io.asSource
import dev.sanmer.zip.io.checkBounds
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.readByteArray

class DeflateDataSource private constructor(
    private val dataSource: DataSource,
    private val points: List<Point>,
    override val length: Long
) : DataSource {
    private fun inflater(outOffset: Long): Pair<Point, Inflater> {
        val inflater = Inflater(-Inflater.MAX_WINDOW_BITS)
        val idx = points.binarySearchBy(outOffset) { it.outOffset }
        val point = points[if (idx >= 0) idx else -(idx + 1) - 1]
        if (point.bits != 0) {
            val bytes = ByteArray(1)
            dataSource.read(bytes, 0, 1, point.inOffset - 1)
            val byte = bytes[0].toInt() and 0xFF
            val ret = inflater.prime(point.bits, byte ushr (8 - point.bits))
            check(ret == Inflater.ReturnCode.Ok) { ret }
        }
        if (point.dictionary.isNotEmpty()) {
            val ret = inflater.setDictionary(point.dictionary)
            check(ret == Inflater.ReturnCode.Ok) { ret }
        }
        return point to inflater
    }

    override fun source(offset: Long): Source {
        checkBounds(length, offset)
        val (point, inflater) = inflater(offset)
        val source = InflaterSource(dataSource.source(point.inOffset), inflater).asSource()
        source.skip(offset - point.outOffset)
        return source
    }

    override fun read(
        bytes: ByteArray,
        byteOffset: Int,
        byteCount: Int,
        offset: Long
    ): Int {
        checkBounds(length, offset)
        val source = source(offset)
        var read = 0
        while (read < byteCount) {
            val n = source.readAtMostTo(bytes, byteOffset + read, byteOffset + byteCount)
            if (n == -1) break
            read += n
        }
        return read
    }

    override fun close() = dataSource.close()

    override fun toString() = "deflate($dataSource)"

    private class Point(
        val inOffset: Long,
        val outOffset: Long,
        val bits: Int,
        val dictionary: ByteArray
    ) {
        override fun toString() = buildString {
            append("{ ")
            append("inOffset = $inOffset, ")
            append("outOffset = $outOffset, ")
            append("bits = $bits, ")
            append("dictionary = ${dictionary.size}")
            append(" }")
        }
    }

    companion object Builder {
        private const val WINDOW_SIZE = 32 shl 10
        private const val PER_POINT = WINDOW_SIZE + 20
        private const val MAX_INDEX_BYTES = 64L shl 20
        private const val MAX_SPAN = 8L shl 20
        private const val MIN_SPAN = 256L shl 10
        private const val ALIGN = 64L shl 10

        private fun span(uncompressedSize: Long): Long {
            val maxIndexBytes = minOf((uncompressedSize * 0.03).toLong(), MAX_INDEX_BYTES)
            val maxPoints = (maxIndexBytes / PER_POINT).coerceAtLeast(3)
            val spanByBudget = (uncompressedSize + maxPoints - 1) / maxPoints
            val span = spanByBudget.coerceIn(MIN_SPAN, MAX_SPAN)
            return (span + ALIGN - 1) / ALIGN * ALIGN
        }

        private fun build(source: RawSource, span: Long): List<Point> {
            val inflater = Inflater(-Inflater.MAX_WINDOW_BITS)
            val points = mutableListOf(
                Point(
                    inOffset = 0L,
                    outOffset = 0L,
                    bits = 0,
                    dictionary = ByteArray(0)
                )
            )

            val inBuffer = Buffer()
            val outBuffer = Buffer()
            source.readAtMostTo(inBuffer, WINDOW_SIZE.toLong())
            while (inBuffer.size != 0L) {
                val (ret, outLen) = inflater.inflate(inBuffer, outBuffer, Inflater.Flush.Block)
                check(ret == Inflater.ReturnCode.Ok) { ret }
                if (outBuffer.size > WINDOW_SIZE) outBuffer.skip(outLen.toLong())

                val totalIn = inflater.totalIn
                val totalOut = inflater.totalOut
                val dataType = inflater.dataType
                if (
                    dataType and 128 != 0 && dataType and 64 == 0
                    && totalOut - points[points.lastIndex].outOffset >= span
                ) {
                    points += Point(
                        inOffset = totalIn,
                        outOffset = totalOut,
                        bits = dataType and 7,
                        dictionary = outBuffer.peek().readByteArray()
                    )
                }

                source.readAtMostTo(inBuffer, WINDOW_SIZE - inBuffer.size)
            }
            return points
        }

        fun of(dataSource: DataSource, uncompressedSize: Long) = DeflateDataSource(
            dataSource = dataSource,
            points = build(
                source = dataSource.source(),
                span = span(uncompressedSize)
            ),
            length = uncompressedSize
        )
    }
}