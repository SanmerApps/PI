package dev.sanmer.pi.compat

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.system.Os
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import java.io.File

object MediaStoreCompat {
    fun Context.createMediaStoreUri(
        file: File,
        collection: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        mimeType: String
    ): Uri? {
        val entry = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, file.parent)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }

        return contentResolver.insert(collection, entry)
    }

    fun Context.getDisplayNameForUri(uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.toFile().name
        }

        require(uri.scheme == "content") { "Uri lacks 'content' scheme: $uri" }

        contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                )
            }
        }

        return uri.toString()
    }

    fun Context.getPathForUri(uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.toFile().path
        }

        require(uri.scheme == "content") { "Uri lacks 'content' scheme: $uri" }

        contentResolver.openFileDescriptor(
            getDocumentUri(this, uri),
            "r"
        )?.use {
            return Os.readlink("/proc/self/fd/${it.fd}")
        }

        return uri.toString()
    }

    fun Context.getFileForUri(uri: Uri) = File(getPathForUri(uri))

    fun getDocumentUri(context: Context, uri: Uri): Uri {
        return when {
            DocumentsContract.isTreeUri(uri) -> {
                DocumentFile.fromTreeUri(context, uri)?.uri ?: uri
            }
            else -> {
                uri
            }
        }
    }

    fun Context.copyToDir(uri: Uri, dir: File): File {
        if (!dir.exists()) dir.mkdirs()
        val tmp = dir.resolve(getDisplayNameForUri(uri))

        contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            tmp.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tmp
    }
}