package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.repository.LocalRepository
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
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pmCompat get() = ProviderCompat.packageManagerCompat

    private val packagesFlow = MutableStateFlow(listOf<PackageInfo>())
    private val appsFlow = MutableStateFlow(listOf<PackageInfo>())
    val apps get() = appsFlow.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set

    init {
        Timber.d("AppsViewModel init")
        dataObserver()
    }

    private fun dataObserver() {
        localRepository.getAllAsFlow()
            .combine(packagesFlow) { _, source ->
                if (source.isEmpty()) return@combine

                appsFlow.value = source
                    .sortedByDescending { it.lastUpdateTime }

                isLoading = false

            }.launchIn(viewModelScope)
    }

    private suspend fun getPackages() = withContext(Dispatchers.IO) {
        val allPackages = runCatching {
            pmCompat.getInstalledPackages(
                PackageManager.GET_PERMISSIONS, context.userId
            ).list
        }.onFailure {
            Timber.e(it, "getInstalledPackages")
        }.getOrDefault(emptyList())

        allPackages.filter {
            it.applicationInfo.enabled
        }
    }

    fun loadData() {
        viewModelScope.launch {
            packagesFlow.value = getPackages()

            val packageNames = packagesFlow.value.map { it.packageName }
            val authorized = localRepository.getAll()

            localRepository.delete(
                authorized.filter { it.packageName !in packageNames }
            )
        }
    }
}