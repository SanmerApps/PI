package dev.sanmer.zip

import dev.sanmer.zip.common.CentralDirectoryRecord
import dev.sanmer.zip.common.CompressionMethod
import dev.sanmer.zip.common.DataDescriptorRecord
import dev.sanmer.zip.common.EofCentralDirectoryRecord
import dev.sanmer.zip.common.LocalFileRecord
import dev.sanmer.zip.io.DataSource
import dev.sanmer.zip.io.asSource
import kotlinx.io.RawSource
import kotlinx.io.bytestring.ByteString

class ZipFile private constructor(
    private val dataSource: DataSource,
    private val cdRecords: Map<ByteString, CentralDirectoryRecord>
) {
    private val localRecords = hashMapOf<ByteString, LocalFileRecord>()

    val length get() = dataSource.length
    val records get() = cdRecords.values

    operator fun get(name: ByteString) = cdRecords[name]
    operator fun contains(name: ByteString) = cdRecords.containsKey(name)

    fun asDataSource() = dataSource

    fun getLocal(cdRecord: CentralDirectoryRecord): LocalFileRecord {
        val local = localRecords[cdRecord.mName]
        if (local != null) return local

        val localFileOffset = cdRecord.mLocalFileOffset.toLong()
        val record = LocalFileRecord.read(
            source = dataSource.source(localFileOffset).asSource(),
            cdRecord = cdRecord
        )
        localRecords[cdRecord.mName] = record
        if (!record.gpFlags.isDeferredCrc) return record

        val dataOffset = localFileOffset + record.size
        DataDescriptorRecord.verify(
            source = dataSource.source(dataOffset + record.mCompressedSize.toInt()).asSource(),
            cdRecord = cdRecord
        )
        return record
    }

    fun dataOffset(cdRecord: CentralDirectoryRecord): Long {
        val local = getLocal(cdRecord)
        val localFileOffset = cdRecord.mLocalFileOffset.toLong()
        return localFileOffset + local.size
    }

    fun getRawSource(cdRecord: CentralDirectoryRecord) = dataSource.source(
        offset = dataOffset(cdRecord),
        byteCount = cdRecord.mCompressedSize.toLong()
    )

    fun getSource(cdRecord: CentralDirectoryRecord) =
        getRawSource(cdRecord).uncompress(cdRecord.compressionMethod)

    fun getRawDataSource(cdRecord: CentralDirectoryRecord) = dataSource.slice(
        offset = dataOffset(cdRecord),
        byteCount = cdRecord.mCompressedSize.toLong()
    )

    fun getDataSource(cdRecord: CentralDirectoryRecord) = when (cdRecord.compressionMethod) {
        CompressionMethod.Store -> getRawDataSource(cdRecord)

        CompressionMethod.Deflate -> DeflateDataSource.of(
            dataSource = getRawDataSource(cdRecord),
            uncompressedSize = cdRecord.mUncompressedSize.toLong()
        )

        else -> throw IllegalArgumentException("Unsupported compressionMethod: ${cdRecord.mCompressionMethod}")
    }

    companion object Builder {
        fun of(
            dataSource: DataSource,
            maxTailSize: Int = DEFAULT_BUFFER_SIZE
        ): ZipFile {
            require(dataSource.length >= EofCentralDirectoryRecord.FIX_HEADER_SIZE_BYTES) { "Not Zip" }

            val eofCDRecord = EofCentralDirectoryRecord.find(
                source = dataSource.source((dataSource.length - maxTailSize).coerceAtLeast(0))
                    .asSource(),
                maxTailSize = maxTailSize
            )

            val cdRecords = CentralDirectoryRecord.read(
                source = dataSource.source(eofCDRecord.mDirectoryOffset.toLong()).asSource(),
                eofCDRecord = eofCDRecord
            )

            return ZipFile(
                dataSource = dataSource,
                cdRecords = cdRecords
            )
        }

        fun RawSource.uncompress(method: CompressionMethod) = when (method) {
            CompressionMethod.Store -> this
            CompressionMethod.Deflate -> InflaterSource(
                source = this,
                inflater = Inflater(-Inflater.MAX_WINDOW_BITS)
            )

            else -> throw IllegalArgumentException("Unsupported compressionMethod: ${method.value}")
        }
    }
}