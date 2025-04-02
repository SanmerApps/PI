package dev.sanmer.pi.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.ui.main.MainScreen
import dev.sanmer.pi.ui.main.SetupScreen
import dev.sanmer.pi.ui.provider.LocalPreference
import dev.sanmer.pi.ui.theme.AppTheme
import dev.sanmer.pi.viewmodel.MainViewModel
import dev.sanmer.pi.viewmodel.MainViewModel.LoadState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { viewModel.isPending }

        setContent {
            when (viewModel.state) {
                LoadState.Pending -> {}
                is LoadState.Ready -> CompositionLocalProvider(
                    LocalPreference provides viewModel.preference
                ) {
                    AppTheme {
                        Crossfade(
                            targetState = viewModel.preference.provider != Provider.None,
                        ) { isReady ->
                            if (isReady) {
                                MainScreen()
                            } else {
                                SetupScreen(
                                    setProvider = viewModel::setProvider
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}