package dev.sanmer.pi.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.providable.LocalUserPreferences
import dev.sanmer.pi.ui.theme.AppTheme
import dev.sanmer.pi.viewmodel.InstallViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class InstallActivity : ComponentActivity() {
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: InstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("InstallActivity onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent.data == null) {
            finish()
        }

        if (BuildCompat.atLeastT) {
            val permission = listOf(
                Manifest.permission.POST_NOTIFICATIONS
            )

            PermissionCompat.requestPermissions(this, permission) { state ->
                if (!state.allGranted) {
                    Timber.w("permission: $state")
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

            LaunchedEffect(ProviderCompat.isAlive) {
                if (ProviderCompat.isAlive) {
                    initPackage(intent)
                } else {
                    ProviderCompat.init(preferences.provider)
                }
            }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences
            ) {
                AppTheme(
                    dynamicColor = preferences.dynamicColor
                ) {
                    InstallScreen(
                        onFinish = this::finish
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.d("InstallActivity onDestroy")
        setResult(Activity.RESULT_OK)
        super.onDestroy()
    }

    private fun initPackage(intent: Intent) = lifecycleScope.launch {
        val packageUri = checkNotNull(intent.data)
        val isOk = viewModel.loadData(packageUri)
        if (isOk && viewModel.isAuthorized) {
            viewModel.startInstall()
            finish()
        }
    }
}