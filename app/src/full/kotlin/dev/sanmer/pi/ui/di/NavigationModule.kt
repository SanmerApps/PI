package dev.sanmer.pi.ui.di

import androidx.navigation3.runtime.NavBackStack
import dev.sanmer.pi.ui.screens.Screen
import dev.sanmer.pi.ui.screens.apps.AppsScreen
import dev.sanmer.pi.ui.screens.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.scope.dsl.activityRetainedScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val Navigation = module {
    includes(ViewModels)

    activityRetainedScope {
        scoped { NavBackStack(Screen.Apps) }

        navigation<Screen.Apps> {
            val backStack = get<NavBackStack<Screen>>()
            AppsScreen(
                viewModel = koinViewModel(),
                goTo = backStack::add
            )
        }

        navigation<Screen.Settings> {
            val backStack = get<NavBackStack<Screen>>()
            SettingsScreen(
                viewModel = koinViewModel(),
                goBack = backStack::removeLastOrNull
            )
        }
    }
}