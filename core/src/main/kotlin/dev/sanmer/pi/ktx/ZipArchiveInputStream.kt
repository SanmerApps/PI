package dev.sanmer.pi.ktx

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.FileNotFoundException

@Throws(FileNotFoundException::class)
internal fun ZipArchiveInputStream.find(name: String): ZipArchiveEntry {
    while (true) {
        val entry = nextEntry ?: break
        if (entry.name == name) return entry
    }
    throw FileNotFoundException("$name not found")
}