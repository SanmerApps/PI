package dev.sanmer.zip.common

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readUIntLe
import kotlinx.io.readUShortLe

class EofCentralDirectoryRecord internal constructor(
    internal val mTotalRecords: UShort,
    internal val mDirectorySize: UInt,
    internal val mDirectoryOffset: UInt,
    internal val mCommentSize: Int
) {
    val totalRecords get() = mTotalRecords.toInt()

    val directorySize get() = mDirectorySize.toInt()

    val directoryOffset get() = mDirectoryOffset.toInt()

    val size get() = FIX_HEADER_SIZE_BYTES + mCommentSize

    override fun toString() = buildString {
        append("{ ")
        append("mTotalRecords = $mTotalRecords, ")
        append("mDirectorySize = $mDirectorySize, ")
        append("mDirectoryOffset = $mDirectoryOffset, ")
        append("mCommentSize = $mCommentSize")
        append(" }")
    }

    companion object Builder {
        internal const val RECORD_SIGNATURE = 0x06054b50U
        internal const val FIX_HEADER_SIZE_BYTES = 22L
        const val MAX_HEADER_SIZE_BYTES = 22 + 0xFFFF

        fun read(source: Source): EofCentralDirectoryRecord {
            source.require(FIX_HEADER_SIZE_BYTES)
            val recordSignature = source.readUIntLe()
            require(recordSignature == RECORD_SIGNATURE) {
                "Expect signature = 0x06054b50, but 0x${recordSignature.toHexString()}"
            }

            source.skip(2) // Number of this disk
            source.skip(2) // Disk where CD starts
            source.skip(2) // Record on disk count
            val mTotalRecords = source.readUShortLe()
            val mDirectorySize = source.readUIntLe()
            val mDirectoryOffset = source.readUIntLe()
            val mCommentSize = source.readUShortLe().toInt()
            source.skip(mCommentSize.toLong())

            return EofCentralDirectoryRecord(
                mTotalRecords = mTotalRecords,
                mDirectorySize = mDirectorySize,
                mDirectoryOffset = mDirectoryOffset,
                mCommentSize = mCommentSize
            )
        }

        fun find(
            source: Source,
            maxTailSize: Int = MAX_HEADER_SIZE_BYTES
        ): EofCentralDirectoryRecord {
            val bufferSize = minOf(maxTailSize, DEFAULT_BUFFER_SIZE).toLong()
            val buffer = Buffer()
            while (source.readAtMostTo(buffer, bufferSize) != -1L) {
                if (buffer.size > maxTailSize) {
                    buffer.skip(buffer.size - maxTailSize)
                }
            }
            val bytes = buffer.peek().readByteArray()
            require(bytes.size >= FIX_HEADER_SIZE_BYTES) { "Not EofCentralDirectoryRecord" }
            for (i in bytes.size - 22 downTo 0) {
                if (bytes[i] == 0x50.toByte()
                    && bytes[i + 1] == 0x4B.toByte()
                    && bytes[i + 2] == 0x05.toByte()
                    && bytes[i + 3] == 0x06.toByte()
                ) {
                    val commentSize = (bytes[i + 20].toInt() and 0xFF) or
                            ((bytes[i + 21].toInt() and 0xFF) shl 8)
                    if (i + FIX_HEADER_SIZE_BYTES + commentSize == bytes.size.toLong()) {
                        val buffer = buffer.peek()
                        buffer.skip(i.toLong())
                        try {
                            return read(buffer)
                        } catch (_: IllegalArgumentException) {

                        }
                    }
                }
            }
            throw IllegalArgumentException("Not EofCentralDirectoryRecord")
        }
    }
}