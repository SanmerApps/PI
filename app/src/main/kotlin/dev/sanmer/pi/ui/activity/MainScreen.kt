package dev.sanmer.pi.ui.activity

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.navigation.graphs.homeScreen
import dev.sanmer.pi.ui.navigation.graphs.settingsScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = MainScreen.Home.route
        ) {
            homeScreen(
                navController = navController
            )
            settingsScreen(
                navController = navController
            )
        }
    }
}