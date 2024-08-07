package dev.sanmer.pi.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.Compat
import dev.sanmer.pi.PackageInfoCompat.isOverlayPackage
import dev.sanmer.pi.UserHandleCompat
import dev.sanmer.pi.compat.MediaStoreCompat.createMediaStoreUri
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.AppOpsManagerDelegate.Mode.Companion.isAllowed
import dev.sanmer.pi.ktx.combineToLatest
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel(), AppOpsManagerDelegate.AppOpsCallback {
    private val isProviderAlive get() = Compat.isAlive
    private val pm by lazy { Compat.getPackageManager() }
    private val aom by lazy { Compat.getAppOpsService() }

    var isSearch by mutableStateOf(false)
        private set
    private val queryFlow = MutableStateFlow("")

    private val packagesFlow = MutableStateFlow(listOf<IPackageInfo>())
    private val cacheFlow = MutableStateFlow(listOf<IPackageInfo>())
    private val appsFlow = MutableStateFlow(listOf<IPackageInfo>())
    val apps get() = appsFlow.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set

    override fun opChanged(op: Int, uid: Int, packageName: String) {
        Timber.d("opChanged<${AppOpsManagerDelegate.opToName(op)}>: $packageName")

        viewModelScope.launch {
            packagesFlow.value = getPackages()
        }
    }

    init {
        Timber.d("AppsViewModel init")
        providerObserver()
        dataObserver()
        queryObserver()
    }

    private fun providerObserver() {
        viewModelScope.launch {
            Compat.isAliveFlow.collectLatest { isAlive ->
                if (isAlive) {
                    packagesFlow.update { getPackages() }

                    aom.startWatchingMode(
                        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
                        packageName = null,
                        callback = this@AppsViewModel
                    )
                }
            }
        }

        addCloseable {
            if (isProviderAlive) {
                aom.stopWatchingMode(callback = this)
            }
        }
    }

    private fun dataObserver() {
        viewModelScope.launch {
            packagesFlow.combineToLatest(userPreferencesRepository.data) { source, preferences ->
                cacheFlow.update {
                    source.map { pi ->
                        pi.copy(
                            isRequester = preferences.requester == pi.packageName,
                            isExecutor = preferences.executor == pi.packageName
                        )
                    }.sortedByDescending { it.lastUpdateTime }
                        .sortedByDescending { it.isAuthorized }
                        .sortedByDescending { it.isExecutor || it.isRequester }
                }
            }
        }
    }

    private fun queryObserver() {
        viewModelScope.launch {
            cacheFlow.combineToLatest(queryFlow) { source, key ->
                appsFlow.update {
                    source.filter {
                        if (key.isNotBlank()) {
                            it.appLabel.contains(key, ignoreCase = true)
                                    || it.packageName.contains(key, ignoreCase = true)
                        } else {
                            true
                        }
                    }
                }
            }
        }
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        if (!isProviderAlive) return@withContext emptyList()

        val allPackages = pm.getInstalledPackages(
            PackageManager.GET_PERMISSIONS, UserHandleCompat.myUserId()
        )

        allPackages.filter {
            !it.isOverlayPackage
        }.map {
            it.toIPackageInfo(
                isAuthorized = it.isAuthorized()
            )
        }.also {
            isLoading = it.isEmpty()
        }
    }

    fun search(key: String) {
        queryFlow.value = key
    }

    fun openSearch() {
        isSearch = true
    }

    fun closeSearch() {
        isSearch = false
        queryFlow.value = ""
    }

    fun buildSettings(packageInfo: IPackageInfo) = object : Settings {
        override suspend fun export(context: Context): Boolean {
            val sourceDir = packageInfo.applicationInfo?.let { File(it.sourceDir) }
            if (sourceDir == null) return false

            val files = sourceDir.parentFile?.listFiles { file ->
                file.name.endsWith(".apk")
            } ?: return false

            val filename = with(packageInfo) { "${appLabel}-${versionName}-${longVersionCode}.apk" }

            return when {
                files.size == 1 -> context.exportApk(
                    file = files.first(),
                    path = "PI/${filename}"
                )

                files.size > 1 -> context.exportApks(
                    files = files.toList(),
                    path = "PI/${filename}s"
                )

                else -> false
            }
        }

        override suspend fun setAuthorized() {
            val setMode: (AppOpsManagerDelegate.Mode) -> Unit = {
                aom.setMode(
                    op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
                    packageInfo = packageInfo,
                    mode = it
                )
            }

            when {
                packageInfo.isAuthorized -> setMode(AppOpsManagerDelegate.Mode.Default)
                else -> setMode(AppOpsManagerDelegate.Mode.Allow)
            }
        }

        override suspend fun setRequester() {
            userPreferencesRepository.setRequester(packageInfo.packageName)
        }

        override suspend fun setExecutor() {
            userPreferencesRepository.setExecutor(packageInfo.packageName)
        }
    }

    private suspend fun Context.exportApk(
        file: File,
        path: String,
    ) = withContext(Dispatchers.IO) {
        val uri = createMediaStoreUri(
            file = File(Environment.DIRECTORY_DOWNLOADS, path),
            mimeType = "android/vnd.android.package-archive"
        )

        contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().buffered().copyTo(output)
            return@withContext true
        }

        false
    }

    private suspend fun Context.exportApks(
        files: List<File>,
        path: String,
    ) = withContext(Dispatchers.IO) {
        val uri = createMediaStoreUri(
            file = File(Environment.DIRECTORY_DOWNLOADS, path),
            mimeType = "android/zip"
        )

        contentResolver.openOutputStream(uri)?.let(::ZipOutputStream)?.use { output ->
            files.forEach { file ->
                output.putNextEntry(ZipEntry(file.name))
                file.inputStream().buffered().copyTo(output)
                output.closeEntry()
            }

            return@withContext true
        }

        false
    }

    private fun PackageInfo.isAuthorized() = aom.checkOpNoThrow(
        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
        packageInfo = this
    ).isAllowed()

    interface Settings {
        suspend fun export(context: Context): Boolean
        suspend fun setAuthorized()
        suspend fun setRequester()
        suspend fun setExecutor()
    }
}