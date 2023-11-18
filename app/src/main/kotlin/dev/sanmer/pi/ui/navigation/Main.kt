package dev.sanmer.pi.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import dev.sanmer.pi.ui.utils.navigatePopUpTo

enum class MainScreen(val route: String) {
    Home("HomeScreen"),
    Apps("AppsScreen"),
    Settings("SettingsScreen")
}

fun NavController.navigateToApps() = navigatePopUpTo(MainScreen.Apps.route)
fun NavController.navigateToSettings() = navigatePopUpTo(MainScreen.Settings.route)
