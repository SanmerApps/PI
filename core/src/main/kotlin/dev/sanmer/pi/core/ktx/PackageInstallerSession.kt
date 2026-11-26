package dev.sanmer.pi.core.ktx

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller
import dev.sanmer.pi.core.compat.IntentSenderCompat
import dev.sanmer.zip.ZipFile
import dev.sanmer.zip.io.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.asSink
import kotlinx.io.bytestring.decodeToString

@SuppressLint("RequestInstallPackagesPolicy")
suspend fun PackageInstaller.Session.commit() = IntentSenderCompat.onDelegate { sender ->
    commit(sender)
}

suspend fun PackageInstaller.Session.write(
    name: String,
    source: RawSource,
    length: Long,
    onProgress: (Long) -> Unit = {}
) = withContext(Dispatchers.IO) {
    var copied = 0L
    val output = openWrite(name, 0, length)
    val sink = output.asSink()
    val buffer = Buffer()
    var bytes = source.readAtMostTo(buffer, DEFAULT_BUFFER_SIZE.toLong())
    while (bytes > 0) {
        sink.write(buffer, bytes)
        copied += bytes
        onProgress(copied)
        bytes = source.readAtMostTo(buffer, DEFAULT_BUFFER_SIZE.toLong())
    }
    fsync(output)
    output.close()
}

suspend fun PackageInstaller.Session.write(
    names: List<String>,
    dataSource: DataSource,
    onProgress: (Long) -> Unit = {}
) = withContext(Dispatchers.IO) {
    val zip = ZipFile.of(dataSource)
    var copied = 0L
    for (record in zip.records) {
        val name = record.name.decodeToString(Charsets.UTF_8)
        if (name !in names) continue
        val source = zip.getSource(record)
        val output = openWrite(name, 0, record.uncompressedSize.toLong())
        val sink = output.asSink()
        val buffer = Buffer()
        var bytes = source.readAtMostTo(buffer, DEFAULT_BUFFER_SIZE.toLong())
        while (bytes > 0) {
            sink.write(buffer, bytes)
            copied += bytes
            onProgress(copied)
            bytes = source.readAtMostTo(buffer, DEFAULT_BUFFER_SIZE.toLong())
        }
        fsync(output)
        output.close()
        source.close()
    }
}