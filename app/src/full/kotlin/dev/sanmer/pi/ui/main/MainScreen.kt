package dev.sanmer.pi.ui.main

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sanmer.pi.ui.screens.apps.AppsScreen
import dev.sanmer.pi.ui.screens.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Apps
    ) {
        composable<Screen.Apps> {
            AppsScreen(
                navController = navController
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                navController = navController
            )
        }
    }
}

sealed class Screen {
    @Serializable
    data object Apps : Screen()

    @Serializable
    data object Settings : Screen()
}