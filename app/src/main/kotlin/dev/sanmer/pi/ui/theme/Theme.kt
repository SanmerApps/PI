package dev.sanmer.pi.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import dev.sanmer.pi.compat.BuildCompat

@Composable
fun ComponentActivity.AppTheme(
    darkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
    val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

    DisposableEffect(darkMode) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { darkMode },
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim,
                darkScrim,
            ) { darkMode }
        )
        onDispose {}
    }

    MaterialTheme(
        colorScheme = getColorScheme(darkMode),
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}

@Composable
private fun getColorScheme(darkMode: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (BuildCompat.atLeastS) {
        when {
            darkMode -> dynamicDarkColorScheme(context)
            else -> dynamicLightColorScheme(context)
        }
    } else {
        when {
            darkMode -> BlueLightColorScheme
            else -> BlueDarkColorScheme
        }
    }
}