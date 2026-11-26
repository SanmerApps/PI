package dev.sanmer.zip.io

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import java.io.FileDescriptor

private class FileDescriptorSource(
    private val fd: FileDescriptor
) : RawSource {
    @OptIn(UnsafeIoApi::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (byteCount == 0L) return 0L
        return UnsafeBufferOperations.writeToTail(sink, 1) { bytes, pos, limit ->
            val maxToCopy = minOf(byteCount, (limit - pos).toLong()).toInt()
            Os.read(fd, bytes, pos, maxToCopy)
        }.toLong()
    }

    override fun close() {}

    override fun toString() = "RawSource($fd)"
}

fun ParcelFileDescriptor.asSource(): RawSource = FileDescriptorSource(fileDescriptor)

private class ParcelFileDescriptorDataSource(
    internal val pfd: ParcelFileDescriptor,
    override val length: Long
) : DataSource {
    constructor(
        pfd: ParcelFileDescriptor
    ) : this(
        pfd = pfd,
        length = pfd.statSize
    )

    private val source = pfd.asSource()

    override fun source(offset: Long): RawSource {
        checkBounds(length, offset)
        Os.lseek(pfd.fileDescriptor, offset, OsConstants.SEEK_SET)
        return source
    }

    override fun read(
        bytes: ByteArray,
        byteOffset: Int,
        byteCount: Int,
        offset: Long
    ): Int {
        checkBounds(length, offset)
        return Os.pread(pfd.fileDescriptor, bytes, byteOffset, byteCount, offset)
    }

    override fun close() = pfd.close()

    override fun toString() = "DataSource($pfd)"
}

fun ParcelFileDescriptor.asDataSource(): DataSource = ParcelFileDescriptorDataSource(this)

val DataSource.pfdOrNull get() = (this as? ParcelFileDescriptorDataSource)?.pfd
