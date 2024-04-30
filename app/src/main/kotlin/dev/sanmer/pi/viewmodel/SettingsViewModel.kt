package dev.sanmer.pi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.datastore.Provider
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val isProviderAlive get() = ProviderCompat.isAlive
    val providerVersion get() = ProviderCompat.get({ it.version }, -1)
    val providerPlatform get() = ProviderCompat.get({ it.platform }, "")

    init {
        Timber.d("SettingsViewModel init")
    }

    fun setDynamicColor(value: Boolean) =
        userPreferencesRepository.setDynamicColor(value)

    fun setProvider(value: Provider) =
        userPreferencesRepository.setProvider(value)

    fun setSelfUpdate(value: Boolean) =
        userPreferencesRepository.setSelfUpdate(value)

    fun tryStartProvider() {
        if (isProviderAlive) return

        viewModelScope.launch {
            val userPreferences = userPreferencesRepository.data.first()
            ProviderCompat.init(userPreferences.provider)
        }
    }
}