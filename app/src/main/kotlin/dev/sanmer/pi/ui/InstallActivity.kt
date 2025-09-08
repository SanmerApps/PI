package dev.sanmer.pi.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import dev.sanmer.pi.Logger
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.ui.main.MainViewModel
import dev.sanmer.pi.ui.main.MainViewModel.LoadState
import dev.sanmer.pi.ui.provider.LocalPreference
import dev.sanmer.pi.ui.screens.install.InstallScreen
import dev.sanmer.pi.ui.screens.install.InstallViewModel
import dev.sanmer.pi.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallActivity : ComponentActivity() {
    private val main by viewModel<MainViewModel>()
    private val viewModel by viewModel<InstallViewModel>()

    private val logger = Logger.Android("InstallActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.d("onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildCompat.atLeastT) {
            PermissionCompat.requestPermission(
                context = this,
                permission = Manifest.permission.POST_NOTIFICATIONS
            )
        }

        val uri = intent.data
        if (uri == null) {
            finish()
            return
        } else {
            viewModel.loadFromUri(uri)
        }

        setContent {
            when (main.loadState) {
                LoadState.Pending -> {}
                is LoadState.Ready -> CompositionLocalProvider(
                    LocalPreference provides main.preference
                ) {
                    AppTheme(
                        darkMode = main.preference.darkMode.isDarkTheme
                    ) {
                        InstallScreen()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        logger.d("onDestroy")
        super.onDestroy()
    }
}