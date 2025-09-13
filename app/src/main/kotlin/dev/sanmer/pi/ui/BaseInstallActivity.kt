package dev.sanmer.pi.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import dev.sanmer.pi.Logger
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.ui.screens.install.InstallViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BaseInstallActivity : ComponentActivity() {
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
    }

    override fun onDestroy() {
        logger.d("onDestroy")
        super.onDestroy()
    }
}