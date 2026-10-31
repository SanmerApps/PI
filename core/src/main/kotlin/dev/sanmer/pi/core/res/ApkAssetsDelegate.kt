package dev.sanmer.pi.core.res

import android.content.res.ApkAssets
import android.system.Os
import dev.sanmer.pi.core.compat.BuildCompat
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
            apkAssets.close()
            if (autoClose) runCatching { Os.remove(path) }
        }
    }

    class Fd(
        length: Long,
        private val fd: FileDescriptor,
        private val autoClose: Boolean
    ) : ApkAssetsDelegate {
        private val apkAssets = when {
            BuildCompat.atLeastR -> ApkAssets.loadFromFd(
                fd,
                fd.toString(),
                0,
                length,
                0,
                null
            )

            else -> ApkAssets.loadFromFd(
                fd,
                fd.toString(),
                false,
                false
            )
        }

        override fun get() = apkAssets

        override fun close() {
            apkAssets.close()
            if (autoClose) runCatching { Os.close(fd) }
        }
    }
}