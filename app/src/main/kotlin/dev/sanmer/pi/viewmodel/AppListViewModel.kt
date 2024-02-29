package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.PackageInfoCompat.isOverlayPackage
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.navigation.MainScreen
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
class AppListViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val target = getTarget(savedStateHandle)

    private val context: Context by lazy { getApplication() }
    private val pm by lazy { context.packageManager }
    private val pmCompat get() = ProviderCompat.packageManagerCompat

    var isSearch by mutableStateOf(false)
        private set
    private val keyFlow = MutableStateFlow("")

    private val packagesFlow = MutableStateFlow(listOf<IPackageInfo>())
    private val appsFlow = MutableStateFlow(listOf<IPackageInfo>())
    val apps get() = appsFlow.asStateFlow()

    private var newPackage: IPackageInfo? by mutableStateOf(null)

    var isLoading by mutableStateOf(true)
        private set

    init {
        Timber.d("AppListViewModel init")
        dataObserver()
    }

    private fun dataObserver() {
        packagesFlow.combine(keyFlow) { list, key ->
            if (list.isEmpty()) return@combine

            appsFlow.value = list
                .filter {
                    if (key.isBlank()) return@filter true
                    key.lowercase() in (it.label + it.packageName).lowercase()

                }.sortedBy { it.label.uppercase() }

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

        allPackages
            .filter { !it.isOverlayPackage }
            .map {
                IPackageInfo(
                    packageInfo = it,
                    pm = pm
                )
            }
    }

    fun loadData() {
        viewModelScope.launch {
            packagesFlow.value = getPackages()
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

    fun isSelected(pi: IPackageInfo): Boolean {
        return pi.packageName == newPackage?.packageName
    }

    fun toggle(pi: IPackageInfo) {
        newPackage = if (isSelected(pi)) null else pi
    }

    fun setPackage(): Boolean {
        if (newPackage == null) return false

        with(userPreferencesRepository) {
            when (target) {
                Target.Requester -> setRequester(newPackage!!.packageName)
                Target.Executor -> setExecutor(newPackage!!.packageName)
            }
        }

        return true
    }

    enum class Target {
        Requester,
        Executor
    }

    companion object {
        fun putTarget(target: Target) =
            MainScreen.AppList.route.replace(
                "{target}", target.name
            )

        fun getTarget(savedStateHandle: SavedStateHandle): Target =
            (checkNotNull(savedStateHandle["target"]) as String)
                .let(Target::valueOf)
    }
}