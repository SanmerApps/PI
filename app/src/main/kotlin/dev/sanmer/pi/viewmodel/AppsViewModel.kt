package dev.sanmer.pi.viewmodel

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.PackageInfoCompat.isOverlayPackage
import dev.sanmer.pi.UserHandleCompat
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.ktx.combineToLatest
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Default.toIPackageInfo
import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val serviceRepository: ServiceRepository
) : ViewModel(), AppOpsManagerDelegate.AppOpsCallback {
    private val pm by lazy { serviceRepository.getPackageManager() }
    private val aom by lazy { serviceRepository.getAppOpsManager() }

    var isSearch by mutableStateOf(false)
        private set
    private val queryFlow = MutableStateFlow("")

    private val packagesFlow = MutableStateFlow(listOf<IPackageInfo>())
    private val cacheFlow = MutableStateFlow(listOf<IPackageInfo>())

    var loadState by mutableStateOf<LoadState>(LoadState.Pending)
        private set
    val apps inline get() = loadState.apps
    val isPending inline get() = loadState.isPending

    var isFailed by mutableStateOf(false)
        private set

    val isQueryEmpty get() = queryFlow.value.isEmpty()

    override fun opChanged(op: Int, uid: Int, packageName: String) {
        Timber.d("opChanged<${AppOpsManagerDelegate.opToName(op)}>: $packageName")

        viewModelScope.launch {
            packagesFlow.update { getPackages() }
        }
    }

    init {
        Timber.d("AppsViewModel init")
        serviceObserver()
        dataObserver()
        queryObserver()
    }

    private fun serviceObserver() {
        viewModelScope.launch {
            serviceRepository.state.collectLatest { state ->
                if (state.isSucceed) {
                    packagesFlow.update { getPackages() }

                    aom.startWatchingMode(
                        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
                        packageName = null,
                        callback = this@AppsViewModel
                    )
                }
                isFailed = state.isFailed
            }
        }

        addCloseable {
            if (serviceRepository.isSucceed) {
                aom.stopWatchingMode(callback = this)
            }
        }
    }

    private fun dataObserver() {
        viewModelScope.launch {
            packagesFlow.combineToLatest(preferenceRepository.data) { source, preferences ->
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
                loadState = LoadState.Ready(
                    source.filter {
                        if (key.isNotBlank()) {
                            it.appLabel.contains(key, ignoreCase = true)
                                    || it.packageName.contains(key, ignoreCase = true)
                        } else {
                            true
                        }
                    }
                )
            }
        }
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        if (!serviceRepository.isSucceed) return@withContext emptyList()

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
        queryFlow.value = key
    }

    fun openSearch() {
        isSearch = true
    }

    fun closeSearch() {
        isSearch = false
        queryFlow.value = ""
    }

    fun settings(packageInfo: IPackageInfo) = object : Settings {
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

        override suspend fun setRequester() =
            preferenceRepository.setRequester(packageInfo.packageName)

        override suspend fun setExecutor() =
            preferenceRepository.setExecutor(packageInfo.packageName)
    }

    private fun PackageInfo.isAuthorized() = aom.checkOpNoThrow(
        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
        packageInfo = this
    ).isAllowed

    interface Settings {
        suspend fun setAuthorized()
        suspend fun setRequester()
        suspend fun setExecutor()
    }

    sealed class LoadState {
        abstract val apps: List<IPackageInfo>

        data object Pending : LoadState() {
            override val apps = emptyList<IPackageInfo>()
        }

        data class Ready(
            override val apps: List<IPackageInfo>
        ) : LoadState()

        val isPending inline get() = this is Pending
    }
}