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
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.datastore.Provider
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val pmCompat get() = ProviderCompat.packageManagerCompat

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

    val authorized get() = localRepository.getAuthorizedAllAsFlow().map { it.size }
    var requester: IPackageInfo? by mutableStateOf(null)
        private set
    var executor: IPackageInfo? by mutableStateOf(null)
        private set

    init {
        Timber.d("HomeViewModel init")
    }

    fun loadData() {
        viewModelScope.launch {
            if (!isProviderAlive) return@launch

            val userPreferences = userPreferencesRepository.data.first()

            requester = pmCompat.getPackageInfo(
                userPreferences.requester, 0, context.userId
            ).toIPackageInfo(pm = pm)

            executor = pmCompat.getPackageInfo(
                userPreferences.executor, 0, context.userId
            ).toIPackageInfo(pm = pm)

        }
    }

    fun resetProvider() {
        userPreferencesRepository.setProvider(Provider.None)
        providerDestroy()
    }

    fun providerInit() {
        viewModelScope.launch {
            val userPreferences = userPreferencesRepository.data.first()
            ProviderCompat.init(userPreferences.provider)
        }
    }

    fun providerDestroy() {
        ProviderCompat.destroy()
    }
}