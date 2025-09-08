package dev.sanmer.pi.res

import android.content.res.ApkAssets
import android.util.Log
import dev.sanmer.pi.ContextCompat
import dev.sanmer.pi.ktx.statSize
import dev.sanmer.pi.ktx.temp
import java.io.FileDescriptor
import java.io.InputStream

sealed interface ApkAssetsWrapper : Wrapper<ApkAssets> {
    class Stream(
        original: InputStream
    ) : ApkAssetsWrapper {
        private val temp = ContextCompat.getContext().externalCacheDir.temp()
        private val asset by lazy { ApkAssets.loadFromPath(temp.absolutePath, 0, null) }

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
        private val original: FileDescriptor
    ) : ApkAssetsWrapper {
        private val asset by lazy {
            ApkAssets.loadFromFd(
                original,
                original.toString(),
                0,
                original.statSize,
                0,
                null
            )
        }

        override fun get() = asset

        override fun close() = asset.close()
    }
}