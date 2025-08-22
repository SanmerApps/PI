package dev.sanmer.pi.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.Logger
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.model.Task
import dev.sanmer.pi.model.Task.Default.putTask
import dev.sanmer.pi.model.Task.Default.taskOrNull
import dev.sanmer.pi.service.ParseService
import dev.sanmer.pi.ui.main.MainViewModel
import dev.sanmer.pi.ui.main.MainViewModel.LoadState
import dev.sanmer.pi.ui.screens.install.InstallScreen
import dev.sanmer.pi.ui.screens.install.InstallViewModel
import dev.sanmer.pi.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

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
        if (uri != null) {
            ParseService.start(this, uri)
            finish()
            return
        }

        val task = intent.taskOrNull
        if (task != null) {
            viewModel.load(task)
        } else {
            finish()
            return
        }

        setContent {
            when (main.loadState) {
                LoadState.Pending -> {}
                is LoadState.Ready -> AppTheme(
                    darkMode = main.preference.darkMode.isDarkTheme
                ) {
                    InstallScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        logger.d("onDestroy")
        super.onDestroy()
    }

    companion object Default {
        fun apk(
            context: Context,
            archivePath: File,
            archiveInfo: PackageInfo,
            sourceInfo: PackageInfo,
            userId: Int = context.userId
        ) {
            val task = Task.Apk(
                archivePath = archivePath,
                archiveInfo = archiveInfo,
                userId = userId,
                sourceInfo = sourceInfo
            )
            context.startActivity(
                Intent(context, InstallActivity::class.java).also {
                    it.putTask(task)
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }

        fun appBundle(
            context: Context,
            archivePath: File,
            archiveInfo: PackageInfo,
            splitConfigs: List<SplitConfig>,
            sourceInfo: PackageInfo,
            userId: Int = context.userId
        ) {
            val task = Task.AppBundle(
                archivePath = archivePath,
                archiveInfo = archiveInfo,
                splitConfigs = splitConfigs,
                userId = userId,
                sourceInfo = sourceInfo
            )
            context.startActivity(
                Intent(context, InstallActivity::class.java).also {
                    it.putTask(task)
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
}