package dev.sanmer.pi.factory

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.system.Os
import dev.sanmer.pi.Logger
import dev.sanmer.pi.parser.BundleInfo
import dev.sanmer.pi.parser.PackageInfoLite
import dev.sanmer.pi.parser.PackageParser
import dev.sanmer.pi.parser.PackageParser.toLite

class BundleFactory(
    private val context: Context
) {
    private fun ContentResolver.queryString(uri: Uri, columnName: String): String? {
        query(
            uri,
            arrayOf(columnName),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(columnName))
            }
        }
        return null
    }

    private fun getOwner(uri: Uri): String? {
        require(uri.scheme == "content") { "Expected scheme = content" }
        return when {
            uri.authority == MediaStore.AUTHORITY -> {
                context.contentResolver.queryString(
                    uri = uri,
                    columnName = MediaStore.MediaColumns.OWNER_PACKAGE_NAME
                )
            }

            else -> {
                uri.authority?.let {
                    context.packageManager.resolveContentProvider(it, 0)
                        ?.packageName
                }
            }
        }
    }

    private fun getPackageInfoLite(packageName: String): PackageInfoLite? {
        return runCatching {
            context.packageManager.getPackageInfo(
                packageName, 0
            )
        }.getOrNull()?.toLite()
    }

    fun openFd(uri: Uri): ParcelFileDescriptor {
        require(uri.scheme == "content") { "Expected scheme = content" }
        return requireNotNull(context.contentResolver.openFileDescriptor(uri, "r")) {
            "Failed to open $uri"
        }
    }

    fun load(uri: Uri): Data {
        val owner = getOwner(uri)
        val bundleInfo = openFd(uri).use {
            val path = Os.readlink("/proc/self/fd/${it.fd}")
            logger.i("From $owner, Path $path")
            PackageParser.loadBundleFromFd(it.fileDescriptor)
        }
        return Data(
            uri = uri,
            bundleInfo = bundleInfo,
            currentInfo = getPackageInfoLite(bundleInfo.packageInfo.packageName),
            sourceInfo = owner?.let(::getPackageInfoLite)
        )
    }

    data class Data(
        val uri: Uri,
        val bundleInfo: BundleInfo,
        val currentInfo: PackageInfoLite?,
        val sourceInfo: PackageInfoLite?,
    )

    companion object Default {
        private val logger = Logger.Android("BundleFactory")
    }
}