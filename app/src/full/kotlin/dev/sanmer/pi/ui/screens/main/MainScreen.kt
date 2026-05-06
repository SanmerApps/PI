package dev.sanmer.pi.ui.screens.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.sanmer.pi.ui.screens.Screen

@Composable
fun MainScreen(
    backStack: NavBackStack<Screen>,
    entryProvider: (Screen) -> NavEntry<Screen>
) {
    NavDisplay(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = backStack::removeLastOrNull,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            fadeIn(
                animationSpec = tween(500)
            ) togetherWith fadeOut(
                animationSpec = tween(500)
            )
        },
        popTransitionSpec = {
            fadeIn(
                animationSpec = tween(500)
            ) togetherWith fadeOut(
                animationSpec = tween(500)
            )
        },
        predictivePopTransitionSpec = {
            fadeIn(
                animationSpec = tween(500)
            ) togetherWith fadeOut(
                animationSpec = tween(500)
            )
        },
        entryProvider = entryProvider
    )
}