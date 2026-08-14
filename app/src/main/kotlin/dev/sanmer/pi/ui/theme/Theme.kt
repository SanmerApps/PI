package dev.sanmer.pi.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.sanmer.pi.compat.BuildCompat

@Composable
fun AppTheme(
    darkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = MaterialTheme(
    colorScheme = colorScheme(darkMode),
    shapes = Shapes,
    typography = Typography,
    content = content
)

@Composable
private fun colorScheme(
    darkMode: Boolean = isSystemInDarkTheme(),
    context: Context = LocalContext.current
) = if (BuildCompat.atLeastS) {
    when {
        darkMode -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
} else {
    when {
        darkMode -> DarkColorScheme
        else -> LightColorScheme
    }
}