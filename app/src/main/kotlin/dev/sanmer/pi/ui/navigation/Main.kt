package dev.sanmer.pi.ui.navigation

import androidx.navigation.NavController
import dev.sanmer.pi.ui.utils.navigatePopUpTo

enum class MainScreen(val route: String) {
    Home("HomeScreen"),
    Settings("SettingsScreen")
}

fun NavController.navigateToHome() = navigatePopUpTo(MainScreen.Home.route)
fun NavController.navigateToSettings() = navigatePopUpTo(MainScreen.Settings.route)
