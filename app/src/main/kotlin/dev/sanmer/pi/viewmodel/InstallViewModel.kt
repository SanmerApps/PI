package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.text.format.Formatter
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.PackageInfoCompat.isNotEmpty
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.VersionCompat.getSdkVersionDiff
import dev.sanmer.pi.compat.VersionCompat.getVersionDiff
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Default.toIPackageInfo
import dev.sanmer.pi.service.InstallService
import dev.sanmer.pi.service.InstallService.Task
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class InstallViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { context.packageManager }

    private var archivePath = File(".")
    var sourceInfo by mutableStateOf(IPackageInfo.empty())
        private set
    var archiveInfo by mutableStateOf(IPackageInfo.empty())
        private set
    val hasSourceInfo by lazy { sourceInfo.isNotEmpty }

    private val currentInfo by lazy { getPackageInfo(archiveInfo.packageName) }
    val versionDiff by lazy { currentInfo.getVersionDiff(context, archiveInfo) }
    val sdkVersionDiff by lazy { currentInfo.getSdkVersionDiff(context, archiveInfo) }

    private var baseSize = 0L
    private val fileSize by derivedStateOf { baseSize + requiredConfigs.sumOf { it.file.length() } }
    val fileSizeStr: String by derivedStateOf { Formatter.formatFileSize(context, fileSize) }

    var splitConfigs = listOf<SplitConfig>()
        private set
    private val requiredConfigs = mutableStateListOf<SplitConfig>()

    private var type by mutableStateOf(Type.Apk)

    init {
        Timber.d("InstallViewModel init")
    }

    fun load(task: Task, source: PackageInfo?) {
        sourceInfo = (source ?: PackageInfo()).toIPackageInfo()
        archivePath = task.archivePath
        archiveInfo = task.archiveInfo.toIPackageInfo()

        when (task) {
            is Task.Apk -> {
                baseSize = archivePath.length()
            }

            is Task.AppBundle -> {
                type = Type.AppBundle
                baseSize = task.baseFile.length()
                splitConfigs = task.splitConfigs
                requiredConfigs.addAll(
                    task.splitConfigs.filter { it.isRequired || it.isRecommended }
                )
            }
        }
    }

    fun isRequiredConfig(config: SplitConfig): Boolean {
        return config in requiredConfigs
    }

    fun toggleSplitConfig(config: SplitConfig) {
        if (isRequiredConfig(config)) {
            requiredConfigs.remove(config)
        } else {
            requiredConfigs.add(config)
        }
    }

    fun install() = when (type) {
        Type.Apk -> {
            InstallService.apk(
                context = context,
                archivePath = archivePath,
                archiveInfo = archiveInfo
            )
        }

        Type.AppBundle -> {
            InstallService.appBundle(
                context = context,
                archivePath = archivePath,
                archiveInfo = archiveInfo,
                splitConfigs = requiredConfigs
            )
        }
    }

    fun deleteCache() {
        archivePath.deleteRecursively()
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        return runCatching {
            pm.getPackageInfo(
                packageName, 0
            )
        }.getOrNull() ?: PackageInfo()
    }

    enum class Type {
        Apk,
        AppBundle
    }
}