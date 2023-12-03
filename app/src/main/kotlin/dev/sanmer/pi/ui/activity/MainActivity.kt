package dev.sanmer.pi.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.app.Settings
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.repository.SettingsRepository
import dev.sanmer.pi.ui.theme.AppTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { isLoading }

        setContent {
            if (BuildCompat.atLeastT) {
                NotificationUtils.PermissionState()
            }

            val workingMode by settingsRepository.getWorkingModeOrNone()
                .collectAsStateWithLifecycle(initialValue = null)

            LaunchedEffect(workingMode) {
                workingMode?.let {
                    ProviderCompat.init(
                        mode = it,
                        scope = lifecycleScope
                    )
                    isLoading = false
                }
            }

            AppTheme {
                Crossfade(
                    targetState = workingMode != Settings.Provider.None,
                    label = "MainActivity"
                ) { isReady ->
                    if (isReady) {
                        MainScreen()
                    } else {
                        SetupScreen(
                            setMode = ::setWorkingMode
                        )
                    }
                }
            }
        }
    }

    private fun setWorkingMode(value: Settings.Provider) {
        lifecycleScope.launch {
            settingsRepository.setWorkingMode(value)
        }
    }
}