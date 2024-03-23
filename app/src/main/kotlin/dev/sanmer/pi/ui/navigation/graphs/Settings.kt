package dev.sanmer.pi.ui.navigation.graphs

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.sanmer.pi.ui.animate.slideInLeftToRight
import dev.sanmer.pi.ui.animate.slideInRightToLeft
import dev.sanmer.pi.ui.animate.slideOutLeftToRight
import dev.sanmer.pi.ui.animate.slideOutRightToLeft
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.screens.settings.SettingsScreen
import dev.sanmer.pi.ui.screens.settings.workingmode.WorkingModeScreen

enum class SettingsScreen(val route: String) {
    Home("Settings"),
    WorkingMode("WorkingMode")
}

private val subScreens = listOf(
    SettingsScreen.WorkingMode.route
)

fun NavGraphBuilder.settingsScreen(
    navController: NavController
) = navigation(
    startDestination = SettingsScreen.Home.route,
    route = MainScreen.Settings.route
) {
    composable(
        route = SettingsScreen.Home.route,
        enterTransition = {
            if (initialState.destination.route in subScreens) {
                slideInLeftToRight()
            } else {
                slideInRightToLeft()
            } + fadeIn()
        },
        exitTransition = {
            if (targetState.destination.route in subScreens) {
                slideOutRightToLeft()
            } else {
                slideOutLeftToRight()
            } + fadeOut()
        }
    ) {
        SettingsScreen(
            navController = navController
        )
    }

    composable(
        route = SettingsScreen.WorkingMode.route,
        enterTransition = { slideInRightToLeft() + fadeIn() },
        exitTransition = { slideOutLeftToRight() + fadeOut() }
    ) {
        WorkingModeScreen(
            navController = navController
        )
    }
}