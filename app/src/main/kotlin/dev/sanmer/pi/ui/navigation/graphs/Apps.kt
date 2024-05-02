package dev.sanmer.pi.ui.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.screens.apps.AppsScreen
import dev.sanmer.pi.ui.screens.apps.details.DetailsScreen

enum class AppsScreen(val route: String) {
    Home("Apps"),
    Details("details/{packageName}")
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

    composable(
        route = AppsScreen.Details.route,
        arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        enterTransition = { scaleIn() + fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        DetailsScreen(
            navController = navController
        )
    }
}