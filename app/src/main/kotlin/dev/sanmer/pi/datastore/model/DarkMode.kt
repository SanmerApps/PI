package dev.sanmer.pi.datastore.model

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import dev.sanmer.pi.R

enum class DarkMode(@field:StringRes val text: Int) {
    Default(R.string.settings_dark_mode_auto),
    On(R.string.settings_dark_mode_on),
    Off(R.string.settings_dark_mode_off);

    val isDarkTheme
        @Composable get() = when (this) {
            Default -> isSystemInDarkTheme()
            On -> true
            Off -> false
        }
}