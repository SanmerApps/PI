package dev.sanmer.pi.viewmodel

import android.Manifest
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.PackageInfoCompat.isNotEmpty
import dev.sanmer.hidden.compat.PackageParserCompat
import dev.sanmer.hidden.compat.content.bundle.SplitConfig
import dev.sanmer.hidden.compat.delegate.AppOpsManagerDelegate
import dev.sanmer.hidden.compat.delegate.AppOpsManagerDelegate.Mode.Companion.isAllowed
import dev.sanmer.pi.Compat
import dev.sanmer.pi.compat.MediaStoreCompat.copyToDir
import dev.sanmer.pi.compat.MediaStoreCompat.getOwnerPackageNameForUri
import dev.sanmer.pi.compat.MediaStoreCompat.getPathForUri
import dev.sanmer.pi.compat.VersionCompat
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
    private val pm by lazy { context.packageManager }
    private val pmCompat get() = Compat.packageManager
    private val aom by lazy {
        AppOpsManagerDelegate(
            Compat.appOpsService
        )
    }

    private var archivePath = File("")
    private val tempDir by lazy { context.tmpDir.resolve(UUID.randomUUID().toString()) }

    var sourceInfo by mutableStateOf(IPackageInfo.empty())
        private set
    var archiveInfo by mutableStateOf(PackageInfo())
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
        if (source.hasOpInstallPackage()) {
            sourceInfo = source.toIPackageInfo(
                isAuthorized = source.isAuthorized()
            )
        }

        Timber.d("loadPackage<path>: ${context.getPathForUri(uri)}")
        val path = context.copyToDir(uri, tempDir)
        PackageParserCompat.parsePackage(path, 0)?.let { pi ->
            archiveInfo = pi
            archivePath = path
            apkSize = archivePath.length()

            Timber.i("loadPackage<Apk>: ${pi.packageName}")
            state = State.Apk
            return@withContext
        }

        PackageParserCompat.parseAppBundle(path, 0, tempDir)?.let { bi ->
            archiveInfo = bi.baseInfo
            archivePath = tempDir
            apkSize = bi.baseFile.length() + bi.splitFiles.sumOf { it.length() }

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

    private fun PackageInfo.isAuthorized() = aom.checkOpNoThrow(
        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
        packageInfo = this
    ).isAllowed()

    private fun PackageInfo.hasOpInstallPackage() =
        requestedPermissions?.contains(
            Manifest.permission.REQUEST_INSTALL_PACKAGES
        ) == true ||
                aom.getOpsForPackage(this).map { it.op }
                    .contains(AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES)

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