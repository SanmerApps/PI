package dev.sanmer.pi.ui.main

import android.content.pm.PackageInfo
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sanmer.pi.core.compat.PackageInfoCompat.isOverlayPackage
import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.model.LoadData
import dev.sanmer.pi.repository.PackagesRepository
import dev.sanmer.pi.repository.PreferenceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class PackagesViewModel(
    private val packagesRepository: PackagesRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    var packageInfos by mutableStateOf<LoadData<List<PackageInfo>>>(LoadData.Loading)
        private set

    var preference by mutableStateOf(Preference())
        private set

    val query = TextFieldState()
    private val queryFlow = snapshotFlow { query.text }

    val listState = LazyListState()

    init {
        Log.d(TAG, "init")
        loadPackages()
        loadPreference()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadPackages() {
        viewModelScope.launch {
            packagesRepository.getInstalledPackagesAsFlow(0)
                .mapLatest { list ->
                    val preference = preferenceRepository.data.first()
                    list.filter {
                        !it.isOverlayPackage
                    }.sortedWith(
                        compareByDescending<PackageInfo> {
                            it.packageName == preference.originatingPackageName
                                    || it.packageName == preference.installerPackageName
                        }.thenByDescending {
                            it.lastUpdateTime
                        }
                    )
                }
                .combine(queryFlow) { list, query -> list to query }
                .collectLatest { (list, query) ->
                    if (query.isNotEmpty()) {
                        packageInfos = LoadData.Success(
                            list.filter {
                                it.packageName.contains(
                                    query, ignoreCase = true
                                ) || packagesRepository.loadLabel(it).contains(
                                    query, ignoreCase = true
                                ) || it.applicationInfo?.uid?.toString() == query
                            }
                        )
                    } else {
                        packageInfos = LoadData.Success(list)
                        listState.requestScrollToItem(0)
                    }
                }
        }
    }

    private fun loadPreference() {
        viewModelScope.launch {
            preferenceRepository.data.collect {
                preference = it
            }
        }
    }

    fun setOriginatingPackageName(value: String) {
        viewModelScope.launch {
            preferenceRepository.setOriginatingPackageName(value)
        }
    }

    fun setInstallerPackageName(value: String) {
        viewModelScope.launch {
            preferenceRepository.setInstallerPackageName(value)
        }
    }

    private companion object Default {
        const val TAG = "PackagesViewModel"
    }
}