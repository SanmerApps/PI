package dev.sanmer.pi.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.sanmer.pi.datastore.compose.LocalPreference
import dev.sanmer.pi.ui.screens.main.MainScreen
import dev.sanmer.pi.ui.screens.main.MainViewModel
import dev.sanmer.pi.ui.screens.main.MainViewModel.LoadState
import dev.sanmer.pi.ui.screens.main.SetupScreen
import dev.sanmer.pi.ui.theme.AppTheme
import org.koin.android.ext.android.get
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.compose.navigation3.getEntryProvider
import org.koin.androidx.scope.activityRetainedScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.scope.Scope

@OptIn(KoinExperimentalAPI::class)
class MainActivity : ComponentActivity(), AndroidScopeComponent {
    override val scope: Scope by activityRetainedScope()
    private val viewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { viewModel.isPending }

        setContent {
            when (viewModel.loadState) {
                LoadState.Pending -> {}
                is LoadState.Ready -> CompositionLocalProvider(
                    LocalPreference provides viewModel.preference
                ) {
                    AppTheme(
                        darkMode = viewModel.preference.darkMode.isDarkTheme
                    ) {
                        Crossfade(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.background
                            ),
                            targetState = viewModel.isNone,
                        ) { isNone ->
                            if (isNone) {
                                SetupScreen(
                                    setProvider = viewModel::setProvider
                                )
                            } else {
                                MainScreen(
                                    backStack = get(),
                                    entryProvider = getEntryProvider()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}