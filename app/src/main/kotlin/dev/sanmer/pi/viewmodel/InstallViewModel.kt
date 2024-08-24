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
import dev.sanmer.pi.compat.MediaStoreCompat.copyToFile
import dev.sanmer.pi.compat.MediaStoreCompat.getOwnerPackageNameForUri
import dev.sanmer.pi.compat.MediaStoreCompat.getPathForUri
import dev.sanmer.pi.compat.VersionCompat.sdkVersionDiff
import dev.sanmer.pi.compat.VersionCompat.versionDiff
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.service.InstallService
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
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { Compat.getPackageManager() }
    private val aom by lazy { Compat.getAppOpsService() }

    private val externalCacheDir inline get() = requireNotNull(context.externalCacheDir)
    private val uuid inline get() = UUID.randomUUID().toString()
    private var archivePath = File(externalCacheDir, uuid)

    var sourceInfo by mutableStateOf(IPackageInfo.empty())
        private set
    var archiveInfo by mutableStateOf(IPackageInfo.empty())
        private set
    val hasSourceInfo by lazy { sourceInfo.isNotEmpty }

    private val currentInfo by lazy { getPackageInfo(archiveInfo.packageName) }
    val versionDiff by lazy { currentInfo.versionDiff(archiveInfo) }
    val sdkDiff by lazy { currentInfo.sdkVersionDiff(archiveInfo) }

    private var baseSize = 0L
    private val totalSize by derivedStateOf { baseSize + requiredConfigs.sumOf { it.file.length() } }
    val totalSizeStr: String by derivedStateOf { Formatter.formatFileSize(context, totalSize) }

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
        context.copyToFile(uri, archivePath)
        PackageParserCompat.parsePackage(archivePath, 0)?.let { pi ->
            archiveInfo = pi.toIPackageInfo()
            baseSize = archivePath.length()

            Timber.i("loadPackage<Apk>: ${pi.packageName}")
            state = State.Apk
            return@withContext
        }

        val archivePathNew = File(externalCacheDir, uuid).apply { mkdirs() }
        PackageParserCompat.parseAppBundle(archivePath, 0, archivePathNew)?.let { bi ->
            archiveInfo = bi.baseInfo.toIPackageInfo()
            baseSize = bi.baseFile.length()

            splitConfigs = bi.splitConfigs
            requiredConfigs.addAll(
                bi.splitConfigs.filter { it.isRequired || it.isRecommended }
            )

            archivePath.delete()
            archivePath = archivePathNew

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

    fun install() = when (state) {
        State.Apk -> {
            InstallService.apk(
                context = context,
                archivePath = archivePath,
                archiveInfo = archiveInfo
            )
        }
        State.AppBundle -> {
            val filenames = requiredConfigs
                .map { it.file.name }
                .toMutableList()
                .apply {
                    add(0, PackageParserCompat.BASE_APK)
                }

            InstallService.appBundle(
                context = context,
                archivePath = archivePath,
                archiveInfo = archiveInfo,
                filenames = filenames
            )
        }
        else -> {}
    }

    fun deleteCache() {
        archivePath.deleteRecursively()
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
    ).isAllowed

    enum class State {
        None,
        InvalidProvider,
        InvalidPackage,
        Apk,
        AppBundle;

        val isReady inline get() = this == Apk || this == AppBundle
    }
}