package dev.sanmer.pi.ui.provider

import androidx.compose.runtime.staticCompositionLocalOf
import dev.sanmer.pi.datastore.model.UserPreferences

val LocalUserPreferences = staticCompositionLocalOf { UserPreferences() }