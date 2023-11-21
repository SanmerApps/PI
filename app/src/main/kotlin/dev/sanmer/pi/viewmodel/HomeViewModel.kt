package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.app.utils.ShizukuUtils
import dev.sanmer.pi.compat.PackageInfoCompat.isOverlayPackage
import dev.sanmer.pi.compat.PackageInfoCompat.isPreinstalled
import dev.sanmer.pi.compat.PackageManagerCompat
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
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
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pm by lazy { context.packageManager }

    val authorized get() = localRepository.getAuthorizedAllAsFlow().map { it.size }
    var requester: PackageInfo? by mutableStateOf(null)
        private set
    var executor: PackageInfo? by mutableStateOf(null)
        private set
    var packages = listOf<PackageInfo>()
        private set

    init {
        Timber.d("HomeViewModel init")
    }

    suspend fun loadData() {
        viewModelScope.launch {
            if (!ShizukuUtils.isEnable) return@launch

            val packagesDeferred = async { getPackages() }

            val requesterPackageName = userPreferencesRepository.getRequesterPackageNameOrDefault()
            requester = PackageManagerCompat.getPackageInfo(
                requesterPackageName, 0, 0
            )

            val executorPackageName = userPreferencesRepository.getExecutorPackageNameOrDefault()
            executor = PackageManagerCompat.getPackageInfo(
                executorPackageName, 0, 0
            )

            packages = packagesDeferred.await()
        }
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        val allPackages = runCatching {
            PackageManagerCompat.getInstalledPackages(0, 0)
        }.onFailure {
            Timber.e(it, "getInstalledPackages")
        }.getOrDefault(emptyList())

        allPackages.filter {
            !it.isOverlayPackage && !it.isPreinstalled
        }.sortedBy {
            it.applicationInfo.loadLabel(pm)
                .toString().uppercase()
        }
    }

    fun setRequesterPackage(pi: PackageInfo) {
        viewModelScope.launch {
            requester = pi
            userPreferencesRepository.setRequesterPackageName(pi.packageName)
        }
    }

    fun setExecutorPackage(pi: PackageInfo) {
        viewModelScope.launch {
            executor = pi
            userPreferencesRepository.setExecutorPackageName(pi.packageName)
        }
    }
}