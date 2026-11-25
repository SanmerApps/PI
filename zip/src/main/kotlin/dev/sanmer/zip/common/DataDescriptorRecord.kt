package dev.sanmer.zip.common

import kotlinx.io.Source
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.readUIntLe

class DataDescriptorRecord internal constructor(
    internal val mCrc32: UInt,
    internal val mCompressedSize: UInt,
    internal val mUncompressedSize: UInt
) {
    override fun toString() = buildString {
        append("{ ")
        append("mCrc32 = $mCrc32, ")
        append("mCompressedSize = $mCompressedSize, ")
        append("mUncompressedSize = $mUncompressedSize")
        append(" }")
    }

    companion object Builder {
        internal const val RECORD_SIGNATURE = 0x08074b50U

        fun read(source: Source): DataDescriptorRecord {
            source.require(12)
            val value = source.readUIntLe()
            val mCrc32 = if (value == RECORD_SIGNATURE) {
                source.readUIntLe()
            } else {
                value
            }
            val mCompressedSize = source.readUIntLe()
            val mUncompressedSize = source.readUIntLe()

            return DataDescriptorRecord(
                mCrc32 = mCrc32,
                mCompressedSize = mCompressedSize,
                mUncompressedSize = mUncompressedSize
            )
        }

        fun verify(source: Source, cdRecord: CentralDirectoryRecord) {
            source.require(12)
            val value = source.readUIntLe()
            val mCrc32 = if (value == RECORD_SIGNATURE) source.readUIntLe() else value
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
        }
    }
}