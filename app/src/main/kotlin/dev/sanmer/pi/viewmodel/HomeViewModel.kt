package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.compat.ContextCompat.userId
import dev.sanmer.pi.compat.PackageInfoCompat.isOverlayPackage
import dev.sanmer.pi.compat.PackageInfoCompat.isPreinstalled
import dev.sanmer.pi.compat.PackageManagerCompat
import dev.sanmer.pi.compat.ShizukuCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val settingsRepository: SettingsRepository,
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { context.packageManager }

    val authorized get() = localRepository.getAuthorizedAllAsFlow().map { it.size }
    var requester: IPackageInfo? by mutableStateOf(null)
        private set
    var executor: IPackageInfo? by mutableStateOf(null)
        private set
    var packages = listOf<IPackageInfo>()
        private set

    init {
        Timber.d("HomeViewModel init")
    }

    suspend fun loadData() {
        viewModelScope.launch {
            if (!ShizukuCompat.isEnable) return@launch

            val packagesDeferred = async { getPackages() }

            val requesterPackageName = settingsRepository.getRequesterOrDefault()
            requester = PackageManagerCompat.getPackageInfo(
                requesterPackageName, 0, context.userId
            ).toIPackageInfo(pm = pm)

            val executorPackageName = settingsRepository.getExecutorOrDefault()
            executor = PackageManagerCompat.getPackageInfo(
                executorPackageName, 0, context.userId
            ).toIPackageInfo(pm = pm)

            packages = packagesDeferred.await()
        }
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        val allPackages = runCatching {
            PackageManagerCompat.getInstalledPackages(0, context.userId)
        }.onFailure {
            Timber.e(it, "getInstalledPackages")
        }.getOrDefault(emptyList())

        allPackages.filter {
            !it.isOverlayPackage && !it.isPreinstalled
        }.map {
            IPackageInfo(
                packageInfo = it,
                pm = pm
            )
        }.sortedBy {
            it.label.uppercase()
        }
    }

    fun setRequesterPackage(pi: IPackageInfo) {
        viewModelScope.launch {
            requester = pi
            settingsRepository.setRequester(pi.packageName)
        }
    }

    fun setExecutorPackage(pi: IPackageInfo) {
        viewModelScope.launch {
            executor = pi
            settingsRepository.setExecutor(pi.packageName)
        }
    }
}