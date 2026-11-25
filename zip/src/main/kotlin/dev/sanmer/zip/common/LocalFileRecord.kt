package dev.sanmer.zip.common

import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.readByteString
import kotlinx.io.readUIntLe
import kotlinx.io.readUShortLe

class LocalFileRecord internal constructor(
    internal val mGpFlags: UShort,
    internal val mCompressionMethod: UShort,
    internal val mCrc32: UInt,
    internal val mCompressedSize: UInt,
    internal val mUncompressedSize: UInt,
    internal val mName: ByteString,
    internal val mExtraFieldSize: Int
) {
    val gpFlags get() = GPFlags(mGpFlags.toInt())

    val compressionMethod get() = CompressionMethod(mCompressionMethod.toInt())

    val crc32 get() = mCrc32

    val compressedSize get() = mCompressedSize.toInt()

    val uncompressedSize get() = mUncompressedSize.toInt()

    val name get() = mName

    val size get() = FIX_HEADER_SIZE_BYTES + mName.size + mExtraFieldSize

    override fun toString() = buildString {
        append("{ ")
        append("mGpFlags = ${gpFlags}, ")
        append("mCompressionMethod = $mCompressionMethod, ")
        append("mCrc32 = $mCrc32, ")
        append("mCompressedSize = $mCompressedSize, ")
        append("mUncompressedSize = $mUncompressedSize, ")
        append("mNameSize = ${mName.size}, ")
        append("mExtraFieldSize = $mExtraFieldSize")
        append(" }")
    }

    companion object Builder {
        internal const val RECORD_SIGNATURE = 0x04034b50U
        internal const val FIX_HEADER_SIZE_BYTES = 30L

        fun read(source: Source): LocalFileRecord {
            source.require(FIX_HEADER_SIZE_BYTES)
            val recordSignature = source.readUIntLe()
            require(recordSignature == RECORD_SIGNATURE) {
                "Expect signature = 0x04034b50, but 0x${recordSignature.toHexString()}"
            }

            source.skip(2) // Version to extract
            val mGpFlags = source.readUShortLe()
            val mCompressionMethod = source.readUShortLe()
            source.skip(2) // Last modification time
            source.skip(2) // Last modification date

            val mCrc32: UInt
            val mCompressedSize: UInt
            val mUncompressedSize: UInt
            if (GPFlags(mGpFlags.toInt()).isDeferredCrc) {
                source.skip(4)
                mCrc32 = 0U
                source.skip(4)
                mCompressedSize = 0U
                source.skip(4)
                mUncompressedSize = 0U
            } else {
                mCrc32 = source.readUIntLe()
                mCompressedSize = source.readUIntLe()
                mUncompressedSize = source.readUIntLe()
            }

            val mNameSize = source.readUShortLe().toInt()
            val mExtraFieldSize = source.readUShortLe().toInt()

            source.require(mNameSize.toLong() + mExtraFieldSize)
            val mName = source.readByteString(mNameSize)
            source.skip(mExtraFieldSize.toLong())

            return LocalFileRecord(
                mGpFlags = mGpFlags,
                mCompressionMethod = mCompressionMethod,
                mCrc32 = mCrc32,
                mCompressedSize = mCompressedSize,
                mUncompressedSize = mUncompressedSize,
                mName = mName,
                mExtraFieldSize = mExtraFieldSize
            )
        }

        fun read(source: Source, cdRecord: CentralDirectoryRecord): LocalFileRecord {
            source.require(FIX_HEADER_SIZE_BYTES)
            val recordSignature = source.readUIntLe()
            require(recordSignature == RECORD_SIGNATURE) {
                "Expect signature = 0x04034b50 for ${
                    cdRecord.mName.decodeToString(Charsets.UTF_8)
                }, but 0x${recordSignature.toHexString()}"
            }

            source.skip(2) // Version to extract
            val mGpFlags = source.readUShortLe()
            val mCompressionMethod = source.readUShortLe()
            require(mCompressionMethod == cdRecord.mCompressionMethod) {
                "Expect compressionMethod = ${cdRecord.mCompressionMethod} for ${
                    cdRecord.mName.decodeToString(Charsets.UTF_8)
                }, but $mCompressionMethod"
            }

            source.skip(2) // Last modification time
            source.skip(2) // Last modification date

            if (GPFlags(mGpFlags.toInt()).isDeferredCrc) {
                source.skip(16)

                return LocalFileRecord(
                    mGpFlags = mGpFlags,
                    mCompressionMethod = mCompressionMethod,
                    mCrc32 = cdRecord.mCrc32,
                    mCompressedSize = cdRecord.mCompressedSize,
                    mUncompressedSize = cdRecord.mUncompressedSize,
                    mName = cdRecord.mName,
                    mExtraFieldSize = cdRecord.mExtraFieldSize
                )
            }

            val mCrc32 = source.readUIntLe()
            require(mCrc32 == cdRecord.mCrc32) {
                "Expect crc32 = ${cdRecord.mCrc32} for ${
                    cdRecord.mName.decodeToString(Charsets.UTF_8)
                }, but $mCrc32"
            }

            val mCompressedSize = source.readUIntLe()
            require(mCompressedSize == cdRecord.mCompressedSize) {
                "Expect compressedSize = ${cdRecord.mCompressedSize} for ${
                    cdRecord.mName.decodeToString(Charsets.UTF_8)
                }, but $mCompressedSize"
            }

            val mUncompressedSize = source.readUIntLe()
            require(mUncompressedSize == cdRecord.mUncompressedSize) {
                "Expect uncompressedSize = ${cdRecord.mUncompressedSize} for ${
                    cdRecord.mName.decodeToString(Charsets.UTF_8)
                }, but $mUncompressedSize"
            }

            val mNameSize = source.readUShortLe().toInt()
            require(mNameSize == cdRecord.mName.size) {
                "Expect nameSize = ${cdRecord.mName.size} for ${
                    cdRecord.mName.decodeToString(Charsets.UTF_8)
                }, but $mNameSize"
            }

            val mExtraFieldSize = source.readUShortLe().toInt()

            source.require(mNameSize.toLong() + mExtraFieldSize)
            val mName = source.readByteString(mNameSize)
            require(mName == cdRecord.mName) {
                "Expect name = ${
                    cdRecord.mName.decodeToString(Charsets.UTF_8)
                }, but ${
                    mName.decodeToString(Charsets.UTF_8)
                }"
            }

            source.skip(mExtraFieldSize.toLong())

            return LocalFileRecord(
                mGpFlags = mGpFlags,
                mCompressionMethod = mCompressionMethod,
                mCrc32 = cdRecord.mCrc32,
                mCompressedSize = cdRecord.mCompressedSize,
                mUncompressedSize = cdRecord.mUncompressedSize,
                mName = mName,
                mExtraFieldSize = mExtraFieldSize
            )
        }

        fun readOrNull(source: Source): LocalFileRecord? {
            source.require(FIX_HEADER_SIZE_BYTES)
            val recordSignature = source.readUIntLe()
            if (recordSignature != RECORD_SIGNATURE) return null

            source.skip(2) // Version to extract
            val mGpFlags = source.readUShortLe()
            val mCompressionMethod = source.readUShortLe()
            source.skip(2) // Last modification time
            source.skip(2) // Last modification date

            if (GPFlags(mGpFlags.toInt()).isDeferredCrc) return null
            val mCrc32 = source.readUIntLe()
            val mCompressedSize = source.readUIntLe()
            val mUncompressedSize = source.readUIntLe()

            val mNameSize = source.readUShortLe().toInt()
            val mExtraFieldSize = source.readUShortLe().toInt()

            source.require(mNameSize.toLong() + mExtraFieldSize)
            val mName = source.readByteString(mNameSize)
            source.skip(mExtraFieldSize.toLong())

            return LocalFileRecord(
                mGpFlags = mGpFlags,
                mCompressionMethod = mCompressionMethod,
                mCrc32 = mCrc32,
                mCompressedSize = mCompressedSize,
                mUncompressedSize = mUncompressedSize,
                mName = mName,
                mExtraFieldSize = mExtraFieldSize
            )
        }

        inline fun Source.forEachLocalFileRecord(action: (LocalFileRecord) -> Unit) {
            while (true) action(readOrNull(this) ?: break)
        }

        inline fun Source.findLocalFileRecord(predicate: (LocalFileRecord) -> Boolean): LocalFileRecord? {
            forEachLocalFileRecord { if (predicate(it)) return it }
            return null
        }
    }
}