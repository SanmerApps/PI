package dev.sanmer.pi.utils.extensions

import android.content.Context

val Context.tmpDir get() = cacheDir.resolve("tmp")
    .apply {
        if (!exists()) mkdirs()
    }