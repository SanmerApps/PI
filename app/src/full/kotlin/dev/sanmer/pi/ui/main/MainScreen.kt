package dev.sanmer.pi.ui.main

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sanmer.pi.ui.screens.apps.AppsScreen
import dev.sanmer.pi.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Apps()
    ) {
        Screen.Apps(navController).addTo(this)
        Screen.Settings(navController).addTo(this)
    }
}

sealed class Screen(
    private val route: String,
    private val content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
    private val arguments: List<NamedNavArgument> = emptyList(),
    private val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = { fadeIn() },
    private val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = { fadeOut() },
) {
    fun addTo(builder: NavGraphBuilder) = builder.composable(
        route = this@Screen.route,
        arguments = this@Screen.arguments,
        enterTransition = this@Screen.enterTransition,
        exitTransition = this@Screen.exitTransition,
        content = this@Screen.content
    )

    @Suppress("FunctionName")
    companion object Routes {
        fun Apps() = "Apps"
        fun Settings() = "Settings"
    }

    class Apps(navController: NavController) : Screen(
        route = Apps(),
        content = { AppsScreen(navController = navController) }
    )

    class Settings(navController: NavController) : Screen(
        route = Settings(),
        content = { SettingsScreen(navController = navController) }
    )
}