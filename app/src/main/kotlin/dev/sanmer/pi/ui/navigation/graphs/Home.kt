package dev.sanmer.pi.ui.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import dev.sanmer.pi.ui.animate.slideInRightToLeft
import dev.sanmer.pi.ui.animate.slideOutLeftToRight
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.screens.applist.AppListScreen
import dev.sanmer.pi.ui.screens.apps.AppsScreen
import dev.sanmer.pi.ui.screens.home.HomeScreen
import dev.sanmer.pi.ui.screens.sessions.SessionsScreen

enum class HomeScreen(val route: String) {
    Home("Home"),
    Apps("AppsScreen"),
    AppList("AppList/{target}"),
    Sessions("Sessions")
}

fun NavGraphBuilder.homeScreen(
    navController: NavController
) = navigation(
    startDestination = HomeScreen.Home.route,
    route = MainScreen.Home.route
) {
    composable(
        route = HomeScreen.Home.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        HomeScreen(
            navController = navController
        )
    }

    composable(
        route = HomeScreen.Apps.route,
        enterTransition = { slideInRightToLeft() + fadeIn() },
        exitTransition = { slideOutLeftToRight() + fadeOut() }
    ) {
        AppsScreen(
            navController = navController
        )
    }

    composable(
        route = HomeScreen.AppList.route,
        arguments = listOf(navArgument("target") { type = NavType.StringType }),
        enterTransition = { slideInRightToLeft() + fadeIn() },
        exitTransition = { slideOutLeftToRight() + fadeOut() }
    ) {
        AppListScreen(
            navController = navController
        )
    }

    composable(
        route = HomeScreen.Sessions.route,
        enterTransition = { slideInRightToLeft() + fadeIn() },
        exitTransition = { slideOutLeftToRight() + fadeOut() }
    ) {
        SessionsScreen(
            navController = navController
        )
    }
}