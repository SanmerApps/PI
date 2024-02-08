package dev.sanmer.pi.ui.activity

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
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.datastore.Provider
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.providable.LocalUserPreferences
import dev.sanmer.pi.ui.theme.AppTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { isLoading }

        setContent {
            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(initialValue = null)

            if (userPreferences == null) {
                // Keep on splash screen
                return@setContent
            } else {
                isLoading = false
            }

            LaunchedEffect(userPreferences) {
                if (!ProviderCompat.isAlive) {
                    ProviderCompat.init(userPreferences!!.provider)
                }
            }

            CompositionLocalProvider(
                LocalUserPreferences provides userPreferences!!
            ) {
                AppTheme(
                    dynamicColor = userPreferences!!.dynamicColor
                ) {
                    Crossfade(
                        targetState = userPreferences!!.provider != Provider.None,
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

            if (BuildCompat.atLeastT) {
                NotificationUtils.PermissionState()
            }
        }
    }

    private fun setProvider(value: Provider) {
        lifecycleScope.launch {
            userPreferencesRepository.setProvider(value)
        }
    }
}