package dev.sanmer.pi.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import dev.sanmer.pi.ui.main.MainViewModel
import dev.sanmer.pi.ui.main.MainViewModel.LoadState
import dev.sanmer.pi.ui.provider.LocalPreference
import dev.sanmer.pi.ui.screens.install.InstallScreen
import dev.sanmer.pi.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallActivity : BaseInstallActivity() {
    private val viewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            when (viewModel.loadState) {
                LoadState.Pending -> {}
                is LoadState.Ready -> CompositionLocalProvider(
                    LocalPreference provides viewModel.preference
                ) {
                    AppTheme(
                        darkMode = viewModel.preference.darkMode.isDarkTheme
                    ) {
                        InstallScreen()
                    }
                }
            }
        }
    }
}