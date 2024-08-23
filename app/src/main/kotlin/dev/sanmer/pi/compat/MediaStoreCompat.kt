package dev.sanmer.pi.compat

import android.content.ContentResolver
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
        collection: Uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL),
        mimeType: String
    ): Uri {
        val entry = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, file.parent)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        }

        return requireNotNull(contentResolver.insert(collection, entry))
    }

    private fun ContentResolver.queryString(uri: Uri, columnName: String): String? {
        query(
            uri,
            arrayOf(columnName),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(
                    cursor.getColumnIndexOrThrow(columnName)
                )
            }
        }

        return null
    }

    fun Context.getOwnerPackageNameForUri(uri: Uri): String? {
        require(uri.scheme == "content") { "Expected scheme = content" }
        return when {
            uri.authority == MediaStore.AUTHORITY -> {
                contentResolver.queryString(
                    uri = uri,
                    columnName = MediaStore.MediaColumns.OWNER_PACKAGE_NAME
                )
            }

            else -> {
                uri.authority?.let {
                    packageManager.resolveContentProvider(
                        it, 0
                    )?.packageName
                }
            }
        }
    }

    fun Context.getDisplayNameForUri(uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.toFile().name
        }

        require(uri.scheme == "content") { "Expected scheme = content" }
        return contentResolver.queryString(
            uri = uri,
            columnName = MediaStore.MediaColumns.DISPLAY_NAME
        ) ?: uri.toString()
    }

    private fun getDocumentUri(context: Context, uri: Uri): Uri {
        return when {
            DocumentsContract.isTreeUri(uri) -> DocumentFile.fromTreeUri(context, uri)?.uri ?: uri
            else -> uri
        }
    }

    fun Context.getPathForUri(uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.toFile().path
        }

        require(uri.scheme == "content") { "Expected scheme = content" }
        contentResolver.openFileDescriptor(
            getDocumentUri(this, uri), "r"
        )?.use {
            return Os.readlink("/proc/self/fd/${it.fd}")
        }

        return uri.toString()
    }

    fun Context.copyToFile(uri: Uri, file: File): Long? {
        return contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            file.outputStream().use(input::copyTo)
        }
    }
}