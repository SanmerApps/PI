package dev.sanmer.pi.ui.screens.install

import android.content.Context
import android.content.pm.UserInfo
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sanmer.pi.Logger
import dev.sanmer.pi.UserHandleCompat
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.factory.BundleFactory
import dev.sanmer.pi.factory.VersionFactory
import dev.sanmer.pi.factory.VersionFactory.Default.version
import dev.sanmer.pi.ktx.orEmpty
import dev.sanmer.pi.model.ServiceState
import dev.sanmer.pi.parser.PackageInfoLite
import dev.sanmer.pi.parser.SplitConfig
import dev.sanmer.pi.repository.ServiceRepository
import dev.sanmer.pi.service.InstallService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InstallViewModel(
    private val serviceRepository: ServiceRepository,
    private val bundleFactory: BundleFactory,
    private val versionFactory: VersionFactory
) : ViewModel() {
    var serviceState by mutableStateOf<ServiceState>(ServiceState.Pending)
        private set
    val isServiceReady inline get() = serviceState.isSucceed
    var loadState by mutableStateOf<LoadState>(LoadState.Pending)
        private set
    val isBundleReady inline get() = loadState is LoadState.Success
    val isReady inline get() = isServiceReady && isBundleReady

    var users by mutableStateOf(listOf<UserInfo>())
        private set
    var user by mutableStateOf(UserInfo(-1, "", 0))

    private var data: BundleFactory.Data? = null
    private var sizeBytes by mutableLongStateOf(0)
    val size by derivedStateOf { versionFactory.fileSize(sizeBytes) }

    var splitConfigs = listOf<SplitConfig>()
        private set
    private val requiredConfigs = mutableStateListOf<SplitConfig>()

    val logger = Logger.Android("InstallViewModel")

    init {
        logger.d("init")
        serviceObserver()
    }

    private fun serviceObserver() {
        viewModelScope.launch {
            serviceRepository.state.collectLatest {
                serviceState = it
                if (it.isSucceed) {
                    loadUsers()
                }
            }
        }
    }

    private fun loadUsers() {
        val um = serviceRepository.getUserManager()
        users = um.getUsers()
        user = um.getUserInfo(UserHandleCompat.myUserId())
    }

    fun recreate(provider: Provider) {
        viewModelScope.launch {
            serviceRepository.recreate(provider)
        }
    }

    private fun BundleFactory.Data.toSuccess(): LoadState.Success {
        sizeBytes = bundleInfo.sizeBytes + bundleInfo.splitConfigs.sumOf { it.sizeBytes }
        splitConfigs = bundleInfo.splitConfigs
        requiredConfigs.addAll(splitConfigs.filter { it.isRequired || it.isRecommended })

        val sourceInfo = sourceInfo?.run {
            copy(
                versionName = (longVersionCode to versionName).version,
                compileSdkVersionCodename = versionFactory.sdkVersions(
                    target = targetSdkVersion,
                    min = minSdkVersion,
                    compile = compileSdkVersion
                )
            )
        }

        val currentInfo = currentInfo.orEmpty()
        val archiveInfo = bundleInfo.packageInfo.run {
            copy(
                versionName = versionFactory.versionDiff(
                    that = with(this) { longVersionCode to versionName },
                    other = with(currentInfo) { longVersionCode to versionName }
                ),
                compileSdkVersionCodename = versionFactory.sdkVersionsDiff(
                    target = targetSdkVersion to currentInfo.targetSdkVersion,
                    min = minSdkVersion to currentInfo.minSdkVersion,
                    compile = compileSdkVersion to currentInfo.compileSdkVersion
                )
            )
        }

        return LoadState.Success(
            sourceInfo = sourceInfo,
            archiveInfo = archiveInfo
        )
    }

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                loadState = bundleFactory.load(uri)
                    .also {
                        data = it
                    }.toSuccess()
            }.onFailure {
                loadState = LoadState.Failure(it)
                logger.e(it)
            }
        }
    }

    fun isRequiredConfig(config: SplitConfig): Boolean {
        return config in requiredConfigs
    }

    fun toggleSplitConfig(config: SplitConfig) {
        if (isRequiredConfig(config)) {
            requiredConfigs.remove(config)
            sizeBytes -= config.sizeBytes
        } else {
            requiredConfigs.add(config)
            sizeBytes += config.sizeBytes
        }
    }

    fun start(context: Context) {
        val data = checkNotNull(data)
        val fileNames = if (data.bundleInfo.isZip) {
            mutableListOf(data.bundleInfo.fileName)
                .apply { addAll(requiredConfigs.map { it.fileName }) }
        } else {
            emptyList()
        }

        InstallService.start(
            context = context,
            uri = data.uri,
            archiveInfo = data.bundleInfo.packageInfo,
            fileNames = fileNames,
            sourceInfo = data.sourceInfo,
            userId = user.id
        )
    }

    sealed class LoadState {
        data object Pending : LoadState()
        data class Success(
            val sourceInfo: PackageInfoLite?,
            val archiveInfo: PackageInfoLite
        ) : LoadState()

        data class Failure(val error: Throwable) : LoadState()
    }
}