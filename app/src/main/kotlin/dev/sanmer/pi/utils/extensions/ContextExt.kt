package dev.sanmer.pi.utils.extensions

import android.content.Context
import android.content.Intent
import androidx.core.app.LocaleManagerCompat

val Context.tmpDir get() = checkNotNull(externalCacheDir).resolve("tmp")
    .apply {
        if (!exists()) mkdirs()
    }

fun Context.openUrl(url: String) {
    Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
        startActivity(this)
    }
}

val Context.applicationLocale
    get() = LocaleManagerCompat.getApplicationLocales(applicationContext)
        .toList().firstOrNull()