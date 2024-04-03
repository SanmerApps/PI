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
    val providerVersion get() = ProviderCompat.get({ it.version }, -1)
    val providerPlatform get() = ProviderCompat.get({ it.platform }, "")

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
            val userPreferences = if (isProviderAlive) {
                userPreferencesRepository.data.first()
            } else {
                return@launch
            }

            requester = pmCompat.getPackageInfo(
                userPreferences.requester, 0, context.userId
            ).toIPackageInfo(pm = pm)

            executor = pmCompat.getPackageInfo(
                userPreferences.executor, 0, context.userId
            ).toIPackageInfo(pm = pm)

        }
    }
}