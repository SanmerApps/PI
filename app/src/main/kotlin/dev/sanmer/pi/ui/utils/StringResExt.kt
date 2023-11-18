package dev.sanmer.pi.ui.utils

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
@ReadOnlyComposable
internal fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}

@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int, @StringRes vararg formatArgs: Int): String {
    val resources = resources()
    val strings = formatArgs.map {
        resources.getString(it)
    }.toTypedArray()

    return resources.getString(id, *strings)
}