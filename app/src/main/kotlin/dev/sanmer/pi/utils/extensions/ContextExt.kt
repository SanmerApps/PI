package dev.sanmer.pi.utils.extensions

import android.content.Context

val Context.tmpDir get() = checkNotNull(externalCacheDir).resolve("tmp")
    .apply {
        if (!exists()) mkdirs()
    }