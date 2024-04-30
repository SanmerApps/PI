package dev.sanmer.pi.ui.activity

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.navigation.graphs.appsScreen
import dev.sanmer.pi.ui.navigation.graphs.sessionsScreen
import dev.sanmer.pi.ui.navigation.graphs.settingsScreen
import dev.sanmer.pi.ui.utils.navigatePopUpTo

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNav(
                navController = navController
            )
        }
    ) {
        NavHost(
            modifier = Modifier.padding(bottom = it.calculateBottomPadding()),
            navController = navController,
            startDestination = MainScreen.Apps.route
        ) {
            appsScreen(
                navController = navController
            )
            sessionsScreen(
                navController = navController
            )
            settingsScreen(
                navController = navController
            )
        }
    }
}

@Composable
private fun BottomNav(
    navController: NavController
) {
    val mainScreens by remember {
        derivedStateOf { MainScreen.entries }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier.imePadding()
    ) {
        mainScreens.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = screen.label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                alwaysShowLabel = false,
                selected = selected,
                onClick = { if (!selected) navController.navigatePopUpTo(screen.route) }
            )
        }
    }
}