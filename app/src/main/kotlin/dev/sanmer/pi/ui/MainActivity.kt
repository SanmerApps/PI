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
import dev.sanmer.pi.Compat
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.receiver.PackageReceiver
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.main.MainScreen
import dev.sanmer.pi.ui.main.SetupScreen
import dev.sanmer.pi.ui.providable.LocalUserPreferences
import dev.sanmer.pi.ui.theme.AppTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { isLoading }
        PackageReceiver.register(this)

        setContent {
            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(initialValue = null)

            val preferences = if (userPreferences == null) {
                return@setContent
            } else {
                isLoading = false
                checkNotNull(userPreferences)
            }

            LaunchedEffect(userPreferences) {
                Compat.init(preferences.provider)
            }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences
            ) {
                AppTheme(
                    dynamicColor = preferences.dynamicColor
                ) {
                    Crossfade(
                        targetState = preferences.provider != Provider.None,
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

    override fun onDestroy() {
        PackageReceiver.unregister(this)
        super.onDestroy()
    }

    private fun setProvider(value: Provider) {
        lifecycleScope.launch {
            userPreferencesRepository.setProvider(value)
        }
    }
}