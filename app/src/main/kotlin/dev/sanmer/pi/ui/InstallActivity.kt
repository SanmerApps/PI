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
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.compat.PermissionCompat
import dev.sanmer.pi.ktx.parcelable
import dev.sanmer.pi.service.InstallService.Default.putTask
import dev.sanmer.pi.service.InstallService.Default.taskOrNull
import dev.sanmer.pi.service.InstallService.Task
import dev.sanmer.pi.service.ParseService
import dev.sanmer.pi.ui.main.InstallScreen
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
            viewModel.load(task, intent.sourceInfoOrNull)
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
        private const val EXTRA_SOURCE_INFO = "dev.sanmer.pi.extra.SOURCE_INFO"

        private fun Intent.putSourceInfo(value: PackageInfo?) =
            putExtra(EXTRA_SOURCE_INFO, value)

        private val Intent.sourceInfoOrNull: PackageInfo?
            get() = parcelable(EXTRA_SOURCE_INFO)

        fun apk(
            context: Context,
            archivePath: File,
            archiveInfo: PackageInfo,
            sourceInfo: PackageInfo?,
        ) {
            val task = Task.Apk(archivePath, archiveInfo)
            context.startActivity(
                Intent(context, InstallActivity::class.java).also {
                    it.putTask(task)
                    it.putSourceInfo(sourceInfo)
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }

        fun appBundle(
            context: Context,
            archivePath: File,
            archiveInfo: PackageInfo,
            splitConfigs: List<SplitConfig>,
            sourceInfo: PackageInfo?,
        ) {
            val task = Task.AppBundle(archivePath, archiveInfo, splitConfigs)
            context.startActivity(
                Intent(context, InstallActivity::class.java).also {
                    it.putTask(task)
                    it.putSourceInfo(sourceInfo)
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
}