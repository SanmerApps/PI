package dev.sanmer.pi.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.sanmer.pi.ktx.parcelable
import dev.sanmer.pi.ktx.parcelableArrayList
import dev.sanmer.pi.ui.main.HomeViewModel
import dev.sanmer.pi.ui.main.MainScreen
import dev.sanmer.pi.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val viewModel by viewModel<HomeViewModel>()

    @Suppress("DEPRECATION")
    fun fromIntent(intent: Intent) = when (intent.action) {
        Intent.ACTION_VIEW, Intent.ACTION_INSTALL_PACKAGE -> intent.data
            ?.let { viewModel.fromUri(this, it) }

        Intent.ACTION_SEND -> intent.parcelable<Uri>(Intent.EXTRA_STREAM)
            ?.let { viewModel.fromUri(this, it) }

        Intent.ACTION_SEND_MULTIPLE -> intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
            .forEach { viewModel.fromUri(this, it) }

        else -> {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        fromIntent(intent)

        splashScreen.setKeepOnScreenCondition { viewModel.state.isLoading }

        setContent {
            AppTheme {
                MainScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}