package dev.sanmer.pi.ui.utils

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder

fun NavController.navigateSingleTopTo(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) = navigate(
    route = route
) {
    launchSingleTop = true
    restoreState = true
    builder()
}

fun NavController.navigatePopUpTo(
    route: String
) = navigateSingleTopTo(
    route = route
) {
    popUpTo(graph.findStartDestination().id) {
        saveState = true
    }
}