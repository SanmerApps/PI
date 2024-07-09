package dev.sanmer.pi.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.main.InstallScreen
import dev.sanmer.pi.ui.providable.LocalUserPreferences
import dev.sanmer.pi.ui.theme.AppTheme
import dev.sanmer.pi.viewmodel.InstallViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class InstallActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: InstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent.data == null) {
            finish()
        } else {
            initPackage(intent)
        }

        if (BuildCompat.atLeastT) {
            val permission = listOf(Manifest.permission.POST_NOTIFICATIONS)
            PermissionCompat.requestPermissions(this, permission) { state ->
                if (!state.allGranted) {
                    Timber.w("notGranted: $state")
                }
            }
        }

        setContent {
            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(initialValue = null)

            val preferences = if (userPreferences == null) {
                return@setContent
            } else {
                checkNotNull(userPreferences)
            }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences
            ) {
                AppTheme(
                    dynamicColor = preferences.dynamicColor
                ) {
                    InstallScreen()
                }
            }
        }
    }

    override fun finish() {
        setResult(RESULT_OK)
        super.finish()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
    }

    private fun initPackage(intent: Intent) = with(viewModel) {
        lifecycleScope.launch {
            loadPackage(checkNotNull(intent.data))
            if (sourceInfo.isAuthorized && state != InstallViewModel.State.AppBundle) {
                startInstall()
                finish()
            }
        }
    }
}