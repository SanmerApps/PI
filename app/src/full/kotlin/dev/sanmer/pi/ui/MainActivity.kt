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
import dev.sanmer.pi.ui.main.MainScreen
import dev.sanmer.pi.ui.main.MainViewModel
import dev.sanmer.pi.ui.main.MainViewModel.LoadState
import dev.sanmer.pi.ui.main.SetupScreen
import dev.sanmer.pi.ui.provider.LocalPreference
import dev.sanmer.pi.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val viewModel by viewModel<MainViewModel>()

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
                    AppTheme {
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
                                MainScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}