package dev.sanmer.pi.ui.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.screens.sessions.SessionsScreen

enum class SessionsScreen(val route: String) {
    Home("Sessions")
}

fun NavGraphBuilder.sessionsScreen(
    navController: NavController
) = navigation(
    startDestination = SessionsScreen.Home.route,
    route = MainScreen.Sessions.route
) {
    composable(
        route = SessionsScreen.Home.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        SessionsScreen(
            navController = navController
        )
    }
}