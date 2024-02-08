package dev.sanmer.pi.ui.providable

import androidx.compose.runtime.staticCompositionLocalOf
import dev.sanmer.pi.datastore.UserPreferencesExt

val LocalUserPreferences = staticCompositionLocalOf { UserPreferencesExt.default() }