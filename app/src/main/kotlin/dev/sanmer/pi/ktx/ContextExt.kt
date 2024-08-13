package dev.sanmer.pi.ktx

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.LocaleManagerCompat
import java.util.Locale

val Context.applicationLocale: Locale?
    get() = LocaleManagerCompat.getApplicationLocales(applicationContext)
        .toList().firstOrNull()

fun Context.viewUrl(url: String) {
    startActivity(
        Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
    )
}

fun Context.viewPackage(packageName: String) {
    Intent(
        Intent.ACTION_SHOW_APP_INFO
    ).apply {
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        startActivity(this)
    }
}

fun Context.appSetting(packageName: String) {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    )
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }

    return null
}

fun Context.finishActivity() {
    if (this is Activity) finish()
}