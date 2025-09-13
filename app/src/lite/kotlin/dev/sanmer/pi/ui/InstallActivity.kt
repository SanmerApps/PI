package dev.sanmer.pi.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import dev.sanmer.pi.ui.screens.install.InstallScreen
import dev.sanmer.pi.ui.theme.AppTheme

class InstallActivity : BaseInstallActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                InstallScreen()
            }
        }
    }
}