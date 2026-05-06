package dev.sanmer.pi.ui.screens

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
    @Serializable
    data object Apps : Screen

    @Serializable
    data object Settings : Screen
}