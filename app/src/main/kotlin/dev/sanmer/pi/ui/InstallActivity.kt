package dev.sanmer.pi.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.service.InstallService.Default.putTask
import dev.sanmer.pi.service.InstallService.Default.taskOrNull
import dev.sanmer.pi.service.InstallService.Task
import dev.sanmer.pi.service.ParseService
import dev.sanmer.pi.ui.screens.install.InstallScreen
import dev.sanmer.pi.ui.theme.AppTheme
import dev.sanmer.pi.viewmodel.InstallViewModel
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class InstallActivity : ComponentActivity() {
    private val viewModel: InstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
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
            AppTheme {
                InstallScreen()
            }
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
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