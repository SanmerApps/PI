package dev.sanmer.pi.ui.providable

import androidx.compose.runtime.staticCompositionLocalOf
import dev.sanmer.pi.datastore.UserPreferences

val LocalUserPreferences = staticCompositionLocalOf { UserPreferences.default() }