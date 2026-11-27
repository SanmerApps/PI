package dev.sanmer.zip.common

import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString
import kotlinx.io.readUIntLe
import kotlinx.io.readUShortLe

class CentralDirectoryRecord internal constructor(
    internal val mGpFlags: UShort,
    internal val mCompressionMethod: UShort,
    internal val mCrc32: UInt,
    internal val mCompressedSize: UInt,
    internal val mUncompressedSize: UInt,
    internal val mName: ByteString,
    internal val mExtraFieldSize: Int,
    internal val mCommentSize: Int,
    internal val mLocalFileOffset: UInt
) {
    val gpFlags get() = GPFlags(mGpFlags.toInt())

    val compressionMethod get() = CompressionMethod(mCompressionMethod.toInt())

    val crc32 get() = mCrc32

    val compressedSize get() = mCompressedSize.toInt()

    val uncompressedSize get() = mUncompressedSize.toInt()

    val name get() = mName

    val size get() = FIX_HEADER_SIZE_BYTES + mName.size + mExtraFieldSize + mCommentSize

    override fun toString() = buildString {
        append("{ ")
        append("mGpFlags = ${gpFlags}, ")
        append("mCompressionMethod = $mCompressionMethod, ")
        append("mCrc32 = $mCrc32, ")
        append("mCompressedSize = $mCompressedSize, ")
        append("mUncompressedSize = $mUncompressedSize, ")
        append("mLocalFileOffset = $mLocalFileOffset, ")
        append("mNameSize = ${mName.size}, ")
        append("mExtraFieldSize = $mExtraFieldSize, ")
        append("mCommentSize = $mCommentSize")
        append(" }")
    }

    companion object Builder {
        internal const val RECORD_SIGNATURE = 0x02014b50U
        internal const val FIX_HEADER_SIZE_BYTES = 46L

        fun read(source: Source): CentralDirectoryRecord {
            source.require(FIX_HEADER_SIZE_BYTES)
            val recordSignature = source.readUIntLe()
            require(recordSignature == RECORD_SIGNATURE) {
                "Expect signature = 0x02014b50, but 0x${recordSignature.toHexString()}"
            }

            source.skip(2) // Made by
            source.skip(2) // Version to extract
            val mGpFlags = source.readUShortLe()
            val mCompressionMethod = source.readUShortLe()
            source.skip(2) // Last modification time
            source.skip(2) // Last modification date
            val mCrc32 = source.readUIntLe()
            val mCompressedSize = source.readUIntLe()
            val mUncompressedSize = source.readUIntLe()
            val mNameSize = source.readUShortLe().toInt()
            val mExtraFieldSize = source.readUShortLe().toInt()
            val mCommentSize = source.readUShortLe().toInt()
            source.skip(2) // Disk start
            source.skip(2) // Internal attributes
            source.skip(4) // External attributes
            val mLocalFileOffset = source.readUIntLe()

            source.require(mNameSize.toLong() + mExtraFieldSize + mCommentSize)
            val mName = source.readByteString(mNameSize)
            source.skip(mExtraFieldSize.toLong())
            source.skip(mCommentSize.toLong())

            return CentralDirectoryRecord(
                mGpFlags = mGpFlags,
                mCompressionMethod = mCompressionMethod,
                mCrc32 = mCrc32,
                mCompressedSize = mCompressedSize,
                mUncompressedSize = mUncompressedSize,
                mName = mName,
                mExtraFieldSize = mExtraFieldSize,
                mCommentSize = mCommentSize,
                mLocalFileOffset = mLocalFileOffset
            )
        }

        fun read(
            source: Source,
            eofCDRecord: EofCentralDirectoryRecord
        ) = buildMap {
            for (i in 0 until eofCDRecord.mTotalRecords.toInt()) {
                try {
                    val record = read(source)
                    put(record.mName, record)
                } catch (e: Throwable) {
                    throw IllegalArgumentException("Not CentralDirectoryRecord at index $i", e)
                }
            }
        }
    }
}