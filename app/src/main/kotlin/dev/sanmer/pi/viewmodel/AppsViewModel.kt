package dev.sanmer.pi.viewmodel

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val pmCompat get() = ProviderCompat.packageManagerCompat

    val isProviderAlive get() = ProviderCompat.isAlive

    var isSearch by mutableStateOf(false)
        private set
    private val keyFlow = MutableStateFlow("")

    private val packagesFlow = MutableStateFlow(listOf<PackageInfo>())
    private val cacheFlow = MutableStateFlow(listOf<IPackageInfo>())
    private val appsFlow = MutableStateFlow(listOf<IPackageInfo>())
    val apps get() = appsFlow.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set

    init {
        Timber.d("AppsViewModel init")
        dataObserver()
        keyObserver()
    }

    private fun dataObserver() {
        combine(
            localRepository.getAllAsFlow(),
            userPreferencesRepository.data,
            packagesFlow,
        ) { authorized, preferences, source ->
            if (source.isEmpty()) return@combine

            cacheFlow.value = source.map { pi ->
                pi.toIPackageInfo(
                    isAuthorized = authorized.find {
                        it.packageName == pi.packageName
                    }?.authorized ?: false,
                    isRequester = preferences.requester == pi.packageName,
                    isExecutor = preferences.executor == pi.packageName
                )
            }.sortedByDescending { it.lastUpdateTime }
                .sortedByDescending { it.isAuthorized }
                .sortedByDescending { it.isExecutor || it.isRequester }

            isLoading = false

        }.launchIn(viewModelScope)
    }

    private fun keyObserver() {
        combine(
            keyFlow,
            cacheFlow
        ) { key, source ->

            appsFlow.value = source
                .filter {
                    if (key.isNotBlank()) {
                        it.appLabel.contains(key, ignoreCase = true)
                                || it.packageName.contains(key, ignoreCase = true)
                    } else {
                        true
                    }
                }

        }.launchIn(viewModelScope)
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        val allPackages = runCatching {
            pmCompat.getInstalledPackages(
                PackageManager.GET_PERMISSIONS, UserHandleCompat.myUserId()
            ).list
        }.onFailure {
            Timber.e(it, "getInstalledPackages")
        }.getOrDefault(emptyList())

        allPackages.filter {
            it.applicationInfo.enabled
        }
    }

    fun loadData() {
        if (!isProviderAlive) return

        viewModelScope.launch {
            packagesFlow.value = getPackages()

            val packageNames = packagesFlow.value.map { it.packageName }
            val authorized = localRepository.getAll()

            localRepository.delete(
                authorized.filter { it.packageName !in packageNames }
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
}