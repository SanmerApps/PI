package dev.sanmer.pi.ktx

import android.system.Os
import android.system.OsConstants
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.FileDescriptor
import java.io.FileInputStream

internal fun FileDescriptor.asZipFile() = ZipFile.builder()
    .setIgnoreLocalFileHeader(true)
    .setSeekableByteChannel(FileInputStream(this).channel)
    .get()

val FileDescriptor.statSize: Long
    get() {
        val st = Os.fstat(this)
        return if (OsConstants.S_ISREG(st.st_mode) ||
            OsConstants.S_ISLNK(st.st_mode)
        ) {
            st.st_size
        } else {
            -1
        }
    }