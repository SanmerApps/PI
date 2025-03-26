package dev.sanmer.pi.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.PIService
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.ui.main.MainScreen
import dev.sanmer.pi.ui.main.SetupScreen
import dev.sanmer.pi.ui.provider.LocalPreference
import dev.sanmer.pi.ui.theme.AppTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preference: PreferenceRepository

    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { isLoading }

        setContent {
            val preferenceState = preference.data.collectAsStateWithLifecycle(initialValue = null)
            val preference = preferenceState.value ?: return@setContent
            isLoading = false

            LaunchedEffect(preference) {
                PIService.init(preference.provider)
            }

            CompositionLocalProvider(
                LocalPreference provides preference
            ) {
                AppTheme {
                    Crossfade(
                        targetState = preference.provider != Provider.None,
                        label = "MainActivity"
                    ) { isReady ->
                        if (isReady) {
                            MainScreen()
                        } else {
                            SetupScreen(
                                setProvider = ::setProvider
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setProvider(value: Provider) {
        lifecycleScope.launch {
            preference.setProvider(value)
        }
    }
}