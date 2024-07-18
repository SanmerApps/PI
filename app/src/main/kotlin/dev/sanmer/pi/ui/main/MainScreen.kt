package dev.sanmer.pi.ui.main

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sanmer.pi.ui.main.Screen.Companion.apps
import dev.sanmer.pi.ui.main.Screen.Companion.settings
import dev.sanmer.pi.ui.main.Screen.Companion.workingMode
import dev.sanmer.pi.ui.screens.apps.AppsScreen
import dev.sanmer.pi.ui.screens.settings.SettingsScreen
import dev.sanmer.pi.ui.screens.workingmode.WorkingModeScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars
    ) {
        NavHost(
            modifier = Modifier.padding(it),
            navController = navController,
            startDestination = Screen.Apps.route
        ) {
            apps(navController)
            settings(navController)
            workingMode(navController)
        }
    }
}

enum class Screen(val route: String) {
    Apps("Apps"),
    Settings("Settings"),
    WorkingMode("WorkingMode");

    companion object {

        fun NavGraphBuilder.apps(
            navController: NavController
        ) = composable(
            route = Apps.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            AppsScreen(
                navController = navController
            )
        }

        fun NavGraphBuilder.settings(
            navController: NavController
        ) = composable(
            route = Settings.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SettingsScreen(
                navController = navController
            )
        }

        fun NavGraphBuilder.workingMode(
            navController: NavController
        ) = composable(
            route = WorkingMode.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            WorkingModeScreen(
                navController = navController
            )
        }
    }
}