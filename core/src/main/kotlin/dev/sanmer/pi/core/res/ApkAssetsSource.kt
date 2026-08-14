package dev.sanmer.pi.core.res

import android.content.res.ApkAssets
import android.content.res.AssetFileDescriptor
import android.util.Log
import dev.sanmer.pi.core.compat.BuildCompat
import java.io.File
import java.io.InputStream

sealed interface ApkAssetsSource : AutoCloseable {
    fun get(): ApkAssets

    class Stream(
        original: InputStream,
        cacheDir: File
    ) : ApkAssetsSource {
        private val temp = File.createTempFile("stream", "", cacheDir)

        private val asset by lazy {
            when {
                BuildCompat.atLeastR -> ApkAssets.loadFromPath(
                    temp.absolutePath,
                    0,
                    null
                )

                else -> ApkAssets.loadFromPath(
                    temp.absolutePath,
                    false,
                    false
                )
            }
        }

        init {
            original.buffered().use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        override fun get() = asset

        override fun close() {
            asset.close()
            if (temp.delete()) {
                Log.w("ApkAssetsWrapper.Stream", "Deleted $temp")
            }
        }
    }

    class Fd(
        private val original: AssetFileDescriptor
    ) : ApkAssetsSource {
        private val asset by lazy {
            when {
                BuildCompat.atLeastR -> ApkAssets.loadFromFd(
                    original.fileDescriptor,
                    original.toString(),
                    0,
                    original.length,
                    0,
                    null
                )

                else -> ApkAssets.loadFromFd(
                    original.fileDescriptor,
                    original.toString(),
                    false,
                    false
                )
            }
        }

        override fun get() = asset

        override fun close() = asset.close()
    }
}