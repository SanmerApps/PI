package dev.sanmer.pi.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.app.utils.NotificationUtils
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.ui.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            if (BuildCompat.atLeastT) {
                NotificationUtils.PermissionState()
            }

            AppTheme {
                MainScreen()
            }
        }
    }
}