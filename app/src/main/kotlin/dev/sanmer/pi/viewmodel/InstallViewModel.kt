package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.Compat
import dev.sanmer.pi.ContextCompat.userId
import dev.sanmer.pi.PackageInfoCompat.isNotEmpty
import dev.sanmer.pi.PackageParserCompat
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.MediaStoreCompat.copyToDir
import dev.sanmer.pi.compat.MediaStoreCompat.getOwnerPackageNameForUri
import dev.sanmer.pi.compat.MediaStoreCompat.getPathForUri
import dev.sanmer.pi.compat.VersionCompat
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.AppOpsManagerDelegate.Mode.Companion.isAllowed
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.service.InstallService
import dev.sanmer.pi.utils.extensions.tmpDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InstallViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
) : AndroidViewModel(application), AppOpsManagerDelegate.AppOpsCallback {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { Compat.getPackageManager() }
    private val aom by lazy { Compat.getAppOpsService() }

    private var archivePath = File("")
    private val tempDir by lazy { context.tmpDir.resolve(UUID.randomUUID().toString()) }

    var sourceInfo by mutableStateOf(IPackageInfo.empty())
        private set
    var archiveInfo by mutableStateOf(IPackageInfo.empty())
        private set
    val hasSourceInfo by lazy { sourceInfo.isNotEmpty }

    private val currentInfo by lazy { getPackageInfo(archiveInfo.packageName) }
    val versionDiff by lazy { VersionCompat.getVersionDiff(currentInfo, archiveInfo) }
    val sdkDiff by lazy { VersionCompat.getSdkVersionDiff(currentInfo, archiveInfo) }

    private var baseSize = 0L
    private val totalSize by derivedStateOf { baseSize + requiredConfigs.sumOf { it.size } }
    val totalSizeStr: String by derivedStateOf {
        Formatter.formatFileSize(context, totalSize)
    }

    var splitConfigs = listOf<SplitConfig>()
        private set
    private val requiredConfigs = mutableStateListOf<SplitConfig>()

    var state by mutableStateOf(State.None)
        private set

    override fun opChanged(op: Int, uid: Int, packageName: String) {
        val isAuthorized = aom.checkOpNoThrow(op, uid, packageName).isAllowed()
        sourceInfo = sourceInfo.copy(isAuthorized = isAuthorized)
    }

    init {
        Timber.d("InstallViewModel init")
    }

    suspend fun loadPackage(uri: Uri) = withContext(Dispatchers.IO) {
        val userPreferences = userPreferencesRepository.data.first()

        if (!Compat.init(userPreferences.provider)) {
            state = State.InvalidProvider
            return@withContext
        }

        val packageName = context.getOwnerPackageNameForUri(uri)
        Timber.d("loadPackage<sourceInfo>: $packageName")
        val source = getPackageInfo(packageName)
        if (source.isNotEmpty) {
            sourceInfo = source.toIPackageInfo(
                isAuthorized = source.isAuthorized()
            )
        }

        Timber.d("loadPackage<path>: ${context.getPathForUri(uri)}")
        val path = context.copyToDir(uri, tempDir)
        PackageParserCompat.parsePackage(path, 0)?.let { pi ->
            archiveInfo = pi.toIPackageInfo()
            archivePath = path
            baseSize = archivePath.length()

            Timber.i("loadPackage<Apk>: ${pi.packageName}")
            state = State.Apk
            return@withContext
        }

        PackageParserCompat.parseAppBundle(path, 0, tempDir)?.let { bi ->
            archiveInfo = bi.baseInfo.toIPackageInfo()
            archivePath = tempDir
            baseSize = bi.baseFile.length()

            splitConfigs = bi.splitConfigs
            requiredConfigs.addAll(
                bi.splitConfigs.filter { it.isRequired || it.isRecommended }
            )

            Timber.i("loadPackage<AppBundle>: ${bi.baseInfo.packageName}")
            Timber.i("loadPackage<AppBundle>: allSplits = ${splitConfigs.size}")
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
        val setMode: (AppOpsManagerDelegate.Mode) -> Unit = {
            aom.setMode(
                op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
                packageInfo = sourceInfo,
                mode = it
            )
        }

        when {
            sourceInfo.isAuthorized -> {
                setMode(AppOpsManagerDelegate.Mode.Default)
                sourceInfo = sourceInfo.copy(isAuthorized = false)
            }
            else -> {
                setMode(AppOpsManagerDelegate.Mode.Allow)
                sourceInfo = sourceInfo.copy(isAuthorized = true)
            }
        }
    }

    fun startInstall() {
        val filenames = requiredConfigs
            .map { it.filename }
            .toMutableList()
            .apply {
                add(0, PackageParserCompat.BASE_APK)
            }

        if (state == State.AppBundle) {
            Timber.d("startInstall<AppBundle>: files = ${splitConfigs.size}")
        }

        InstallService.start(
            context = context,
            archivePath = archivePath,
            archiveInfo = archiveInfo,
            filenames = filenames
        )
    }

    fun deleteTempDir() {
        Timber.d("deleteTempDir")
        tempDir.deleteRecursively()
    }

    private fun getPackageInfo(packageName: String?): PackageInfo {
        if (packageName == null) return PackageInfo()
        return runCatching {
            pm.getPackageInfo(
                packageName, 0, context.userId
            )
        }.getOrNull() ?: PackageInfo()
    }

    private fun PackageInfo.isAuthorized() = aom.checkOpNoThrow(
        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
        packageInfo = this
    ).isAllowed()

    enum class State {
        None,
        InvalidProvider,
        InvalidPackage,
        Apk,
        AppBundle;

        companion object {
            fun State.isLoading() = this == None
            fun State.isFailed() = this == InvalidProvider || this == InvalidPackage
            fun State.isReady() = this == Apk || this == AppBundle
        }
    }
}