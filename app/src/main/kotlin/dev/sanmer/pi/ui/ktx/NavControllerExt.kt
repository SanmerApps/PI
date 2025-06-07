package dev.sanmer.pi.ui.ktx

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun <T : Any> NavController.navigateSingleTopTo(
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {}
) = navigate(route) {
    launchSingleTop = true
    restoreState = true
    builder()
}