package dev.sanmer.pi.utils.extensions

import android.content.Context
import android.content.Intent
import androidx.core.app.LocaleManagerCompat

val Context.tmpDir get() = checkNotNull(externalCacheDir).resolve("tmp")
    .apply {
        if (!exists()) mkdirs()
    }

val Context.applicationLocale
    get() = LocaleManagerCompat.getApplicationLocales(applicationContext)
        .toList().firstOrNull()

fun Context.viewUrl(url: String) {
    Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
        startActivity(this)
    }
}

fun Context.viewPackage(packageName: String) {
    Intent(
        Intent.ACTION_SHOW_APP_INFO
    ).apply {
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        startActivity(this)
    }
}