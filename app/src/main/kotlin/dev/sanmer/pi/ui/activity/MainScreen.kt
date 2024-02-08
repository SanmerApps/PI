package dev.sanmer.pi.ui.activity

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sanmer.pi.ui.animate.slideInRightToLeft
import dev.sanmer.pi.ui.animate.slideOutLeftToRight
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.screens.apps.AppsScreen
import dev.sanmer.pi.ui.screens.home.HomeScreen
import dev.sanmer.pi.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = MainScreen.Home.route
        ) {
            composable(
                route = MainScreen.Home.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                HomeScreen(
                    navController = navController
                )
            }

            composable(
                route = MainScreen.Apps.route,
                enterTransition = { slideInRightToLeft() + fadeIn() },
                exitTransition = { slideOutLeftToRight() + fadeOut() }
            ) {
                AppsScreen(
                    navController = navController
                )
            }

            composable(
                route = MainScreen.Settings.route,
                enterTransition = { slideInRightToLeft() + fadeIn() },
                exitTransition = { slideOutLeftToRight() + fadeOut() }
            ) {
                SettingsScreen(
                    navController = navController
                )
            }
        }
    }
}