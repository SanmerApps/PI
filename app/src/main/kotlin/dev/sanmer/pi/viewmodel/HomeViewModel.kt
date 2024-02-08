package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.PackageInfoCompat.isOverlayPackage
import dev.sanmer.hidden.compat.PackageInfoCompat.isPreinstalled
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.datastore.Provider
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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

    val isProviderAlive get() = ProviderCompat.isAlive
    val providerVersion get() = with(ProviderCompat) {
        when {
            isAlive -> version
            else -> -1
        }
    }

    val providerPlatform get() = with(ProviderCompat) {
        when {
            isAlive -> platform
            else -> ""
        }
    }

    private val pmCompat get() = ProviderCompat.packageManagerCompat

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
            if (!isProviderAlive) return@launch

            val packagesDeferred = async { getPackages() }
            val userPreferences = userPreferencesRepository.data.first()

            requester = pmCompat.getPackageInfo(
                userPreferences.requester, 0, context.userId
            ).toIPackageInfo(pm = pm)

            executor = pmCompat.getPackageInfo(
                userPreferences.executor, 0, context.userId
            ).toIPackageInfo(pm = pm)

            packages = packagesDeferred.await()
        }
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        val allPackages = runCatching {
            pmCompat.getInstalledPackages(
                0, context.userId
            ).list
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

    fun resetProvider() {
        userPreferencesRepository.setProvider(Provider.None)
        providerDestroy()
    }

    fun providerInit() {
        ProviderCompat.init()
    }

    fun providerDestroy() {
        ProviderCompat.destroy()
    }

    fun setRequesterPackage(pi: IPackageInfo) {
        requester = pi
        userPreferencesRepository.setRequester(pi.packageName)
    }

    fun setExecutorPackage(pi: IPackageInfo) {
        executor = pi
        userPreferencesRepository.setExecutor(pi.packageName)
    }
}