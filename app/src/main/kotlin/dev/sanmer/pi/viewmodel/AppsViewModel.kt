package dev.sanmer.pi.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.Compat
import dev.sanmer.pi.PackageInfoCompat.isOverlayPackage
import dev.sanmer.pi.UserHandleCompat
import dev.sanmer.pi.compat.MediaStoreCompat.createDownloadUri
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.AppOpsManagerDelegate.Mode.Companion.isAllowed
import dev.sanmer.pi.ktx.viewPackage
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.receiver.PackageReceiver
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.InputStream
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
    private val keyFlow = MutableStateFlow("")

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
        packagesObserver()
        dataObserver()
        keyObserver()
    }

    private fun packagesObserver() {
        Compat.isAliveFlow
            .onEach { isAlive ->
                if (!isAlive) return@onEach

                packagesFlow.value = getPackages()

                aom.startWatchingMode(
                    op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
                    packageName = null,
                    callback = this
                )

            }.launchIn(viewModelScope)

        PackageReceiver.eventFlow
            .onEach {
                if (!isProviderAlive) return@onEach

                packagesFlow.value = getPackages()

            }.launchIn(viewModelScope)

        addCloseable {
            if (isProviderAlive) {
                aom.stopWatchingMode(callback = this)
            }
        }
    }

    private fun dataObserver() {
        combine(
            userPreferencesRepository.data,
            packagesFlow,
        ) { preferences, source ->
            if (source.isEmpty()) return@combine

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

            isLoading = false

        }.launchIn(viewModelScope)
    }

    private fun keyObserver() {
        combine(
            keyFlow,
            cacheFlow
        ) { key, source ->

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

        }.launchIn(viewModelScope)
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
        }
    }

    fun search(key: String) {
        keyFlow.value = key
    }

    fun openSearch() {
        isSearch = true
    }

    fun closeSearch() {
        isSearch = false
        keyFlow.value = ""
    }

    fun buildSettings(packageInfo: IPackageInfo) = object : Settings {
        private val launchIntent by lazy {
            pm.getLaunchIntentForPackage(
                packageInfo.packageName, UserHandleCompat.myUserId()
            )
        }

        override val isOpenable by lazy { launchIntent != null }

        override fun launch(context: Context) {
            context.startActivity(launchIntent)
        }

        override fun view(context: Context) {
            context.viewPackage(packageInfo.packageName)
        }

        override suspend fun export(context: Context): Boolean {
            val sourceDir = packageInfo.applicationInfo?.let { File(it.sourceDir) }
            if (sourceDir == null) return false

            val filename = with(packageInfo) { "${appLabel}-${versionName}-${longVersionCode}.apk" }
            val path = "PI" + File.separator + filename

            val files = sourceDir.parentFile?.listFiles { file ->
                file.name.endsWith(".apk")
            } ?: return false

            val streams = files.map { it to it.inputStream().buffered() }
            when {
                streams.size == 1 -> {
                    context.exportApk(
                        input = streams.first().second,
                        path = path
                    )
                }

                streams.size > 1 -> {
                    context.exportApks(
                        inputs = streams,
                        path = path + 's'
                    )
                }
            }

            streams.forEach { it.second.close() }
            return true
        }

        override suspend fun setAuthorized() {
            withContext(Dispatchers.IO) {
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
        }

        override suspend fun setRequester() {
            withContext(Dispatchers.IO) {
                userPreferencesRepository.setRequester(packageInfo.packageName)
            }
        }

        override suspend fun setExecutor() {
            withContext(Dispatchers.IO) {
                userPreferencesRepository.setExecutor(packageInfo.packageName)
            }
        }
    }

    private suspend fun Context.exportApk(
        input: InputStream,
        path: String,
    ) = withContext(Dispatchers.IO) {
        val uri = createDownloadUri(
            path = path,
            mimeType = "android/vnd.android.package-archive"
        )

        contentResolver.openOutputStream(uri)?.use { output ->
            input.copyTo(output)
            return@withContext true
        }

        false
    }

    private suspend fun Context.exportApks(
        inputs: List<Pair<File, InputStream>>,
        path: String,
    ) = withContext(Dispatchers.IO) {
        val uri = createDownloadUri(
            path = path,
            mimeType = "android/zip"
        )

        contentResolver.openOutputStream(uri)?.let(::ZipOutputStream)?.use { output ->
            inputs.forEach { (file, input) ->
                output.putNextEntry(ZipEntry(file.name))
                input.copyTo(output)
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
        val isOpenable: Boolean
        fun launch(context: Context)
        fun view(context: Context)
        suspend fun export(context: Context): Boolean
        suspend fun setAuthorized()
        suspend fun setRequester()
        suspend fun setExecutor()
    }
}