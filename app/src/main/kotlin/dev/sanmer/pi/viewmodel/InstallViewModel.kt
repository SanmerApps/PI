package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.PackageInfoCompat.isEmpty
import dev.sanmer.hidden.compat.PackageInfoCompat.isNotEmpty
import dev.sanmer.hidden.compat.PackageInfoCompat.isSystemApp
import dev.sanmer.hidden.compat.PackageParserCompat
import dev.sanmer.hidden.compat.content.bundle.SplitConfig
import dev.sanmer.pi.Compat
import dev.sanmer.pi.compat.MediaStoreCompat.copyToDir
import dev.sanmer.pi.compat.VersionCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.service.InstallService
import dev.sanmer.pi.utils.extensions.tmpDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InstallViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { context.packageManager }
    private val pmCompat get() = Compat.packageManager

    private var archivePath = File("")
    private val tempDir by lazy { context.tmpDir.resolve(UUID.randomUUID().toString()) }

    var sourceInfo by mutableStateOf(IPackageInfo.empty())
        private set
    var archiveInfo by mutableStateOf(PackageInfo())
        private set
    private val isSelf get() = sourceInfo.packageName == archiveInfo.packageName
    var isAuthorized = false
        private set
    val hasSourceInfo get() = sourceInfo.isNotEmpty

    val archiveLabel by lazy { archiveInfo.applicationInfo.loadLabel(pm).toString() }
    private val currentInfo by lazy { getPackageInfoCompat(archiveInfo.packageName) }
    val versionDiff by lazy { VersionCompat.getVersionDiff(currentInfo, archiveInfo) }
    val sdkDiff by lazy { VersionCompat.getSdkVersionDiff(currentInfo, archiveInfo) }

    private var apkSize = 0L
    val formattedApkSize: String by lazy { Formatter.formatFileSize(context, apkSize) }

    var splitConfigs = listOf<SplitConfig>()
        private set
    private val requiredConfigs = mutableStateListOf<SplitConfig>()

    var state by mutableStateOf(State.None)
        private set

    init {
        Timber.d("InstallViewModel init")
    }

    suspend fun loadPackage(uri: Uri) = withContext(Dispatchers.IO) {
        val userPreferences = userPreferencesRepository.data.first()
        val selfUpdate = userPreferences.selfUpdate

        if (!Compat.init(userPreferences.provider)) {
            state = State.InvalidProvider
            return@withContext
        }

        val packageName = getSourcePackageForHost(uri)
        val source = getPackageInfo(packageName)
        if (!source.isSystemApp) {
            isAuthorized = localRepository.getByPackageInfo(source)
            sourceInfo = source.toIPackageInfo(
                isAuthorized = isAuthorized
            )
        }

        val path = context.copyToDir(uri, tempDir)
        PackageParserCompat.parsePackage(path, 0)?.let { pi ->
            archiveInfo = pi
            archivePath = path
            apkSize = archivePath.length()

            isAuthorized = isAuthorized || (isSelf && selfUpdate)
            state = State.Apk
            return@withContext
        }

        PackageParserCompat.parseAppBundle(path, 0, tempDir)?.let { bi ->
            archiveInfo = bi.baseInfo
            archivePath = tempDir
            apkSize = bi.baseFile.length() + bi.splitFiles.sumOf { it.length() }

            splitConfigs = bi.splitConfigs
            requiredConfigs.addAll(
                bi.splitConfigs.filter { it.isRequired() || it.isRecommended() }
            )

            isAuthorized = false
            state = State.AppBundle
            return@withContext
        }

        state = State.InvalidPackage
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

    fun toggleAuthorized() {
        if (sourceInfo.isEmpty) return

        viewModelScope.launch {
            sourceInfo = sourceInfo.let {
                it.copy(isAuthorized = !it.isAuthorized)
            }

            localRepository.insertPackage(sourceInfo)
        }
    }

    fun startInstall() {
        val splitConfigs = requiredConfigs
            .map { it.filename }
            .toMutableList()
            .apply {
                add(0, PackageParserCompat.BASE_APK)
            }

        InstallService.start(
            context = context,
            archivePath = archivePath,
            archiveInfo = archiveInfo,
            splitConfigs = splitConfigs
        )
    }

    fun deleteTempDir() {
        tempDir.deleteRecursively()
    }

    private fun getSourcePackageForHost(uri: Uri): String? {
        val host = uri.host ?: return null
        return runCatching {
            pm.resolveContentProvider(
                host, 0
            )?.packageName
        }.getOrNull()
    }

    private fun getPackageInfo(packageName: String?): PackageInfo {
        if (packageName == null) return PackageInfo()
        return runCatching {
            pm.getPackageInfo(
                packageName, 0
            )
        }.getOrNull() ?: PackageInfo()
    }

    private fun getPackageInfoCompat(packageName: String?): PackageInfo {
        if (packageName == null) return PackageInfo()
        return runCatching {
            pmCompat.getPackageInfo(
                packageName, 0, context.userId
            )
        }.getOrNull() ?: PackageInfo()
    }

    enum class State {
        None,
        InvalidProvider,
        InvalidPackage,
        Apk,
        AppBundle
    }
}