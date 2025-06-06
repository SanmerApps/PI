package dev.sanmer.pi.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.UserInfo
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sanmer.pi.PackageInfoCompat.isNotEmpty
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.VersionCompat.fileSize
import dev.sanmer.pi.compat.VersionCompat.getSdkVersionDiff
import dev.sanmer.pi.compat.VersionCompat.getVersionDiff
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Default.toIPackageInfo
import dev.sanmer.pi.repository.ServiceRepository
import dev.sanmer.pi.service.InstallService
import dev.sanmer.pi.service.InstallService.Task
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class InstallViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val pm by lazy { context.packageManager }
    private val um by lazy { serviceRepository.getUserManager() }

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
    val fileSize by derivedStateOf {
        (baseSize + requiredConfigs.sumOf { it.file.length() }).fileSize(context)
    }

    var splitConfigs = listOf<SplitConfig>()
        private set
    private val requiredConfigs = mutableStateListOf<SplitConfig>()

    private var type by mutableStateOf(Type.Apk)

    var users by mutableStateOf(listOf<UserInfoCompat>())
        private set
    var user by mutableStateOf(UserInfoCompat.Empty)
        private set

    init {
        Timber.d("InstallViewModel init")
        loadUsers()
    }

    private fun loadUsers() {
        runCatching {
            users = um.getUsers().map(::UserInfoCompat)
        }.onFailure {
            Timber.w(it)
        }
    }

    fun updateUser(userInfo: UserInfoCompat) {
        user = userInfo
    }

    fun load(task: Task) {
        archivePath = task.archivePath
        archiveInfo = task.archiveInfo.toIPackageInfo()
        sourceInfo = task.sourceInfo.toIPackageInfo()

        runCatching {
            user = UserInfoCompat(um.getUserInfo(task.userId))
        }.onFailure {
            Timber.w(it)
        }

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
                archiveInfo = archiveInfo,
                userId = user.id,
                sourceInfo = sourceInfo
            )
        }

        Type.AppBundle -> {
            InstallService.appBundle(
                context = context,
                archivePath = archivePath,
                archiveInfo = archiveInfo,
                splitConfigs = requiredConfigs,
                userId = user.id,
                sourceInfo = sourceInfo
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

    class UserInfoCompat(
        val id: Int,
        val name: String
    ) {
        constructor(userInfo: UserInfo) : this(
            id = userInfo.id,
            name = userInfo.name ?: userInfo.id.toString()
        )

        companion object Default {
            val Empty = UserInfoCompat(
                id = -1,
                name = ""
            )
        }
    }
}