package dev.sanmer.pi.ui

import android.app.ComponentCaller
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.sanmer.pi.ui.main.MainScreen
import dev.sanmer.pi.ui.main.MainViewModel
import dev.sanmer.pi.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val viewModel by viewModel<MainViewModel>()

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        intent.data?.let { viewModel.fromUri(this, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        intent.data?.let { viewModel.fromUri(this, it) }

        setContent {
            AppTheme {
                MainScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}