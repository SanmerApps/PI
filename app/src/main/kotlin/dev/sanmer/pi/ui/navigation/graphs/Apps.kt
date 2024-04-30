package dev.sanmer.pi.ui.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.screens.apps.AppsScreen

enum class AppsScreen(val route: String) {
    Home("Apps")
}

fun NavGraphBuilder.appsScreen(
    navController: NavController
) = navigation(
    startDestination = AppsScreen.Home.route,
    route = MainScreen.Apps.route
) {
    composable(
        route = AppsScreen.Home.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        AppsScreen(
            navController = navController
        )
    }
}