package dev.sanmer.pi.ui.activity

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.navigation.graphs.appsScreen
import dev.sanmer.pi.ui.navigation.graphs.settingsScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars
    ) {
        NavHost(
            modifier = Modifier.padding(it),
            navController = navController,
            startDestination = MainScreen.Apps.route
        ) {
            appsScreen(
                navController = navController
            )
            settingsScreen(
                navController = navController
            )
        }
    }
}