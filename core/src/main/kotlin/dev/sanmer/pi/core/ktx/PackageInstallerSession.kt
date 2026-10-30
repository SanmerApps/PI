package dev.sanmer.pi.core.ktx

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller
import android.content.res.AssetFileDescriptor
import dev.sanmer.pi.core.compat.IntentSenderCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.InputStream
import java.io.OutputStream

@SuppressLint("RequestInstallPackagesPolicy")
suspend fun PackageInstaller.Session.commit() = IntentSenderCompat.onDelegate { sender ->
    commit(sender)
}

private inline fun InputStream.copyTo(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    onProgress: (Long) -> Unit
): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        onProgress(bytesCopied)
        bytes = read(buffer)
    }
    return bytesCopied
}

suspend fun PackageInstaller.Session.writeFd(
    name: String,
    fd: AssetFileDescriptor,
    onProgress: (Long) -> Unit = {}
) = withContext(Dispatchers.IO) {
    fd.createInputStream().use { input ->
        openWrite(name, 0, fd.length).use { output ->
            input.copyTo(output) { onProgress(it) }
            fsync(output)
        }
    }
}

suspend fun PackageInstaller.Session.writeZip(
    names: List<String>,
    fd: AssetFileDescriptor,
    onProgress: (Long) -> Unit = {}
) = withContext(Dispatchers.IO) {
    ZipFile.Builder()
        .setIgnoreLocalFileHeader(true)
        .setSeekableByteChannel(fd.createInputStream().channel)
        .get()
        .use { zip ->
            var copied = 0L
            for (entry in zip.entries) {
                if (entry.name in names) {
                    zip.getInputStream(entry).use { input ->
                        openWrite(entry.name, 0, entry.size).use { output ->
                            copied += input.copyTo(output) { onProgress(copied + it) }
                            fsync(output)
                        }
                    }
                }
            }
        }
}