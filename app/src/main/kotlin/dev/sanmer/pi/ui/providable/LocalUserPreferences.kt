package dev.sanmer.pi.ui.providable

import androidx.compose.runtime.staticCompositionLocalOf
import dev.sanmer.pi.datastore.UserPreferencesCompat

val LocalUserPreferences = staticCompositionLocalOf { UserPreferencesCompat.default() }