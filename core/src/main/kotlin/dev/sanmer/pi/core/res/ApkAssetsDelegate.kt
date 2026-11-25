package dev.sanmer.pi.core.res

import android.content.Context
import android.content.res.ApkAssets
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.system.Os
import android.util.Log
import androidx.annotation.RequiresApi
import dev.sanmer.pi.core.compat.BuildCompat
import dev.sanmer.zip.io.asSource
import kotlinx.io.RawSource
import kotlinx.io.asSink
import java.io.File
import java.io.FileDescriptor

sealed interface ApkAssetsDelegate : AutoCloseable {
    fun get(): ApkAssets

    class Path(
        private val path: String,
        private val autoClose: Boolean
    ) : ApkAssetsDelegate {
        private val apkAssets = when {
            BuildCompat.atLeastR -> ApkAssets.loadFromPath(
                path,
                0,
                null
            )

            else -> ApkAssets.loadFromPath(
                path,
                false,
                false
            )
        }

        override fun get() = apkAssets

        override fun close() {
            if (autoClose) Os.remove(path)
            apkAssets.close()
        }
    }

    class Fd private constructor(
        private val fd: FileDescriptor,
        private val apkAssets: ApkAssets,
        private val autoClose: Boolean
    ) : ApkAssetsDelegate {
        constructor(
            fd: FileDescriptor,
            autoClose: Boolean,
            friendlyName: String = fd.toString()
        ) : this(
            fd = fd,
            apkAssets = when {
                BuildCompat.atLeastR -> ApkAssets.loadFromFd(
                    fd,
                    friendlyName,
                    0,
                    null
                )

                else -> ApkAssets.loadFromFd(
                    fd,
                    friendlyName,
                    false,
                    false
                )
            },
            autoClose = autoClose
        )

        @RequiresApi(Build.VERSION_CODES.R)
        constructor(
            fd: FileDescriptor,
            offset: Long,
            length: Long,
            autoClose: Boolean,
            friendlyName: String = fd.toString()
        ) : this(
            fd = fd,
            apkAssets = ApkAssets.loadFromFd(
                fd,
                friendlyName,
                offset,
                length,
                0,
                null
            ),
            autoClose = autoClose
        )

        override fun get() = apkAssets

        override fun close() {
            if (autoClose) Os.close(fd)
            apkAssets.close()
        }
    }

    object Builder {
        inline fun <R> createTemp(
            source: RawSource,
            cacheDir: File?,
            block: (ApkAssets) -> R
        ): R {
            val temp = File.createTempFile("stream", "", cacheDir)
            return try {
                temp.outputStream().use { output ->
                    source.asSource().use { source ->
                        source.transferTo(output.asSink())
                    }
                }
                Path(temp.absolutePath, false).use { apkAssets ->
                    block(apkAssets.get())
                }
            } finally {
                if (temp.delete()) Log.d("ApkAssetsDelegate", "Deleted $temp")
            }
        }

        inline fun <R> openFd(
            fd: FileDescriptor,
            autoClose: Boolean,
            friendlyName: String = fd.toString(),
            block: (ApkAssets) -> R
        ) = Fd(fd, autoClose, friendlyName).use { apkAssets ->
            block(apkAssets.get())
        }

        @RequiresApi(Build.VERSION_CODES.R)
        inline fun <R> openFd(
            fd: FileDescriptor,
            offset: Long,
            length: Long,
            autoClose: Boolean,
            friendlyName: String = fd.toString(),
            block: (ApkAssets) -> R
        ) = Fd(fd, offset, length, autoClose, friendlyName).use { apkAssets ->
            block(apkAssets.get())
        }

        class AutoQuitThreadCallback(
            private val callback: ProxyFileDescriptorCallback,
            private val thread: HandlerThread
        ) : ProxyFileDescriptorCallback() {
            override fun onGetSize() = callback.onGetSize()

            override fun onRead(offset: Long, size: Int, data: ByteArray) =
                callback.onRead(offset, size, data)

            override fun onRelease() {
                callback.onRelease()
                thread.quitSafely()
            }
        }

        inline fun <R> openFd(
            context: Context,
            callback: ProxyFileDescriptorCallback,
            friendlyName: String = callback.toString(),
            block: (ApkAssets) -> R
        ): R {
            val storage = requireNotNull(context.getSystemService(StorageManager::class.java)) {
                "Expect StorageManager"
            }
            val thread = HandlerThread(friendlyName).apply { start() }
            return storage.openProxyFileDescriptor(
                ParcelFileDescriptor.MODE_READ_ONLY,
                AutoQuitThreadCallback(callback, thread),
                Handler(thread.looper)
            ).use { pfd ->
                openFd(pfd.fileDescriptor, false, friendlyName, block)
            }
        }
    }
}