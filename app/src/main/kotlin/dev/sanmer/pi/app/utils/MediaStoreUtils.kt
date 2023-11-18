package dev.sanmer.pi.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile

object MediaStoreUtils {
    fun getDisplayNameForUri(context: Context, uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.toFile().name
        }

        require(uri.scheme == "content") { "Uri lacks 'content' scheme: $uri" }

        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        val cr = context.contentResolver
        cr.query(uri, projection, null, null, null)?.use { cursor ->
            val displayNameColumn = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                return cursor.getString(displayNameColumn)
            }
        }

        return uri.toString()
    }
}