package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.PackageInfoCompat.isEmpty
import dev.sanmer.hidden.compat.PackageInfoCompat.isNotEmpty
import dev.sanmer.hidden.compat.PackageInfoCompat.isSystemApp
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.compat.ProviderCompat
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class InstallViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { context.packageManager }
    private val pmCompat get() = ProviderCompat.packageManagerCompat

    private var archiveUri = Uri.EMPTY
    private val tmpFile by lazy { context.tmpDir.resolve(Const.TEMP_PACKAGE) }
    var sourceInfo by mutableStateOf(IPackageInfo.empty())
        private set
    var archiveInfo by mutableStateOf(PackageInfo())
        private set
    private val isSelf get() = sourceInfo.packageName == archiveInfo.packageName
    var isAuthorized by mutableStateOf(false)
        private set

    val archiveLabel by lazy { archiveInfo.applicationInfo.loadLabel(pm).toString() }
    val currentInfo by lazy { getPackageInfoCompat(archiveInfo.packageName) }
    val versionDiff by lazy { VersionCompat.getVersionDiff(currentInfo, archiveInfo) }
    val sdkDiff by lazy { VersionCompat.getSdkVersionDiff(currentInfo, archiveInfo) }

    val isReady by derivedStateOf { archiveInfo.isNotEmpty && ProviderCompat.isAlive }

    suspend fun loadData(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val userPreferences = userPreferencesRepository.data.first()
        val selfUpdate = userPreferences.selfUpdate

        val sourcePackage = getSourcePackageForHost(uri)
        val source = getPackageInfo(sourcePackage)
        if (!source.isSystemApp) {
            isAuthorized = localRepository.getByPackageInfo(source)
            sourceInfo = source.toIPackageInfo(
                authorized = isAuthorized,
                pm = pm
            )
        }

        val cr = context.contentResolver
        cr.openInputStream(uri)?.use { input ->
            tmpFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext false

        val archive = getArchiveInfo(tmpFile)
        if (archive.isNotEmpty) {
            archiveUri = uri
            archiveInfo = archive
            isAuthorized = isAuthorized || (isSelf && selfUpdate)

            true
        } else {
            false
        }
    }

    fun toggleAuthorized() {
        if (sourceInfo.inner.isEmpty) return

        viewModelScope.launch {
            sourceInfo = sourceInfo.let {
                it.copy(authorized = !it.authorized)
            }

            localRepository.insert(sourceInfo)
        }
    }

    fun startInstall() {
        InstallService.start(
            context = context,
            archiveUri = archiveUri,
            archiveInfo = archiveInfo
        )
    }

    fun clearFile() {
        tmpFile.apply { if (exists()) delete() }
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

    private fun getArchiveInfo(archiveFile: File): PackageInfo {
        return pm.getPackageArchiveInfo(
            archiveFile.path, 0
        )?.also {
            it.applicationInfo.sourceDir = archiveFile.path
            it.applicationInfo.publicSourceDir = archiveFile.path
        } ?: PackageInfo()
    }
}